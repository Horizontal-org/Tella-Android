package org.horizontal.tella.mobile.util.jobs

import androidx.annotation.MainThread
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.util.LockTimeoutManager
import org.hzontal.tella.keys.key.LifecycleMainKey
import timber.log.Timber

object PendingReportsLock {

    fun pendingReports(dataSource: DataSource): Single<List<ReportInstance>> {
        return Single.zip(
            dataSource.listOutboxReportInstances(),
            dataSource.listTellaUploadServers(),
            BiFunction<List<ReportInstance>, List<TellaReportServer>, List<ReportInstance>> { instances, servers ->
                val serverIds = servers.filter { keepsVaultUnlocked(it) }.map { it.id }.toSet()
                instances.filter { it.serverId in serverIds }
            }
        )
    }

    fun refresh(dataSource: DataSource): Single<List<ReportInstance>> {
        return pendingReports(dataSource)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { apply(it.isNotEmpty()) }
    }

    fun refreshQuietly(dataSource: DataSource): Completable {
        return refresh(dataSource)
            .ignoreElement()
            .doOnError { Timber.e(it, "Failed to refresh pending reports lock") }
            .onErrorComplete()
    }

    fun keepsVaultUnlocked(server: TellaReportServer): Boolean {
        return server.isActivatedBackgroundUpload || server.isAutoUpload
    }

    @MainThread
    fun apply(hasPendingReports: Boolean) {
        if (hasPendingReports) {
            try {
                LockTimeoutManager.holdForPendingReports()
            } catch (e: LifecycleMainKey.MainKeyUnavailableException) {
                Timber.d("Vault locked, pending reports wait for the next unlock")
            }
        } else {
            LockTimeoutManager.releasePendingReportsHold()
        }
    }
}
