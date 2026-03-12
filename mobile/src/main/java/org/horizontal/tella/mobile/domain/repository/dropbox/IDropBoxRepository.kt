package org.horizontal.tella.mobile.domain.repository.dropbox

import io.reactivex.Flowable
import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.UploadProgressInfo
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer

interface IDropBoxRepository {
    fun createDropboxClient(server: DropBoxServer): Single<DropboxClientHandle>
    fun createDropboxFolder(
        client: DropboxClientHandle,
        folderName: String,
        description: String
    ): Single<String>

    fun uploadFileWithProgress(
        client: DropboxClientHandle,
        folderPath: String,
        mediaFile: FormMediaFile,
    ): Flowable<UploadProgressInfo>
}