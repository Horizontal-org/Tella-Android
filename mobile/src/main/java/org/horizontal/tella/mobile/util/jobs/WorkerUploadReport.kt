package org.horizontal.tella.mobile.util.jobs

import android.annotation.SuppressLint
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.Single
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
) : RxWorker(context, workerParams) {

    @SuppressLint("RestrictedApi")
    override fun createWork(): Single<Result> {
        return Single.fromCallable {
            if (!statusProvider.isOnline()) {
                return@fromCallable Result.retry()
            }

            val mainKey = try {
                MyApplication.getMainKeyHolder().get().key.encoded
            } catch (e: LifecycleMainKey.MainKeyUnavailableException) {
                Timber.e(e, "Failed to retrieve main key")
                return@fromCallable Result.retry()
            }

            val dataSource = DataSource.getInstance(context, mainKey)
            val reportFormInstances = getOutboxReportInstances(dataSource)

            if (reportFormInstances.isEmpty()) {
                setNoTimeOut(false)
                return@fromCallable Result.success()
            } else {
                setNoTimeOut(true)
            }

            val server = getServer(dataSource) ?: return@fromCallable Result.failure()

            for (reportInstance in reportFormInstances) {
                val reportWithFiles = getReportBundle(dataSource, reportInstance).blockingGet()

                if (reportWithFiles.reportApiId.isEmpty()) {
                    val report = reportsRepository.submitReport(
                        server, ReportBodyEntity(
                            reportWithFiles.title, reportWithFiles.description
                        )
                    ).blockingGet()

                    reportWithFiles.reportApiId = report.id

                    reportsRepository.submitFiles(reportWithFiles, server, report.id)
                } else {
                    reportsRepository.submitFiles(
                        reportWithFiles, server, reportWithFiles.reportApiId
                    )
                }

                Timber.d("*** Test worker *** widgetMediaFiles? %s", reportWithFiles.widgetMediaFiles)
            }

            setNoTimeOut(false)
            return@fromCallable Result.success()
        }.onErrorReturn { error ->
            Timber.e(error, "WorkerUploadReport failed")
            Result.failure()
        }
    }

    private fun getServer(dataSource: DataSource): TellaReportServer? {
        return dataSource.listTellaUploadServers().blockingGet()
            .firstOrNull { server -> server.isActivatedBackgroundUpload || server.isAutoUpload }
    }

    private fun getAutoBackgroundServers(dataSource: DataSource): Single<List<TellaReportServer>> {
        return dataSource.listTellaUploadServers()
            .map { servers -> servers.filter { it.isActivatedBackgroundUpload || it.isAutoUpload } }
    }

    private fun filterInstancesByAutoBackgroundServers(
        instances: List<ReportInstance>,
        autoBackgroundServers: List<TellaReportServer>
    ): List<ReportInstance> {
        return instances.filter { instance ->
            autoBackgroundServers.any { server -> server.id == instance.serverId }
        }
    }

    private fun getOutboxReportInstances(dataSource: DataSource): List<ReportInstance> {
        val outboxInstances = dataSource.listOutboxReportInstances().blockingGet()
        val autoBackgroundServers = getAutoBackgroundServers(dataSource).blockingGet()

        return filterInstancesByAutoBackgroundServers(
            outboxInstances,
            autoBackgroundServers
        ).sortedByDescending { it.updated }
    }

    private fun getReportBundle(
        dataSource: DataSource,
        reportInstance: ReportInstance
    ): Single<ReportInstance> {

        return dataSource.getReportMediaFiles(reportInstance).flatMap { files ->
            MyApplication.keyRxVault.rxVault
                .firstOrError()
                .flatMap { rxVault ->
                    rxVault.get(files.mapNotNull { it.id }.toTypedArray())
                }
                .map { vaultFiles ->
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
                    reportInstance
                }
        }
    }

    private fun setNoTimeOut(enableNoTimeout: Boolean) {
        if (enableNoTimeout) {
            MyApplication.getMainKeyHolder().timeout = LifecycleMainKey.NO_TIMEOUT
        } else {
            MyApplication.getMainKeyHolder().timeout = LockTimeoutManager.IMMEDIATE_SHUTDOWN
        }
    }
}
