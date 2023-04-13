package rs.readahead.washington.mobile.util.jobs

import android.annotation.SuppressLint
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.Flowable
import org.hzontal.tella.keys.key.LifecycleMainKey
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.bus.event.ReportUploadProgressEvent
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository.UploadStatus
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository
import rs.readahead.washington.mobile.util.StatusProvider
import rs.readahead.washington.mobile.util.ThreadUtil
import timber.log.Timber

const val TAG = "WorkerUploadReport"

@HiltWorker
class WorkerUploadReport
@AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted workerParams: WorkerParameters,
    val reportsRepository: ReportsRepository,
    val statusProvider: StatusProvider
) : Worker(context, workerParams) {
    private var server: TellaReportServer? = null
    private lateinit var dataSource: DataSource

    @SuppressLint("RestrictedApi")
    override fun doWork(): Result {
        //3 STEPS
        //First submit report with title and description
        //First upload has highest priority, we are going to upload just to that server
        //SECOND get report if response was successful
        //Third submit files with reportID

        if (!statusProvider.isOnline()) {
            return Result.retry()
        }

        var key: ByteArray?
        try {
            if (MyApplication.getMainKeyHolder().get().key.encoded.also { key = it } == null) {
                return Result.Retry()
            }
        } catch (e: LifecycleMainKey.MainKeyUnavailableException) {
            e.printStackTrace()
            return Result.Retry()
        }

        dataSource = DataSource.getInstance(context, key)
        val reportFormInstances = dataSource.listOutboxReportInstances().blockingGet()

        if (reportFormInstances.isEmpty()) {
            return Result.Success()
        }

        Timber.d("*** Test worker *** reportFormInstances size ? %s", reportFormInstances.size)

        server = getServer()

        if (server == null) {
            return Result.Failure()
        }

        for (reportInstance in reportFormInstances) {

            reportInstance.widgetMediaFiles =
                dataSource.getReportMediaFiles(reportInstance).blockingGet()


            reportsRepository.submitReport(
                server!!,
                reportInstance
            )
                .blockingGet()

            Timber.d("*** Test worker *** widgetMediaFiles? %s", reportInstance.widgetMediaFiles)


        }

        return Result.Success()
    }


    private fun getServer(): TellaReportServer? {
        return dataSource.listTellaUploadServers().blockingGet()
            .firstOrNull { server -> server.isActivatedBackgroundUpload }
    }

    private fun submitFiles(reportInstance: ReportInstance, reportId: String) {

        if (reportInstance.widgetMediaFiles.isEmpty()) {
            updateReportProgress(
                setReportStatus(
                    reportInstance,
                    EntityStatus.SUBMITTED
                )
            )

            return
        }
        //Grab the server instance from the server
        Flowable.fromIterable(reportInstance.widgetMediaFiles)
            .flatMap { file: FormMediaFile ->

                reportsRepository.upload(
                    MyApplication.rxVault.get(file.id).blockingGet(),
                    server?.url!!,
                    reportId,
                    server?.accessToken!!
                )
            }.doOnEach {
                updateReportProgress(
                    setReportStatus(
                        reportInstance,
                        EntityStatus.SUBMISSION_IN_PROGRESS
                    )
                )
            }.doOnTerminate {
                updateReportProgress(
                    setReportStatus(
                        reportInstance,
                        EntityStatus.SUBMITTED
                    )
                )
            }
            .blockingSubscribe(
                {

                }
            ) { throwable: Throwable? ->

                Timber.d(throwable)
                // FirebaseCrashlytics.getInstance().recordException(throwable!!)
            }
    }

    private fun setReportStatus(
        reportInstance: ReportInstance,
        status: EntityStatus
    ): ReportInstance {
        reportInstance.status = status

        return reportInstance
    }

    private fun updateReportProgress(reportInstance: ReportInstance) {
        when (reportInstance.status) {

            EntityStatus.SUBMITTED -> {
                dataSource.saveInstance(reportInstance).blockingGet()
                if (Preferences.isAutoDeleteEnabled()) {
                    dataSource.deleteReportInstance(reportInstance.id).blockingGet()
                }
            }
            else -> {
                dataSource.saveInstance(reportInstance).blockingGet()
            }

        }
        //postReportProgressEvent(reportInstance)
    }

    private fun updateProgress(instance: ReportInstance, progressInfo: UploadProgressInfo) {
        when (progressInfo.status) {
            UploadProgressInfo.Status.STARTED, UploadProgressInfo.Status.OK -> dataSource.setUploadStatus(
                progressInfo.fileId,
                UploadStatus.UPLOADING,
                progressInfo.current,
                false
            ).blockingAwait()
            UploadProgressInfo.Status.CONFLICT, UploadProgressInfo.Status.FINISHED -> {
                dataSource.setUploadStatus(
                    progressInfo.fileId,
                    UploadStatus.UPLOADED,
                    progressInfo.current,
                    false
                ).blockingAwait()
            }
            UploadProgressInfo.Status.ERROR -> dataSource.setUploadStatus(
                progressInfo.fileId,
                UploadStatus.SCHEDULED,
                progressInfo.current,
                true
            ).blockingAwait()
            else -> dataSource.setUploadStatus(
                progressInfo.fileId,
                UploadStatus.UNKNOWN,
                progressInfo.current,
                true
            ).blockingAwait()
        }
    }

    private fun postReportProgressEvent(reportInstance: ReportInstance) {
        ThreadUtil.runOnMain {
            MyApplication.bus().post(ReportUploadProgressEvent(reportInstance))
        }
    }

}