package org.horizontal.tella.mobile.domain.usecases.googledrive

import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.googledrive.Config
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer
import org.horizontal.tella.mobile.domain.repository.googledrive.ITellaGoogleDriveRepository
import org.horizontal.tella.mobile.domain.usecases.base.SingleUseCase
import javax.inject.Inject

class GetReportsServersUseCase @Inject constructor(
    private val serverRepository: ITellaGoogleDriveRepository,
    private val config: Config
) :
    SingleUseCase<List<GoogleDriveServer>>() {

    override fun buildUseCaseSingle(): Single<List<GoogleDriveServer>> {
        return serverRepository.listGoogleDriveServers(config.googleClientId)
    }
}