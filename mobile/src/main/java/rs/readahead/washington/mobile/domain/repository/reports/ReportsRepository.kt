package rs.readahead.washington.mobile.domain.repository.reports

import com.hzontal.tella_vault.VaultFile
import io.reactivex.Flowable
import io.reactivex.Single
import rs.readahead.washington.mobile.data.entity.reports.ReportBodyEntity
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.reports.ProjectResult
import rs.readahead.washington.mobile.domain.entity.reports.ReportPostResult
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer

interface ReportsRepository {
    fun login(server: TellaReportServer, projectSlug: String): Single<TellaReportServer>

    fun submitReport(
        server: TellaReportServer,
        reportBody: ReportBodyEntity
    ): Single<ReportPostResult>

    fun getProjects(
        limit: Int,
        offset: Int,
        servers: List<TellaReportServer>
    ): Single<List<ProjectResult>>

    fun upload(mediaFile: VaultFile, server: TellaReportServer): Flowable<UploadProgressInfo?>

    fun check(baseUrl: String): Single<UploadProgressInfo?>
}