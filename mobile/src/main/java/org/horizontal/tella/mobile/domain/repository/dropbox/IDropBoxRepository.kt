package org.horizontal.tella.mobile.domain.repository.dropbox

import com.dropbox.core.v2.DbxClientV2
import io.reactivex.Flowable
import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.UploadProgressInfo
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer

interface IDropBoxRepository {
    fun createDropboxClient(server: DropBoxServer): Single<DbxClientV2>
    fun createDropboxFolder(
        client: DbxClientV2,
        folderName: String,
        description: String
    ): Single<String>

    fun uploadFileWithProgress(
        client: DbxClientV2,
        folderPath: String,
        mediaFile: FormMediaFile,
    ): Flowable<UploadProgressInfo>
}