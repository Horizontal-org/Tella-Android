package org.horizontal.tella.mobile.domain.repository.googledrive

import io.reactivex.Completable
import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer

interface ITellaGoogleDriveRepository {
    fun saveGoogleDriveServer(instance: GoogleDriveServer): Single<GoogleDriveServer>
    fun listGoogleDriveServers(googleDriveId:String): Single<List<GoogleDriveServer>>
    fun removeGoogleDriveServer(id: Long): Completable
}