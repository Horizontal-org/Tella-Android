package org.horizontal.tella.mobile.domain.repository.nextcloud

import android.content.Context
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import io.reactivex.Flowable
import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.UploadProgressInfo
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import java.io.File

interface NextCloudRepository {
    fun validateServerUrl(serverUrl: String): Single<Boolean>
    fun checkUserCredentials(
        serverUrl: String,
        username: String,
        password: String,
        context: Context
    ): Single<RemoteOperationResult<UserInfo?>>

    fun uploadDescription(
        client: OwnCloudClient,
        folderPath: String,
        newFolderPath : String,
        description: String
    ): Single<String>

    fun uploadFileWithProgress(
        client: OwnCloudClient,
        folderPath: String,
        mediaFile: FormMediaFile,
        file: File
    ): Flowable<UploadProgressInfo>
}