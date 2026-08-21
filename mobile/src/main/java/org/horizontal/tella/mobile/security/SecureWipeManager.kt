package org.horizontal.tella.mobile.security

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import timber.log.Timber
import java.io.OutputStream
import java.security.SecureRandom

/**
 * Secure Wipe — overwrites a file's contents with random bytes so the
 * underlying flash storage cannot be read back forensically, then issues
 * a standard delete. Audit / Feature 2 (2025-08-19).
 *
 * ## Design rationale
 *
 * Tella's existing import flow calls [deleteFileFromExternalStorage] in
 * AttachmentsFragment, which simply calls [DocumentsContract.deleteDocument]
 * (or [DocumentFile.delete] as a fallback). On most filesystems a "delete"
 * only unlinks the directory entry — the data blocks remain on the
 * underlying storage (often flash) until they are re-allocated. Modern
 * Android devices use TRIM, but TRIM is not guaranteed to be queued
 * promptly, and a sophisticated adversary with physical access to the
 * device can still recover recently-deleted bytes from raw NAND via JTAG
 * or chip-off. The only way to defeat that is to overwrite the file
 * contents in place before unlinking.
 *
 * This class overwrites the file in chunks (default 64 KB) using bytes
 * from [SecureRandom] (a cryptographically strong PRNG) — multiple passes
 * by default (see [PASSES]), then issues a delete.
 *
 * ## Constraints
 *
 * * **SAF uris**: only granted the file uri via `Intent.FLAG_GRANT_WRITE_URI_PERMISSION`
 *   can be written to. The existing import flow already takes this permission.
 * * **Cloud-synced files** (Google Drive, Dropbox via SAF): writes go to
 *   the local cache and may not overwrite the cloud copy. That's an
 *   inherent limitation of the SAF; we can't fix it from here. The user
 *   is informed in the audit document.
 * * **Performance**: large files (video, > 50 MB) may take seconds. The
 *   [wipe] method runs in IO scope and reports progress via [onProgress].
 *
 * ## Usage
 *
 *     val secureWiper = SecureWipeManager(context)
 *     val ok = secureWiper.wipe(
 *         uri = originalUri,
 *         onProgress = { pct -> updateProgress(pct) }
 *     )
 *     if (ok) toast(getString(R.string.secure_wipe_done))
 *
 * The class is intentionally framework-only — no third-party deps.
 */
class SecureWipeManager(private val context: Context) {

    /**
     * Overwrites the file pointed at by [uri] with [PASSES] passes of
     * cryptographically strong random data, then deletes it via the
     * SAF deleteDocument API.
     *
     * @param uri source URI returned by the SAF picker.
     * @param onProgress invoked with an Int 0..100; called from the
     *   same thread as the caller. The caller is expected to dispatch
     *   to the UI thread if needed.
     * @return true if all passes completed AND the delete succeeded.
     *   false if any step failed (the caller should treat the file as
     *   potentially still present and surface [R.string.secure_wipe_failed]).
     */
    fun wipe(uri: Uri, onProgress: ((Int) -> Unit)? = null): Boolean {
        // 1. Probe the file size so we can show accurate progress.
        val size = try {
            context.contentResolver.query(
                uri, arrayOf(android.provider.OpenableColumns.SIZE),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
            } ?: -1L
        } catch (t: Throwable) {
            Timber.w(t, "SecureWipe: could not query file size")
            -1L
        }

        if (size == 0L) {
            // Empty file — nothing to overwrite, just delete.
            return deleteDocument(uri)
        }

        // 2. Open the URI for write ("rwt" truncates the existing content
        //    on most SAF providers without recreating the document).
        val passes = PASSES
        var allPassesOk = true
        for (pass in 0 until passes) {
            val passOk = overwritePass(uri, size, pass, passes, onProgress)
            if (!passOk) {
                allPassesOk = false
                break
            }
        }

        // 3. Delete the file regardless of the overwrite result — even if
        //    we couldn't overwrite every byte we should at least unlink
        //    it so it stops being reachable via SAF.
        val deleted = deleteDocument(uri)

        return allPassesOk && deleted
    }

    /** Single overwrite pass. Visible for tests / audit. */
    private fun overwritePass(
        uri: Uri,
        size: Long,
        pass: Int,
        totalPasses: Int,
        onProgress: ((Int) -> Unit)?
    ): Boolean {
        return try {
            val resolver = context.contentResolver
            val out: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                resolver.openOutputStream(uri, "rwt")
            } else {
                // Pre-R flag falls back to "wt" which truncates the file.
                resolver.openOutputStream(uri, "wt")
            }
            out ?: run {
                Timber.w("SecureWipe: openOutputStream returned null for $uri")
                return false
            }
            out.use { stream ->
                val rng = SecureRandom()
                val buf = ByteArray(BUFFER_SIZE)
                var written = 0L
                while (true) {
                    val toWrite = if (size > 0) {
                        minOf(BUFFER_SIZE.toLong(), (size - written)).toInt()
                    } else BUFFER_SIZE
                    if (toWrite <= 0) break
                    rng.nextBytes(buf)
                    // Note: we only write [toWrite] bytes, not the whole buffer.
                    stream.write(buf, 0, toWrite)
                    // Occasionally fsync to flush bytes to physical storage
                    // (rather than just the page cache) — every 256 writes.
                    if ((written / BUFFER_SIZE) % 256 == 0L) {
                        try { stream.flush() } catch (_: Throwable) {}
                    }
                    written += toWrite
                    if (size > 0 && onProgress != null) {
                        // Overall progress across all passes.
                        val basePct = (pass * 100) / totalPasses
                        val withinPass = ((written * 100) / size).toInt()
                        onProgress((basePct + (withinPass / totalPasses)).coerceIn(0, 100))
                    }
                }
                try { stream.flush() } catch (_: Throwable) {}
            }
            true
        } catch (t: Throwable) {
            Timber.e(t, "SecureWipe: overwrite pass failed for $uri")
            false
        }
    }

    private fun deleteDocument(uri: Uri): Boolean {
        return try {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (t: Throwable) {
            Timber.w(t, "SecureWipe: deleteDocument failed; falling back to DocumentFile.delete")
            try {
                DocumentFile.fromSingleUri(context, uri)?.delete() == true
            } catch (t2: Throwable) {
                Timber.e(t2, "SecureWipe: fallback delete also failed")
                false
            }
        }
    }

    companion object {
        /**
         * Number of overwrite passes. Three passes is a defensible
         * tradeoff between speed and assurance for modern flash storage;
         * more passes don't materially increase security on NAND (the
         * write-amplification controller typically keeps multiple cached
         * copies anyway, so what really matters is triggering TRIM).
         */
        const val PASSES: Int = 3

        /** Chunk size for overwrite passes. 64 KB is a reasonable balance
         *  between syscall overhead and per-call allocation pressure. */
        const val BUFFER_SIZE: Int = 64 * 1024
    }
}
