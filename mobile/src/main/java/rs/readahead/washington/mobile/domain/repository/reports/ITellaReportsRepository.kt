package rs.readahead.washington.mobile.domain.repository.reports

import io.reactivex.Completable
import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.reports.ReportFormInstance
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstanceBundle

interface ITellaReportsRepository {
    fun saveInstance(instance: ReportFormInstance): Single<ReportFormInstance>
    fun deleteReportInstance(id: Long): Completable
    fun listDraftReportInstances(): Single<List<ReportFormInstance>>
    fun listOutboxReportInstances(): Single<List<ReportFormInstance>>
    fun listSubmittedReportInstances(): Single<List<ReportFormInstance>>
    fun getReportBundle(id: Long): Single<ReportInstanceBundle>
}