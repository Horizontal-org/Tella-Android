package rs.readahead.washington.mobile.domain.repository.nextcloud

import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import io.reactivex.Single

interface NextCloudRepository {
    fun validateServerUrl(serverUrl: String) : Single<Boolean>
    fun checkUserCredentials(serverUrl: String, username: String, password: String): Single<RemoteOperationResult<UserInfo?>>
}