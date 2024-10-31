package rs.readahead.washington.mobile.domain.repository.nextcloud

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer
import java.io.InputStream

interface NextCloudRepository {
    fun validateServerUrl(serverUrl: String) : Single<Boolean>
    fun checkUserCredentials(serverUrl: String, username: String, password: String): Single<RemoteOperationResult<UserInfo?>>
    fun uploadDescription(
        client: OwnCloudClient,
        folderPath: String,
        description: String
    ): Single<String>

    fun uploadFileWithProgress(
        client: OwnCloudClient,
        folderPath: String,
        mediaFile: FormMediaFile,
        inputStream: InputStream
    ): Flowable<UploadProgressInfo>
}