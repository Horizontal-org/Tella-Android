package rs.readahead.washington.mobile.domain.repository.reports

import io.reactivex.Completable
import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstanceBundle

interface ITellaReportsRepository {
    fun saveInstance(instance: ReportInstance): Single<ReportInstance>
    fun deleteReportInstance(id: Long): Completable
    fun listAllReportInstances(): Single<List<ReportInstance>>
    fun listDraftReportInstances(): Single<List<ReportInstance>>
    fun listOutboxReportInstances(): Single<List<ReportInstance>>
    fun listSubmittedReportInstances(): Single<List<ReportInstance>>
    fun getReportBundle(id: Long): Single<ReportInstanceBundle>

}