package org.horizontal.tella.mobile.domain.repository.reports

import com.hzontal.tella_vault.VaultFile
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.data.entity.reports.ReportBodyEntity
import org.horizontal.tella.mobile.domain.entity.UploadProgressInfo
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.domain.entity.reports.ReportPostResult
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer

interface ReportsRepository {

    fun login(server: TellaReportServer, slug: String): Single<TellaReportServer>

    fun submitReport(server: TellaReportServer, instance: ReportInstance, backButtonPressed: Boolean)

    fun submitReport(
        server: TellaReportServer,
        reportBody: ReportBodyEntity
    ): Single<ReportPostResult>

    fun submitFiles(
        instance: ReportInstance,
        server: TellaReportServer,
        reportApiId: String
    )

    fun upload(
        vaultFile: VaultFile,
        urlServer: String,
        reportId: String,
        accessToken: String
    ): Flowable<UploadProgressInfo>

    fun check(
        vaultFile: VaultFile,
        urlServer: String,
        reportId: String,
        accessToken: String
    ): Single<UploadProgressInfo>

    fun getDisposable(): CompositeDisposable

    fun getReportProgress(): SingleLiveEvent<Pair<UploadProgressInfo, ReportInstance>>

    fun geInstanceProgress(): SingleLiveEvent<ReportInstance>

    fun cleanup()
}