package rs.readahead.washington.mobile.util.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hzontal.tella_vault.VaultFile
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.hzontal.tella.keys.key.LifecycleMainKey
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.bus.event.FileUploadProgressEvent
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.entity.reports.ReportBodyEntity
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository.UploadStatus
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository
import rs.readahead.washington.mobile.util.ThreadUtil
import timber.log.Timber

const val TAG = "WorkerUploadReport"

@HiltWorker
class WorkerUploadReport
@AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted workerParams: WorkerParameters,
    val reportsRepository: ReportsRepository
) : Worker(context, workerParams) {
    private var server: TellaReportServer? = null
    private lateinit var dataSource: DataSource

    override fun doWork(): Result {
        //3 STEPS
        //First submit report with title and description
        //First upload has highest priority, we are going to upload just to that server
        //SECOND get report if response was successful
        //Third submit files with reportID

        var key: ByteArray?
        try {
            if (MyApplication.getMainKeyHolder().get().key.encoded.also { key = it } == null) {
                return Result.Failure()
            }
        } catch (e: LifecycleMainKey.MainKeyUnavailableException) {
            e.printStackTrace()
            return Result.Failure()
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
            Timber.d("*** Test worker *** server ? %s", server!!.id)

            Timber.d("*** Test worker *** reportFormInstance? %s", reportInstance.id)

            //submit the report to the server without files
            val report = reportsRepository.submitReport(
                server!!,
                ReportBodyEntity(
                    reportInstance.title,
                    reportInstance.description
                )
            ).blockingGet()

            reportInstance.widgetMediaFiles =
                dataSource.getReportMediaFiles(reportInstance).blockingGet()


            Timber.d("*** Test worker *** report? %s", report.id)


            // for (reportFormInstance in reportFormInstances) {

            Timber.d("*** Test worker *** widgetMediaFiles? %s", reportInstance.widgetMediaFiles)


            //Grab the server instance from the server
            Flowable.fromIterable(reportInstance.widgetMediaFiles)
                .flatMap { file: FormMediaFile ->

                    reportsRepository.upload(
                        MyApplication.rxVault.get(file.id).blockingGet(),
                        server?.url!!,
                        report.id,
                        server?.accessToken!!
                    )
                }.doOnComplete {
                    updateReportInstanceProgress(reportInstance)
                }
                .blockingSubscribe(
                    {
                        /* updateProgress(
                             progressInfo!!
                         )*/
                    }
                ) { throwable: Throwable? ->

                    Timber.d(throwable)
                    // FirebaseCrashlytics.getInstance().recordException(throwable!!)
                }
        }

        return Result.Success()
    }

    private fun getServer(): TellaReportServer? {
        return dataSource.listTellaUploadServers().blockingGet()
            .firstOrNull { server -> server.isActivatedBackgroundUpload }
    }

    private fun updateReportInstanceProgress(reportInstance: ReportInstance){

    }
    private fun updateProgress(progressInfo: UploadProgressInfo) {
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
                if (Preferences.isAutoDeleteEnabled()) {
                    //  deleteMediaFile(progressInfo.fileId)
                }
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
        postProgressEvent(progressInfo)
    }

    private fun deleteMediaFile(id: String) {
        if (MyApplication.rxVault[id]
                .flatMap { vaultFile: VaultFile ->
                    MyApplication.rxVault.delete(
                        vaultFile
                    )
                }
                .subscribeOn(Schedulers.io())
                .blockingGet()
        ) {
            Timber.d("Deleted file %s", id)
        }
    }


    private fun getFilesFromVault(ids: Array<String>): List<VaultFile> {
        return MyApplication.rxVault.get(ids).blockingGet()
    }

    private fun postProgressEvent(progress: UploadProgressInfo) {
        ThreadUtil.runOnMain {
            MyApplication.bus().post(FileUploadProgressEvent(progress))
        }
    }

}