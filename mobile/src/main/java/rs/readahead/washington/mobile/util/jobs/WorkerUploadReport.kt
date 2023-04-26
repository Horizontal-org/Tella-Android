package rs.readahead.washington.mobile.util.jobs

import android.annotation.SuppressLint
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.RxWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hzontal.tella_vault.VaultFile
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.Flowable
import io.reactivex.Single
import org.hzontal.tella.keys.key.LifecycleMainKey
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.bus.event.ReportUploadProgressEvent
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.entity.reports.ReportBodyEntity
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
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
        val reportFormInstances = getOutboxReportInstances()

        if (reportFormInstances.isEmpty()) {
            return Result.Success()
        }

        Timber.d("*** Test worker *** reportFormInstances size ? %s", reportFormInstances.size)

        server = getServer()

        if (server == null) {
            return Result.Failure()
        }

        for (reportInstance in reportFormInstances) {
            val reportWithFiles = getReportBundle(reportInstance).blockingGet()

            if (reportWithFiles.reportApiId.isEmpty()) {
                //submit the report to the server without files
                val report = reportsRepository.submitReport(
                    server!!,
                    ReportBodyEntity(
                        reportWithFiles.title,
                        reportWithFiles.description
                    )
                ).blockingGet()

                reportWithFiles.reportApiId = report.id


               reportsRepository.submitFiles(reportWithFiles,server!!, report.id)


            } else {
                reportsRepository.submitFiles(reportWithFiles,server!!, reportWithFiles.reportApiId)
            }

            Timber.d("*** Test worker *** widgetMediaFiles? %s", reportWithFiles.widgetMediaFiles)


        }

        return Result.Success()
    }


    private fun getServer(): TellaReportServer? {
        return dataSource.listTellaUploadServers().blockingGet()
            .firstOrNull { server -> server.isAutoUpload }
    }

    private fun getOutboxReportInstances(): List<ReportInstance> {
        val reports = dataSource.listOutboxReportInstances().blockingGet().sortedByDescending { it.updated }

        if (reports.isEmpty()) {
            return emptyList()
        }

        return reports
    }

    private fun getReportBundle(reportInstance: ReportInstance): Single<ReportInstance> {
        return dataSource.getReportMediaFiles(reportInstance)
            .flatMap { files ->
                MyApplication.rxVault.get(files.map { it.id }.toTypedArray())
                    .map { vaultFiles ->
                        val vaultFileMap = vaultFiles.associateBy { it?.id }
                        val filesResult = files.mapNotNull { formMediaFile ->
                            val vaultFile = vaultFileMap[formMediaFile.id]
                            if (vaultFile != null) {
                                FormMediaFile.fromMediaFile(vaultFile).apply {
                                    status = formMediaFile.status
                                    uploadedSize = formMediaFile.uploadedSize
                                }
                            } else {
                                null
                            }
                        }.toMutableList()
                        reportInstance.widgetMediaFiles = filesResult
                        reportInstance
                    }
            }
    }


}