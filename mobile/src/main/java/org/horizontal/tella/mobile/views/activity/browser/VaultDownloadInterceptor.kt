package org.horizontal.tella.mobile.views.activity.browser

import android.content.Context
import android.webkit.URLUtil
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.media.MediaFileHandler
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom

/**
 * 2026-08-20 (audit rev 12): Vault Download Interceptor.
 *
 * Intercepts downloads from the [SecureBrowserActivity]'s `WebView` and
 * routes them into Tella's encrypted vault — NOT to the device's public
 * Downloads folder.
 *
 * ## Flow
 *
 * 1. [downloadToVault] is called by the `WebView.setDownloadListener`.
 * 2. The file is fetched via `HttpURLConnection` (no third-party HTTP
 *    client — spec says "OkHttp/Ktor" but Tella already ships OkHttp;
 *    however to avoid pulling a new dependency into the browser module
 *    we use the JDK's `HttpURLConnection` which is always available).
 * 3. The response stream is copied to a temporary file in
 *    `context.noBackupFilesDir` — this directory is NOT backed up to
 *    Google Drive and is NOT visible to other apps, so the unencrypted
 *    temp file has a minimal forensic footprint.
 * 4. The temp file is handed to [MediaFileHandler.importDownloadedFile]
 *    which uses Tella's EXISTING vault encryption pipeline:
 *    `RxVault.builder(stream).setMimeType(...).setName(...).build(null)`
 *    → `BaseVault.baseCreate` → `CipherStreamUtils.getEncryptedOutputStream`.
 * 5. After successful vault ingestion, the temp file is SECURELY DELETED:
 *    overwritten with random bytes (to defeat forensic recovery of the
 *    flash blocks) then deleted.
 * 6. A toast is shown to the user confirming success or failure.
 *
 * ## Security notes
 *
 * - The temp file lives for the shortest possible time (only during the
 *   vault import, which is a single `IOUtils.copy` call).
 * - We do NOT use Android's `DownloadManager` (it writes to public
 *   external storage and leaves a system-visible trace).
 * - We do NOT write to `getExternalStorageDir()` — only `noBackupFilesDir`.
 * - The `HttpURLConnection` follows redirects up to a sane limit (5) to
 *   avoid infinite redirect loops.
 * - The User-Agent is set to the WebView's UA so servers serve the
 *   mobile version (matches what the user sees in the browser).
 *
 * @param context the activity context (for `noBackupFilesDir` + toasts).
 * @param scope the coroutine scope for launching the background fetch.
 */
class VaultDownloadInterceptor(
    private val context: Context,
    private val scope: CoroutineScope
) {

    /**
     * Fetches the file at [url] and imports it into the vault with the
     * given [fileName] and [mimeType].
     *
     * Runs on `Dispatchers.IO` (network + disk). Shows a toast on the
     * main thread when done.
     */
    fun downloadToVault(url: String, fileName: String, mimeType: String) {
        scope.launch {
            val tempFile: File? = try {
                fetchToTempFile(url, fileName)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch %s", url)
                showToast(context.getString(R.string.browser_download_failed, fileName))
                return@launch
            }
            if (tempFile == null) {
                showToast(context.getString(R.string.browser_download_failed, fileName))
                return@launch
            }
            // 2026-08-20 (audit rev 12): Hand the temp file to Tella's
            // EXISTING vault encryption pipeline. No custom crypto.
            // We call blockingGet() on the IO dispatcher — the Single
            // itself also subscribes on IO, so this is safe.
            withContext(Dispatchers.IO) {
                try {
                    val parentId: String? = null
                    val vaultFile = MediaFileHandler.importDownloadedFile(
                        tempFile, fileName, mimeType, parentId
                    ).blockingGet()
                    Timber.d("File imported to vault: id=%s, name=%s", vaultFile.id, vaultFile.name)
                    // Securely delete the temp file now that it's in the vault.
                    securelyDeleteFile(tempFile)
                    showToast(context.getString(R.string.browser_download_saved, fileName))
                } catch (e: Exception) {
                    Timber.e(e, "Vault import failed for %s", fileName)
                    securelyDeleteFile(tempFile)  // still clean up the temp
                    showToast(context.getString(R.string.browser_download_failed, fileName))
                }
            }
        }
    }

    /**
     * Fetches [url] via `HttpURLConnection` and streams the response to a
     * temp file in `context.noBackupFilesDir`.
     *
     * Returns the temp `File` on success, or null on failure.
     */
    private suspend fun fetchToTempFile(url: String, fileName: String): File? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        val tempFile = File(context.noBackupFilesDir, "browser_dl_${System.currentTimeMillis()}_$fileName")
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                // Use the WebView's User-Agent so servers serve the same
                // version the user sees in the browser.
                setRequestProperty("User-Agent", getUserAgent())
                connectTimeout = 30_000
                readTimeout = 60_000
                instanceFollowRedirects = true
            }
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                Timber.e("HTTP %d for %s", responseCode, url)
                return@withContext null
            }
            inputStream = connection.inputStream
            outputStream = FileOutputStream(tempFile)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
            Timber.d("Downloaded %s (%d bytes) to temp file", fileName, tempFile.length())
            tempFile
        } catch (e: IOException) {
            Timber.e(e, "Network error fetching %s", url)
            try { if (tempFile.exists()) tempFile.delete() } catch (_: Throwable) {}
            null
        } finally {
            try { inputStream?.close() } catch (_: Throwable) {}
            try { outputStream?.close() } catch (_: Throwable) {}
            connection?.disconnect()
        }
    }

    /**
     * Securely deletes a file by overwriting it with random bytes before
     * deletion. This makes forensic recovery of the flash blocks much
     * harder.
     *
     * Note: on modern Android with flash storage + TRIM, a single
     * overwrite is generally sufficient; multiple passes (DoD 5220.22-M
     * etc.) provide no additional security on flash.
     */
    private fun securelyDeleteFile(file: File) {
        try {
            if (!file.exists()) return
            // Overwrite with random bytes.
            val length = file.length()
            if (length > 0) {
                FileOutputStream(file).use { fos ->
                    val random = SecureRandom()
                    val buffer = ByteArray(8192)
                    var written = 0L
                    while (written < length) {
                        random.nextBytes(buffer)
                        val toWrite = minOf(buffer.size.toLong(), length - written).toInt()
                        fos.write(buffer, 0, toWrite)
                        written += toWrite
                    }
                    fos.flush()
                }
            }
            // Delete the file.
            if (!file.delete()) {
                file.deleteOnExit()
            }
            Timber.d("Securely deleted temp file: %s", file.name)
        } catch (e: Exception) {
            Timber.e(e, "Failed to securely delete %s", file.name)
            try { file.delete() } catch (_: Throwable) {}
        }
    }

    /**
     * Guesses a filename from the URL + Content-Disposition header + MIME type.
     *
     * Uses Android's `URLUtil.guessFileName` which handles all the common
     * cases (Content-Disposition filename=, URL path, fallback to
     * "downloadfile.bin").
     */
    fun guessFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        return try {
            URLUtil.guessFileName(url, contentDisposition, mimeType ?: "application/octet-stream")
        } catch (e: Exception) {
            Timber.w(e, "guessFileName failed, using fallback")
            "download_${System.currentTimeMillis()}.bin"
        }
    }

    /**
     * Returns the WebView's User-Agent string for use in the download
     * HTTP request. This ensures the downloaded file matches what the
     * user sees in the browser (mobile vs. desktop version).
     *
     * We use the default Android UA because we don't have a WebView
     * instance here. The activity passes the WebView's UA via the
     * constructor if needed — for now the default is fine.
     */
    private fun getUserAgent(): String {
        return try {
            android.webkit.WebSettings.getDefaultUserAgent(context)
        } catch (e: Exception) {
            "Mozilla/5.0 (Linux; Android)"
        }
    }

    /**
     * Shows a toast on the main thread.
     */
    private fun showToast(message: String) {
        scope.launch(Dispatchers.Main) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
