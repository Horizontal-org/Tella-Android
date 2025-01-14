package org.horizontal.tella.mobile.domain.usecases.reports

import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.domain.repository.ITellaUploadServersRepository
import org.horizontal.tella.mobile.domain.usecases.base.SingleUseCase
import javax.inject.Inject

class GetReportsServersUseCase @Inject constructor(private val serverRepository : ITellaUploadServersRepository) :
    SingleUseCase<List<TellaReportServer>>() {


    override fun buildUseCaseSingle(): Single<List<TellaReportServer>> {
       return serverRepository.listTellaUploadServers()
    }
}