/**
 * DefaultStore.kt
 * CleanInsightsSDK
 *
 * Created by Benjamin Erhart.
 * Copyright © 2021 Guardian Project. All rights reserved.
 */
package org.cleaninsights.sdk

import android.util.Log
import kotlinx.coroutines.*
import org.cleaninsights.sdk.CleanInsights.Companion.moshi
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

/**
 * Default implementation of a store. Stores the data in JSON format in a given folder.
 *
 * The `#send` implementation just uses a plain `HttpURLConnection`.
 *
 * @param args
 *      The location where to read and persist accumulated data.
 *      Either in the key "storageFile", which is expected to contain the fully qualified URL to a file.
 *      Or a "storageDir" URL, which is expected to point to a directory.
 * @param debug
 *      Optional function to output debug messages.
 */
open class DefaultStore(args: Map<String, Any> = HashMap(), debug: ((message: String) -> Unit) = fun(_){}) : Store(addStorageFile(args), debug) {

    companion object {
        private const val storageFilename = "cleaninsights.json"

        private fun addStorageFile(args: Map<String, Any>): Map<String, Any> {
            if (args.containsKey("storageFile")) return args

            val a = HashMap(args)

            tryCast<File>(args["storageDir"]) {
                a["storageFile"] = File(this, storageFilename)
            }

            return a
        }

        private fun getStorageFile(args: Map<String, Any>): File? {
            var file: File? = null

            tryCast<File>(args["storageFile"]) {
                file = this
            }

            return file
        }
    }

    @Transient
    private var storageFile: File? = getStorageFile(addStorageFile(args))

    private val mCoroutineScope: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.IO + SupervisorJob())
    }


    override fun load(args: Map<String, Any>): Store? {
        val storageFile = getStorageFile(args) ?: return null

        if (storageFile.canRead()) {
            val json = storageFile.readText()

            try {
                return moshi.adapter(DefaultStore::class.java).fromJson(json)
            }
            catch (e: Exception) {
                tryCast<(String?) -> Unit>(args["debug"]) {
                    this(e.localizedMessage)
                }
            }
        }

        return null
    }

    override fun persist(async: Boolean, done: (e: Exception?) -> Unit) {
        val work = {
            try {
                val json = moshi.adapter(DefaultStore::class.java).toJson(this)

                storageFile?.writeText(json)

                done(null)
            }
            catch (e: Exception) {
                done(e)
            }
        }

        if (async) {
            mCoroutineScope.launch {
                work()
            }
        }
        else {
            work()
        }
    }

    override fun send(data: String, server: URL, timeout: Double, done: (e: Exception?) -> Unit) {
        mCoroutineScope.launch {
            try {
                val bytes = data.toByteArray(Charset.defaultCharset())
                Log.e("CLEAN INSIGHTS","bytes size ${bytes.size}")

                @Suppress("BlockingMethodInNonBlockingContext")
                val conn = server.openConnection() as HttpURLConnection
                conn.doOutput = true
                conn.useCaches = false
                conn.connectTimeout = (timeout * 1000).toInt()
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setFixedLengthStreamingMode(bytes.size)

                if (bytes.isNotEmpty()) {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    conn.outputStream.write(bytes)
                    @Suppress("BlockingMethodInNonBlockingContext")
                    conn.outputStream.flush()
                }

                //conn.disconnect()

                if (conn.responseCode != 200 && conn.responseCode != 204) {
                    Log.e("SERVER URL", "HTTP Error ${conn.responseMessage}")
                    done(IOException(String.format("HTTP Error %s: %s", conn.responseCode, conn.responseMessage)))
                    return@launch
                }

                done(null)
            }
            catch (e: Exception) {
                Log.e("SERVER URL Exception", "${e.message}")
                e.printStackTrace()
                done(e)
            }
        }
    }
}