package org.horizontal.tella.mobile.domain.usecases.dropbox

import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer
import org.horizontal.tella.mobile.domain.repository.dropbox.ITellaDropBoxRepository
import org.horizontal.tella.mobile.domain.usecases.base.SingleUseCase
import javax.inject.Inject

class UpdateDropBoxUseCase @Inject constructor(private val dropBoxRepository: ITellaDropBoxRepository) :
    SingleUseCase<DropBoxServer>() {
    private lateinit var serverDropBoxServer: DropBoxServer

    fun setDropBox(server: DropBoxServer) {
        serverDropBoxServer = server
    }

    override fun buildUseCaseSingle(): Single<DropBoxServer> {
        return dropBoxRepository.updateDropBoxServer(serverDropBoxServer)
    }
}