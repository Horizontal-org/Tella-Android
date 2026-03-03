package org.horizontal.tella.mobile.util.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.rx2.await
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.data.entity.reports.ReportBodyEntity
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.domain.repository.reports.ReportsRepository
import org.horizontal.tella.mobile.util.LockTimeoutManager
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
        // We need the database instance to check status in the 'finally' block
        var dataSource: DataSource? = null

        try {
            if (!statusProvider.isOnline()) {
                return@withContext Result.retry()
            }

            val mainKey = try {
                MyApplication.getMainKeyHolder().get().key.encoded
            } catch (e: LifecycleMainKey.MainKeyUnavailableException) {
                Timber.e(e, "Failed to retrieve main key")
                return@withContext Result.retry()
            }

            dataSource = DataSource.getInstance(context, mainKey)

            val reportFormInstances = getOutboxReportInstances(dataSource)
            if (reportFormInstances.isEmpty()) {
                Timber.i("*** Test worker *** No report instances to upload")
                setNoTimeOut(false)
                return@withContext Result.success()
            } else {
                setNoTimeOut(true)
            }

            val server = getServer(dataSource) ?: run {
                setNoTimeOut(false)
                return@withContext Result.failure()
            }

            for (reportInstance in reportFormInstances) {
                val reportWithFiles = getReportBundle(dataSource, reportInstance)

                if (reportWithFiles.reportApiId.isEmpty()) {
                    // submitReport likely returns a Single<ReportResponse>
                    val reportResponse = reportsRepository.submitReport(
                        server,
                        ReportBodyEntity(
                            reportWithFiles.title,
                            reportWithFiles.description
                        )
                    ).await()

                    reportWithFiles.reportApiId = reportResponse.id

                    reportsRepository.submitFiles(reportWithFiles, server, reportResponse.id)
                        .await()
                } else {
                    reportsRepository.submitFiles(
                        reportWithFiles, server, reportWithFiles.reportApiId
                    ).await()
                }

                Timber.d(
                    "*** Test worker *** widgetMediaFiles? %s",
                    reportWithFiles.widgetMediaFiles
                )
            }

            Result.success()
        } catch (t: Throwable) {
            Timber.e(t, "WorkerUploadReport failed")
            Result.retry()
        } finally {
            // IMPORTANT: Ensure we set up the timeout again, independently of the worker result.
            // We only reset the timeout if the database shows no more pending reports.
            dataSource?.let {
                disableNoTimeoutOnlyWhenDone(it)
            } ?: run {
                // If we never even got a dataSource (key was locked),
                // ensure we don't accidentally leave the timeout disabled forever.
                setNoTimeOut(false)
            }
        }
    }

    // --- suspend helpers (await Rx Single results on IO) ---

    private suspend fun getServer(dataSource: DataSource): TellaReportServer? {
        return dataSource.listTellaUploadServers().await()
            .firstOrNull { server -> server.isActivatedBackgroundUpload || server.isAutoUpload }
    }

    private suspend fun getAutoBackgroundServers(dataSource: DataSource): List<TellaReportServer> {
        return dataSource.listTellaUploadServers()
            .map { servers -> servers.filter { it.isActivatedBackgroundUpload || it.isAutoUpload } }
            .await()
    }

    private suspend fun getOutboxReportInstances(dataSource: DataSource): List<ReportInstance> {
        val outboxInstances = dataSource.listOutboxReportInstances().await()
        val autoBackgroundServers = getAutoBackgroundServers(dataSource)
        return filterInstancesByAutoBackgroundServers(outboxInstances, autoBackgroundServers)
            .sortedByDescending { it.updated }
    }

    private fun filterInstancesByAutoBackgroundServers(
        instances: List<ReportInstance>,
        autoBackgroundServers: List<TellaReportServer>
    ): List<ReportInstance> {
        return instances.filter { instance ->
            autoBackgroundServers.any { server -> server.id == instance.serverId }
        }
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

    private fun setNoTimeOut(enableNoTimeout: Boolean) {
        if (enableNoTimeout)
            Timber.d("*** Test *** enableNoTimeOut")
        else
            Timber.d("*** Test *** disableNoTimeOut")

        MyApplication.getMainKeyHolder().timeout =
            if (enableNoTimeout) LifecycleMainKey.NO_TIMEOUT
            else LockTimeoutManager.IMMEDIATE_SHUTDOWN
    }

    private suspend fun disableNoTimeoutOnlyWhenDone(dataSource: DataSource) {
        // 1. Fetch the current state of the Outbox from the database
        val remaining = dataSource.listOutboxReportInstances().await()

        // 2. Only if the Outbox is truly empty do we allow the timeout to be reset.
        // This protects us if another worker is still processing its own list.
        if (remaining.isEmpty()) {
            setNoTimeOut(false)
        } else {
            Timber.d("*** Test worker *** Work remaining, keeping NoTimeout active")
        }
    }
}
