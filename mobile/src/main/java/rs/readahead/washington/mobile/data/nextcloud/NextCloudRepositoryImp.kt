package rs.readahead.washington.mobile.data.nextcloud

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.GetCapabilitiesRemoteOperation
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.domain.repository.nextcloud.NextCloudRepository


class NextCloudRepositoryImp(private val context: Context) : NextCloudRepository {


    override fun validateServerUrl(serverUrl: String): Single<Boolean> {
        return Single.create { emitter ->
            try {
                val uri = Uri.parse(serverUrl)
                val client = OwnCloudClientFactory.createOwnCloudClient(uri, context, false)

                val getCapabilitiesOperation = GetCapabilitiesRemoteOperation()
                getCapabilitiesOperation.execute(client,
                    { _, result ->


                        result?.let {
                            if (it.isSuccess) {
                                val credentials = OwnCloudCredentialsFactory.newBasicCredentials("dhekra@wearehorizontal.org", "GSumS9uL7X8SoW")
                                client.credentials = credentials


                                // get display name


                                emitter.onSuccess(true)
                            } else {
                                emitter.onError(result.exception)
                            }
                        } ?:  emitter.onSuccess(false)
                    }, Handler(Looper.getMainLooper())
                )

            } catch (e: Exception) {
                Log.e("ValidateServerUrl", "Exception occurred: ${e.localizedMessage}", e)
                emitter.onError(e)
            }
        }.subscribeOn(Schedulers.io())
    }


    override fun checkUserCredentials(serverUrl: String, username: String, password: String): Single<RemoteOperationResult<UserInfo?>> {
        return Single.create<RemoteOperationResult<UserInfo?>> { emitter ->

                // Client
                val baseUri = Uri.parse(serverUrl)
                Log.d("CheckUserCredentials", "Base URI: $baseUri")

                val credentials = OwnCloudCredentialsFactory.newBasicCredentials(username, password)
                Log.d("CheckUserCredentials", "Credentials: $credentials")

                val cloudClient = OwnCloudClientFactory.createOwnCloudClient(baseUri, context, true)
                cloudClient.credentials = credentials
                Log.d("CheckUserCredentials", "Client: $cloudClient")

                // Ensure client and credentials are valid
                if (cloudClient == null || credentials == null) {
                    emitter.onError(Exception("Client or credentials are null"))
                    return@create
                }

                // Create Nextcloud Client and perform the operation
                val nextcloudClient = OwnCloudClientFactory.createNextcloudClient(
                    baseUri,
                    credentials.username,
                    credentials.toOkHttpCredentials(),
                    context,
                    true
                )

            val getCapabilitiesOperation = GetUserInfoRemoteOperation().execute(nextcloudClient)


            Log.d("CheckUserCredentials", "Client: ${getCapabilitiesOperation.isSuccess}")


        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }



}