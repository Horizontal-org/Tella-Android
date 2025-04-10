package org.horizontal.tella.mobile.data.peertopeer

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.domain.peertopeer.PeerRegisterPayload
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class TellaPeerToPeerClient {

    private fun getClientWithFingerprintValidation(expectedFingerprint: String): OkHttpClient {
        val trustManager = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

            override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String?) {}

            override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String?) {
                val serverCert = chain[0]
                val actualFingerprint = CertificateUtils.getPublicKeyHash(serverCert)

                if (!actualFingerprint.equals(expectedFingerprint, ignoreCase = true)) {
                    throw CertificateException(
                        "Server certificate fingerprint mismatch.\nExpected: $expectedFingerprint\nGot: $actualFingerprint"
                    )
                }
            }
        }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .build()
    }

    suspend fun registerPeerDevice(
        ip: String,
        port: String,
        expectedFingerprint: String,
        pin: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {

            val url = "https://$ip:$port/api/v1/register"

            val payload = PeerRegisterPayload(
                pin = pin
            )

            val jsonPayload = Gson().toJson(payload)

            val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())
            val client = getClientWithFingerprintValidation(expectedFingerprint)

            try {
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body.string()
                        Log.d("PeerClient", "Success: $responseBody")
                        Result.success(responseBody)
                    } else {
                        val errorMsg = "Failed: ${response.code} - ${response.message}"
                        Log.e("PeerClient", errorMsg)
                        Result.failure(Exception(errorMsg))
                    }
                }
            } catch (e: Exception) {
                Log.e("PeerClient", "Error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun prepareUpload(
        ip: String,
        port: String,
        expectedFingerprint: String,
        title: String = "Title of the report",
        file: File, // file passed as argument
        fileId: String, // fileId passed as argument
        sha256: String, // sha256 passed as argument
        sessionId: String // sessionId passed as argument
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            val url = "https://$ip:$port/api/v1/prepare-upload"

            val filePayload = """
            {
                "id": "$fileId",
                "fileName": "${file.name}",
                "size": ${file.length()},
                "fileType": "application/octet-stream",
                "sha256": "$sha256"
            }
        """.trimIndent()

            val jsonPayload = """
            {
                "title": "$title",
                "sessionId": "$sessionId",
                "files": [
                    $filePayload
                ]
            }
        """.trimIndent()

            val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())
            val client = getClientWithFingerprintValidation(expectedFingerprint)

            try {
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        responseBody?.let {
                            val jsonObject = JSONObject(it)
                            val transmissionId = jsonObject.getString("transmissionId")
                            Log.d("PrepareUpload", "Transmission ID: $transmissionId")
                        }
                    } else {
                        Log.e("PrepareUpload", "Error ${response.code}: ${response.message}")
                        when (response.code) {
                            409 -> {
                                Log.e("PrepareUpload", "Conflict: Try canceling active sessions.")
                            }
                            else -> {}
                        }
                    }
                } as Result<String>
            } catch (e: Exception) {
                Log.e("PrepareUpload", "Exception: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

}