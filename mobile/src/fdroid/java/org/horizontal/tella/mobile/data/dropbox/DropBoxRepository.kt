package org.horizontal.tella.mobile.data.dropbox

import io.reactivex.Flowable
import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.UploadProgressInfo
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer
import org.horizontal.tella.mobile.domain.repository.dropbox.DropboxClientHandle
import org.horizontal.tella.mobile.domain.repository.dropbox.IDropBoxRepository
import javax.inject.Inject

/**
 * Stub implementation of DropBoxRepository for F-Droid builds.
 *
 * Dropbox is not available in F-Droid; this stub never touches the Dropbox SDK.
 */
class DropBoxRepository @Inject constructor() : IDropBoxRepository {

    override fun createDropboxClient(server: DropBoxServer): Single<DropboxClientHandle> {
        return Single.error(UnsupportedOperationException("Dropbox is not available in F-Droid builds"))
    }

    override fun createDropboxFolder(
        client: DropboxClientHandle,
        folderName: String,
        description: String
    ): Single<String> {
        return Single.error(UnsupportedOperationException("Dropbox is not available in F-Droid builds"))
    }

    override fun uploadFileWithProgress(
        client: DropboxClientHandle,
        folderPath: String,
        mediaFile: FormMediaFile,
    ): Flowable<UploadProgressInfo> {
        return Flowable.error(UnsupportedOperationException("Dropbox is not available in F-Droid builds"))
    }
}




