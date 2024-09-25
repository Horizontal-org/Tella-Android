package rs.readahead.washington.mobile.domain.repository.googledrive

import io.reactivex.Completable
import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer

interface IGoogleDriveRepository {
    fun saveGoogleDriveServer(instance: GoogleDriveServer): Single<GoogleDriveServer>
    fun listGoogleDriveServers(): Single<List<GoogleDriveServer>>
    fun removeGoogleDriveServer(id: Long): Completable
}