package rs.readahead.washington.mobile.domain.repository.reports

import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance
import rs.readahead.washington.mobile.domain.entity.reports.ReportFormInstance

interface ITellaReportsRepository {
    fun saveInstance(instance: ReportFormInstance): Single<ReportFormInstance>
}