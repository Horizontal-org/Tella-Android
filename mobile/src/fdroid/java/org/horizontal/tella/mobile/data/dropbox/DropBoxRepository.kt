package org.horizontal.tella.mobile.data.dropbox

import com.dropbox.core.v2.DbxClientV2
import io.reactivex.Flowable
import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.UploadProgressInfo
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer
import org.horizontal.tella.mobile.domain.repository.dropbox.IDropBoxRepository
import javax.inject.Inject

/**
 * Stub implementation of DropBoxRepository for F-Droid builds.
 * 
 * This class exists to satisfy compile-time dependencies but throws
 * UnsupportedOperationException since Dropbox is not available in F-Droid builds.
 */
class DropBoxRepository @Inject constructor() : IDropBoxRepository {
    
    override fun createDropboxClient(server: DropBoxServer): Single<DbxClientV2> {
        return Single.error(UnsupportedOperationException("Dropbox is not available in F-Droid builds"))
    }
        //TODO: ahlem transform to bottom sheet
    override fun createDropboxFolder(
        client: DbxClientV2,
        folderName: String,
        description: String
    ): Single<String> {
        return Single.error(UnsupportedOperationException("Dropbox is not available in F-Droid builds"))
    }

    override fun uploadFileWithProgress(
        client: DbxClientV2,
        folderPath: String,
        mediaFile: FormMediaFile,
    ): Flowable<UploadProgressInfo> {
        return Flowable.error(UnsupportedOperationException("Dropbox is not available in F-Droid builds"))
    }
}




