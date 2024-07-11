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
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import rs.readahead.washington.mobile.domain.repository.nextcloud.NextCloudRepository
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class NextCloudRepositoryImp(private val context: Context) : NextCloudRepository {

    override fun validateServerUrl(serverUrl: String): Single<Boolean> {
        return Single.fromCallable {
            try {
                val uri = Uri.parse(serverUrl)

                // Create a trust manager that does not validate certificate chains
                val trustAllCertificates = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                }

                // Install the all-trusting trust manager
                val sslContext: SSLContext = SSLContext.getInstance("SSL")
                sslContext.init(null, arrayOf<TrustManager>(trustAllCertificates), java.security.SecureRandom())

                // Create an ssl socket factory with our all-trusting manager
                val sslSocketFactory = sslContext.socketFactory

                val client = OwnCloudClientFactory.createOwnCloudClient(uri, context, true)
                val okHttpClient = OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, trustAllCertificates)
                    .hostnameVerifier { _, _ -> true }
                    .build()

                //client.setCustomOkHttpClient(okHttpClient)

                val getCapabilitiesOperation = GetCapabilitiesRemoteOperation()
                val result = getCapabilitiesOperation.execute(client)
                result.isSuccess
            } catch (e: Exception) {
                false
            }
        }.subscribeOn(Schedulers.io())
    }


    override fun checkUserCredentials(serverUrl: String, username: String, password: String): Single<RemoteOperationResult<UserInfo?>> {
        return Single.fromCallable {
            var result: RemoteOperationResult<UserInfo?>
            try {
                // Client
                val uri = Uri.parse(serverUrl)
                val credentials = OwnCloudCredentialsFactory.newBasicCredentials(username, password)


                val nextcloudClient = OwnCloudClientFactory.createNextcloudClient(
                    uri,
                    credentials.username,
                    credentials.toOkHttpCredentials(),
                    context,
                    true
                )

                // Operation - get display name
                val userInfoResult = GetUserInfoRemoteOperation().execute(nextcloudClient)

                result = if (userInfoResult.isSuccess) {
                    RemoteOperationResult<UserInfo?>(RemoteOperationResult.ResultCode.OK)
                } else {
                    RemoteOperationResult(RemoteOperationResult.ResultCode.UNKNOWN_ERROR)
                }
                result.setResultData(userInfoResult.resultData)
            } catch (e: Exception) {
                result = RemoteOperationResult(RemoteOperationResult.ResultCode.UNKNOWN_ERROR)
            }

            result
        }
    }

}