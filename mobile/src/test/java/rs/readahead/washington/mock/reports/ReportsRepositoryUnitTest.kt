package rs.readahead.washington.mock.reports

import io.reactivex.schedulers.TestScheduler
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import rs.readahead.washington.mobile.data.reports.repository.ReportsRepositoryImp
import rs.readahead.washington.mobile.domain.entity.reports.ReportsLoginResult
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository
import rs.readahead.washington.mock.restservice.RestService
import rs.readahead.washington.mock.utils.TestSchedulerProvider

class ReportsRepositoryUnitTest {

    private lateinit var testScheduler: TestScheduler
    private lateinit var testSchedulerProvider: TestSchedulerProvider
    private lateinit var reportsRepository: ReportsRepository
    private lateinit var tellaReportServer: TellaReportServer

    @Before
    private fun setUp() {
        testScheduler = TestScheduler()
        testSchedulerProvider = TestSchedulerProvider(testScheduler)
        reportsRepository = ReportsRepositoryImp(RestService().reportsRestService)
    }

    @Test
    private fun `test login result is equal to ReportsLoginResult`() {
        assertThat(reportsRepository.login(tellaReportServer).blockingGet(), `is` (mock(ReportsLoginResult::class.java)))
    }
}