package rs.readahead.washington.mobile.domain.repository.googledrive

import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.googledrive.Folder

interface IGoogleDriveRepository {
    fun saveFolder(instance: Folder): Single<Folder>
}