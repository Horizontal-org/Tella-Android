package org.horizontal.tella.mobile.domain.usecases.dropbox

import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer
import org.horizontal.tella.mobile.domain.repository.dropbox.ITellaDropBoxRepository
import org.horizontal.tella.mobile.domain.usecases.base.SingleUseCase
import javax.inject.Inject

class GetReportsServersUseCase @Inject constructor(
    private val serverRepository: ITellaDropBoxRepository,
) :
    SingleUseCase<List<DropBoxServer>>() {

    override fun buildUseCaseSingle(): Single<List<DropBoxServer>> {
        return serverRepository.listDropBoxServers()
    }
}