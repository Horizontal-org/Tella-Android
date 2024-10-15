package rs.readahead.washington.mobile.domain.repository.dropbox

import com.dropbox.core.v2.DbxClientV2
import io.reactivex.Flowable
import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile

interface IDropBoxRepository {
    fun createDropboxFolder(client: DbxClientV2, folderName: String): Single<String>
    fun uploadFileWithProgress(
        client: DbxClientV2,
        folderPath: String,
        mediaFile: FormMediaFile
    ) : Flowable<UploadProgressInfo>
}