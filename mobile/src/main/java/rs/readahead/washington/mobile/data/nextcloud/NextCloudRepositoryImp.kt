package rs.readahead.washington.mobile.data.nextcloud

import android.content.Context
import android.net.Uri
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.GetCapabilitiesRemoteOperation
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import io.reactivex.Single
import rs.readahead.washington.mobile.domain.repository.nextcloud.NextCloudRepository

class NextCloudRepositoryImp(private val context: Context) : NextCloudRepository {

    override fun validateServerUrl(serverUrl: String, callback: (Boolean) -> Unit) {
        try {
            val uri = Uri.parse(serverUrl)
            val client = OwnCloudClientFactory.createOwnCloudClient(uri, context, true)
            val getCapabilitiesOperation = GetCapabilitiesRemoteOperation()
            val result = getCapabilitiesOperation.execute(client)
            callback(result.isSuccess)

        } catch (e: Exception) {
            callback(false)
        }
    }


    override fun checkUserCredentials(serverUrl: String, username: String, password: String): Single<RemoteOperationResult<UserInfo?>> {
        return Single.fromCallable {
            var result: RemoteOperationResult<UserInfo?>
            try {
                val uri = Uri.parse(serverUrl)
                val credentials = OwnCloudCredentialsFactory.newBasicCredentials(username, password)
                val client = OwnCloudClientFactory.createOwnCloudClient(uri, context, true)
                client.credentials = credentials

                val userInfoOperation = GetUserInfoRemoteOperation()
                val userInfoResult = userInfoOperation.execute(client)

                result = if (userInfoResult.isSuccess) {
                    RemoteOperationResult<UserInfo?>(RemoteOperationResult.ResultCode.OK)
                } else {
                    RemoteOperationResult(RemoteOperationResult.ResultCode.INVALID_CREDENTIALS)
                }
                result.setResultData(userInfoResult.resultData)
            } catch (e: Exception) {
                result = RemoteOperationResult(RemoteOperationResult.ResultCode.UNKNOWN_ERROR)
            }

            result
        }
    }
}

}