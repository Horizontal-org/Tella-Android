package rs.readahead.washington.mobile.data.reports.repository

import rs.readahead.washington.mobile.data.reports.remote.ReportsApiService
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository

class ReportsRepositoryImp constructor(private val apiService: ReportsApiService) :
    ReportsRepository {
}