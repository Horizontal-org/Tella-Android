package rs.readahead.washington.mobile.domain.usecases.server

import com.android.artgallery.domain.usecase.base.SingleUseCase
import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.reports.ReportsLoginResult
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository
import javax.inject.Inject

class CheckReportsServerUseCase @Inject constructor(private val reportsRepository: ReportsRepository) :
    SingleUseCase<ReportsLoginResult>() {
    private lateinit var server: TellaReportServer

    fun saveServer(server: TellaReportServer) {
        this.server = server
    }

    override fun buildUseCaseSingle(): Single<ReportsLoginResult> {
       return reportsRepository.login(server)
    }


}