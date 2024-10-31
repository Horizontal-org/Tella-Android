package rs.readahead.washington.mobile.domain.usecases.nextcloud

import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer
import rs.readahead.washington.mobile.domain.repository.nextcloud.ITellaNextCloudRepository
import rs.readahead.washington.mobile.domain.usecases.base.SingleUseCase
import javax.inject.Inject

class GetReportsServersUseCase @Inject constructor(
    private val serverRepository: ITellaNextCloudRepository,
) :
    SingleUseCase<List<NextCloudServer>>() {

    override fun buildUseCaseSingle(): Single<List<NextCloudServer>> {
        return serverRepository.listNextCloudServers()
    }
}