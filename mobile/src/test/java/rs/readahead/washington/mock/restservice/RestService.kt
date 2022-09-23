package rs.readahead.washington.mock.restservice

import org.mockito.Mockito
import rs.readahead.washington.mobile.data.reports.remote.ReportsApiService

class RestService {
    val reportsRestService = Mockito.mock(ReportsApiService::class.java)
}