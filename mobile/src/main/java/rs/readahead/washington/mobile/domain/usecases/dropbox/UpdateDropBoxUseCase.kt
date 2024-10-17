package rs.readahead.washington.mobile.domain.usecases.dropbox

import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.dropbox.DropBoxServer
import rs.readahead.washington.mobile.domain.repository.dropbox.ITellaDropBoxRepository
import rs.readahead.washington.mobile.domain.usecases.base.SingleUseCase
import rs.readahead.washington.mobile.views.fragment.dropbox.di.DropBox
import javax.inject.Inject

class UpdateDropBoxUseCase @Inject constructor(private val dropBoxRepository: ITellaDropBoxRepository) :
    SingleUseCase<DropBoxServer>() {
    private lateinit var serverDropBoxServer: DropBoxServer

    fun setDropBox(server: DropBoxServer) {
        serverDropBoxServer = server
    }

    override fun buildUseCaseSingle(): Single<DropBoxServer> {
        return dropBoxRepository.saveDropBoxServer(serverDropBoxServer)
    }
}