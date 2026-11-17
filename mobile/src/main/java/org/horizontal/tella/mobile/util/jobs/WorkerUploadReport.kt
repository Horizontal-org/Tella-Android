package org.horizontal.tella.mobile.util.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.rx2.await
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.data.entity.reports.ReportBodyEntity
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFileStatus
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.domain.repository.reports.ReportsRepository
import org.horizontal.tella.mobile.util.StatusProvider
import org.hzontal.tella.keys.key.LifecycleMainKey
import timber.log.Timber

const val TAG = "WorkerUploadReport"

@HiltWorker
class WorkerUploadReport @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val reportsRepository: ReportsRepository,
    private val statusProvider: StatusProvider,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var dataSource: DataSource? = null
        var hasIncompleteUploads = false

        try {
            val mainKey = try {
                MyApplication.getMainKeyHolder().get().key.encoded
            } catch (e: LifecycleMainKey.MainKeyUnavailableException) {
                Timber.e(e, "Failed to retrieve main key")
                return@withContext Result.retry()
            }

            val source = DataSource.getInstance(context, mainKey)
            dataSource = source

            val reportFormInstances = PendingReportsLock.refresh(source).await()
                .sortedByDescending { it.updated }
            if (reportFormInstances.isEmpty()) {
                Timber.d("*** Test worker *** no remaining work")
                return@withContext Result.success()
            } else {
                Timber.d("*** Test worker *** there is work remaining")
            }

            if (!statusProvider.isOnline()) {
                Timber.d("*** Test worker *** No connectivity - will retry")
                return@withContext Result.retry()
            }

            for (reportInstance in reportFormInstances) {
                val server = getServer(source, reportInstance.serverId) ?: continue

                val reportWithFiles = getReportBundle(source, reportInstance)

                if (reportWithFiles.reportApiId.isEmpty()) {
                    // submitReport likely returns a Single<ReportResponse>
                    val reportResponse = reportsRepository.submitReport(
                        server,
                        ReportBodyEntity(
                            reportWithFiles.title,
                            reportWithFiles.description
                        )
                    ).await()

                    // reportWithFiles should have an api id now
                    reportWithFiles.reportApiId = reportResponse.id
                }

                Timber.d(
                    "*** Test worker *** about to submit files with reportApiId: %s",
                    reportWithFiles.reportApiId
                )

                reportsRepository.submitFiles(
                    reportWithFiles, server, reportWithFiles.reportApiId
                ).await()

                if (reportWithFiles.widgetMediaFiles.any { it.status != FormMediaFileStatus.SUBMITTED }) {
                    hasIncompleteUploads = true
                }

                Timber.d(
                    "*** Test worker *** stopped submitting files with reportApiId: %s",
                    reportWithFiles.reportApiId
                )

                Timber.d(
                    "*** Test worker *** %s - widgetMediaFiles? %s",
                    reportWithFiles.reportApiId,
                    reportWithFiles.widgetMediaFiles
                )
            }

            if (hasIncompleteUploads) Result.retry() else Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Timber.e(t, "WorkerUploadReport failed")
            Result.retry()
        } finally {
            dataSource?.let { source ->
                withContext(NonCancellable) {
                    try {
                        PendingReportsLock.refresh(source).await()
                    } catch (t: Throwable) {
                        Timber.e(t, "Failed to refresh pending reports lock")
                    }
                }
            }
        }
    }

    // --- suspend helpers (await Rx Single results on IO) ---

    private suspend fun getServer(dataSource: DataSource, serverId: Long): TellaReportServer? {
        return dataSource.listTellaUploadServers().await()
            .firstOrNull { server -> server.id == serverId }
    }

    private suspend fun getReportBundle(
        dataSource: DataSource,
        reportInstance: ReportInstance
    ): ReportInstance {
        val files = dataSource.getReportMediaFiles(reportInstance).await()

        val vaultFiles = MyApplication.keyRxVault.rxVault
            .firstOrError()
            .flatMap { rxVault ->
                rxVault.get(files.mapNotNull { it.id }.toTypedArray())
            }
            .await()

        val vaultFileMap = vaultFiles.associateBy { it?.id }
        val filesResult = files.mapNotNull { formMediaFile ->
            val vaultFile = vaultFileMap[formMediaFile.id]
            vaultFile?.let { file ->
                FormMediaFile.fromMediaFile(file).apply {
                    status = formMediaFile.status
                    uploadedSize = formMediaFile.uploadedSize
                }
            }
        }.toMutableList()

        reportInstance.widgetMediaFiles = filesResult
        return reportInstance
    }

}
