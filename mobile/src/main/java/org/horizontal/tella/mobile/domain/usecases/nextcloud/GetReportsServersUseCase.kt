package org.horizontal.tella.mobile.domain.usecases.nextcloud

import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.nextcloud.NextCloudServer
import org.horizontal.tella.mobile.domain.repository.nextcloud.ITellaNextCloudRepository
import org.horizontal.tella.mobile.domain.usecases.base.SingleUseCase
import javax.inject.Inject

class GetReportsServersUseCase @Inject constructor(
    private val serverRepository: ITellaNextCloudRepository
) :
    SingleUseCase<List<NextCloudServer>>() {


    override fun buildUseCaseSingle(): Single<List<NextCloudServer>> {
        return serverRepository.listNextCloudServers()
    }
}