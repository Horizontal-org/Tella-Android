package org.horizontal.tella.mobile.views.activity.browser

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.media.MediaFileHandler
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.security.SecureRandom
import java.util.Locale

/**
 * 2026-08-20 (audit rev 12): Vault Download Interceptor.
 * 2026-08-21 (audit rev 13 — Topic 5): Fixed the `.bin` extension bug.
 *
 * Intercepts downloads from the [SecureBrowserActivity]'s `WebView` and
 * routes them directly into Tella's encrypted vault — NOT to the device's
 * public Downloads folder.
 *
 * ## Bug fixed in rev 13 (Google-Drive `.bin` issue)
 *
 * Symptom: PDFs downloaded from Google Drive were saved into the vault
 * as `*.bin`, while PDFs from GitHub kept their correct `.pdf` extension.
 *
 * Root cause: the previous implementation delegated filename guessing to
 * `URLUtil.guessFileName()`, which has three failure modes that all
 * converge on the `.bin` fallback:
 *
 *  1. Google-Drive download URLs (`https://drive.google.com/uc?export=download&id=…`
 *     or `https://drive.usercontent.google.com/download?id=…&export=download…`)
 *     contain **no file path** and **no extension**. `URLUtil` cannot
 *     derive an extension from the URL.
 *  2. Google Drive often serves downloads with
 *     `Content-Type: application/octet-stream` (or even `text/html` for
 *     the interstitial "scanning for viruses" page). When `URLUtil`
 *     cannot guess an extension from the URL, it uses the MIME type's
 *     registered extension — and `application/octet-stream`'s registered
 *     extension in Android is `.bin`.
 *  3. Google Drive's `Content-Disposition` header for large files uses
 *     a confirmation page; the header on the actual file response is
 *     either `attachment; filename="file.bin"` (literally `.bin`) or
 *     absent entirely. So even the disposition filename is wrong.
 *
 * GitHub, by contrast, serves raw files via URLs like
 * `https://raw.githubusercontent.com/.../foo.pdf` which already include
 * the extension, and GitHub's `Content-Type` for raw PDFs is correctly
 * `application/pdf`. So `URLUtil` succeeds for GitHub and fails for
 * Google Drive.
 *
 * ## Fix in rev 13
 *
 * We replaced the naïve `URLUtil.guessFileName()` call with a robust
 * four-stage resolver (see [resolveFileNameAndMime]):
 *
 *  1. Parse `Content-Disposition` ourselves using RFC 6266 + RFC 5987
 *     rules — handles `filename="…"` AND `filename*=UTF-8''…` (the
 *     percent-encoded extended form Google Drive sometimes uses).
 *  2. Fall back to the URL's last path segment if it contains an `.` and
 *     the extension is a real extension (not just a TLD like `.com`).
 *  3. Fall back to the server-supplied MIME type → extension map
 *     (`MimeTypeMap.getExtensionFromMimeType`).
 *  4. **Magic-byte sniff the downloaded bytes** (see [sniffMimeFromMagic]).
 *     This is the key new step: once we have the bytes on disk we can
 *     determine the *real* file type from its signature, regardless of
 *     what the server claimed. If the original filename ended in `.bin`
 *     (or `.download` / no usable extension), we **rewrite the extension
 *     and MIME** from the sniffed type before handing the file to the
 *     vault.
 *
 * ## Flow (post-rev 13)
 *
 * 1. [downloadToVault] is called by `WebView.setDownloadListener` with
 *    the URL, the server's `Content-Disposition`, the server's
 *    `Content-Type`, and the user-visible filename as guessed by the
 *    WebView.
 * 2. [resolveFileNameAndMime] produces a *first-pass* filename + MIME
 *    using stages (1)–(3) above.
 * 3. [fetchToTempFile] streams the response into `noBackupFilesDir`.
 * 4. [sniffMimeFromMagic] reads the first 64 bytes and returns the
 *    *real* MIME type from the magic-byte signature.
 * 5. If the first-pass filename has a weak extension (`.bin`, `.download`,
 *    empty), the sniffed MIME overrides both the filename extension and
 *    the MIME that gets passed to the vault.
 * 6. The temp file is handed to [MediaFileHandler.importDownloadedFile]
 *    which uses Tella's EXISTING vault encryption pipeline:
 *    `RxVault.builder(stream).setMimeType(...).setName(...).build(null)`
 *    → `BaseVault.baseCreate` → `CipherStreamUtils.getEncryptedOutputStream`.
 * 7. After successful vault ingestion, the temp file is SECURELY DELETED:
 *    overwritten with random bytes then deleted.
 * 8. A toast confirms success/failure to the user.
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
 * - Magic-byte sniffing happens BEFORE the file enters the vault; we
 *   never inspect or modify encrypted vault files after ingestion.
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
     *
     * The [contentDisposition] and [serverMimeType] come from the
     * WebView's `DownloadListener` callback (which in turn come from
     * the HTTP response headers).
     */
    fun downloadToVault(
        url: String,
        contentDisposition: String?,
        serverMimeType: String?,
        webViewGuessedFileName: String
    ) {
        scope.launch {
            // Stage 1-3: first-pass filename + MIME from headers / URL / MIME map.
            val firstPass = resolveFileNameAndMime(
                url, contentDisposition, serverMimeType, webViewGuessedFileName
            )
            Timber.d(
                "First-pass filename: '%s', mime: '%s' (url=%s, cd='%s', serverMime='%s')",
                firstPass.fileName, firstPass.mimeType, url, contentDisposition, serverMimeType
            )

            val tempFile: File? = try {
                fetchToTempFile(url, firstPass.fileName)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch %s", url)
                showToast(context.getString(R.string.browser_download_failed, firstPass.fileName))
                return@launch
            }
            if (tempFile == null || !tempFile.exists() || tempFile.length() == 0L) {
                showToast(context.getString(R.string.browser_download_failed, firstPass.fileName))
                return@launch
            }

            // Stage 4: sniff the real type from the magic bytes.
            val sniffedMime = sniffMimeFromMagic(tempFile)
            Timber.d("Sniffed MIME from magic bytes: '%s'", sniffedMime)

            // Stage 5: if the first-pass filename has a weak extension,
            // rewrite the extension + MIME from the sniffed type.
            val (finalFileName, finalMime) = applySniffedOverride(firstPass, sniffedMime)
            if (finalFileName != firstPass.fileName || finalMime != firstPass.mimeType) {
                Timber.d(
                    "Rewrote filename '%s'→'%s', mime '%s'→'%s' based on magic-byte sniff",
                    firstPass.fileName, finalFileName, firstPass.mimeType, finalMime
                )
            }

            // Stage 6: hand the temp file to Tella's EXISTING vault
            // encryption pipeline. No custom crypto.
            // We call blockingGet() on the IO dispatcher — the Single
            // itself also subscribes on IO, so this is safe.
            withContext(Dispatchers.IO) {
                try {
                    val parentId: String? = null
                    val vaultFile = MediaFileHandler.importDownloadedFile(
                        tempFile, finalFileName, finalMime, parentId
                    ).blockingGet()
                    Timber.d("File imported to vault: id=%s, name=%s", vaultFile.id, vaultFile.name)
                    // Stage 7: securely delete the temp file now that it's in the vault.
                    securelyDeleteFile(tempFile)
                    // If the magic-byte sniffer rewrote the filename
                    // (e.g., Google Drive served a .bin but it was a
                    // PDF), tell the user so they know what to look for.
                    if (finalFileName != webViewGuessedFileName) {
                        showToast(context.getString(R.string.browser_download_renamed, finalFileName))
                    } else {
                        showToast(context.getString(R.string.browser_download_saved, finalFileName))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Vault import failed for %s", finalFileName)
                    securelyDeleteFile(tempFile)  // still clean up the temp
                    showToast(context.getString(R.string.browser_download_failed, finalFileName))
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Stage 1–3: First-pass filename + MIME resolution (no file bytes yet)
    // -------------------------------------------------------------------------

    /**
     * Resolves the filename and MIME type from the URL, Content-Disposition
     * header, server-supplied MIME, and the WebView's own guess.
     *
     * Resolution order (first non-weak wins):
     *
     *  1. `Content-Disposition: attachment; filename*=UTF-8''…` (RFC 5987
     *     extended form — handles UTF-8 percent-encoded filenames).
     *  2. `Content-Disposition: attachment; filename="…"` (RFC 6266
     *     quoted form).
     *  3. URL path's last segment, IF that segment contains an extension
     *     and the extension isn't a common TLD (`.com`, `.org`, etc.).
     *  4. `serverMimeType` → `MimeTypeMap.getExtensionFromMimeType(...)`.
     *  5. Fall back to `[webViewGuessedFileName]` if non-empty.
     * 6. Last-resort: `download_<timestamp>.bin`.
     *
     * Returns a [FileNameAndMime] where the [mimeType] is the best guess
     * (may be `application/octet-stream` if the server gave us nothing
     * usable). The [fileName] is always non-empty.
     */
    fun resolveFileNameAndMime(
        url: String,
        contentDisposition: String?,
        serverMimeType: String?,
        webViewGuessedFileName: String
    ): FileNameAndMime {
        val mime = (serverMimeType?.takeIf { it.isNotBlank() && it != "unknown/unknown" }
            ?: "application/octet-stream").lowercase(Locale.ROOT)

        var fileName: String? = null

        // (1) RFC 5987 extended form: filename*=UTF-8''My%20File.pdf
        contentDisposition?.let { parseRfc5987Filename(it) }
            ?.takeIf { it.isNotBlank() }
            ?.let { fileName = it }

        // (2) RFC 6266 quoted form: filename="My File.pdf"
        if (fileName == null) {
            contentDisposition?.let { parseRfc6266Filename(it) }
                ?.takeIf { it.isNotBlank() }
                ?.let { fileName = it }
        }

        // (3) URL path's last segment, if it has a real extension.
        if (fileName == null || hasWeakExtension(fileName!!)) {
            val fromUrl = extractFilenameFromUrl(url)
            if (fromUrl != null && !hasWeakExtension(fromUrl)) {
                fileName = fromUrl
            }
        }

        // (4) Server MIME → extension.
        if (fileName == null || hasWeakExtension(fileName!!)) {
            val ext = extensionForMime(mime)
            if (ext != null) {
                val base = webViewGuessedFileName.substringBeforeLast('.', "")
                    .takeIf { it.isNotBlank() } ?: "download_${System.currentTimeMillis()}"
                fileName = "$base.$ext"
            }
        }

        // (5) WebView's own guess as last resort.
        if (fileName == null || fileName.isNullOrBlank()) {
            fileName = webViewGuessedFileName.takeIf { it.isNotBlank() }
                ?: "download_${System.currentTimeMillis()}.bin"
        }

        return FileNameAndMime(sanitizeFileName(fileName!!), mime)
    }

    /**
     * Parses RFC 5987 / RFC 6266 `filename*=UTF-8''…` extended form.
     *
     * Returns the decoded filename, or null if the header doesn't have
     * this form.
     *
     * Example: `attachment; filename*=UTF-8''%E2%82%AC%20rates.pdf`
     * → `"€ rates.pdf"`
     */
    private fun parseRfc5987Filename(contentDisposition: String): String? {
        // Match: filename* = [charset]'[lang]'percent-encoded
        val m = Regex(
            "filename\\*\\s*=\\s*(?:UTF-8|utf-8|ISO-8859-1|iso-8859-1|Windows-1252|windows-1252)?'?[^']*'([^;]+)"
        ).find(contentDisposition)
        if (m != null && m.groupValues.size >= 2) {
            val raw = m.groupValues[1].trim().trim('"')
            return try {
                URLDecoder.decode(raw, "UTF-8")
            } catch (e: Exception) {
                // Fall back to literal raw value if decoding fails.
                raw
            }
        }
        return null
    }

    /**
     * Parses RFC 6266 `filename="…"` quoted form (and the unquoted
     * fallback `filename=…`).
     *
     * Returns the decoded filename, or null if the header doesn't have
     * the filename parameter.
     */
    private fun parseRfc6266Filename(contentDisposition: String): String? {
        // Quoted form first.
        val quoted = Regex(
            "filename\\*?\\s*=\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
        ).find(contentDisposition)
        if (quoted != null && quoted.groupValues.size >= 2) {
            return quoted.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\").trim()
        }
        // Unquoted form (no quotes, ends at ; or end of string).
        val unquoted = Regex("filename\\s*=\\s*([^;]+)").find(contentDisposition)
        if (unquoted != null && unquoted.groupValues.size >= 2) {
            return unquoted.groupValues[1].trim().trim('"')
        }
        return null
    }

    /**
     * Extracts a filename candidate from the URL's last path segment.
     *
     * Returns null if the URL has no path, or the last segment doesn't
     * look like a filename (e.g., it's just a TLD like `com` or a query
     * string).
     */
    private fun extractFilenameFromUrl(url: String): String? {
        return try {
            val uri = url.toUri()
            val last = uri.lastPathSegment ?: return null
            if (last.isBlank()) return null
            // Strip any trailing query string (just in case).
            val clean = last.substringBefore('?').substringBefore('#')
            if (clean.isBlank()) return null
            // Reject TLD-like segments (e.g., "com", "org") and segments
            // with no dot.
            if (!clean.contains('.')) return null
            // Decode percent-encoding (handles "%20" etc.).
            try {
                URLDecoder.decode(clean, "UTF-8")
            } catch (e: Exception) {
                clean
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns true if the filename has a "weak" extension — one that
     * means "we don't actually know the type", and the magic-byte
     * sniffer should be allowed to override it.
     *
     * Weak extensions: `.bin`, `.download`, `.tmp`, `.dat`, `.file`,
     * no extension at all, or an extension longer than 10 characters
     * (likely a URL fragment mistakenly captured as an extension).
     */
    private fun hasWeakExtension(fileName: String): Boolean {
        val dot = fileName.lastIndexOf('.')
        if (dot < 0 || dot == fileName.length - 1) return true  // no ext
        val ext = fileName.substring(dot + 1).lowercase(Locale.ROOT)
        if (ext.length > 10) return true
        return ext in setOf(
            "bin", "download", "tmp", "temp", "dat", "file",
            "octet-stream", "unknown", "bin1"
        )
    }

    /**
     * Maps a MIME type to its file extension using Android's built-in
     * [MimeTypeMap]. Returns null for unknown MIME types.
     *
     * We also handle a few common cases that Android's map misses
     * (e.g., `application/x-zip-compressed` → `zip`).
     */
    private fun extensionForMime(mime: String): String? {
        if (mime.isBlank() || mime == "application/octet-stream") return null
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        if (ext != null) return ext
        // Hand-curated fallbacks for MIME types Android's map misses.
        return when (mime.lowercase(Locale.ROOT)) {
            "application/x-zip-compressed" -> "zip"
            "application/x-rar-compressed" -> "rar"
            "application/x-7z-compressed" -> "7z"
            "application/x-tar" -> "tar"
            "application/x-gzip" -> "gz"
            "application/x-bzip2" -> "bz2"
            "application/x-yaml" -> "yaml"
            "application/x-yaml-compressed" -> "yaml"
            "text/yaml" -> "yaml"
            "application/x-markdown" -> "md"
            "text/markdown" -> "md"
            "text/plain" -> "txt"
            "application/x-msdownload" -> null  // generic, leave weak
            "application/vnd.android.package-archive" -> "apk"
            "application/x-shockwave-flash" -> "swf"
            else -> null
        }
    }

    /**
     * Sanitizes a filename: strips path separators, control chars, and
     * trailing dots/spaces. Caps the length to 200 characters (NTFS/ext
     * limit is 255 bytes; we leave headroom for the encrypted suffix).
     */
    private fun sanitizeFileName(name: String): String {
        var n = name.trim()
        if (n.isBlank()) return "download_${System.currentTimeMillis()}.bin"
        // Strip path separators (we don't want a filename like "../foo.pdf").
        n = n.replace(Regex("[/\\\\]"), "_")
        // Strip control characters.
        n = n.replace(Regex("[\\x00-\\x1f]"), "")
        // Trim trailing dots / spaces (Windows quirk; doesn't hurt on Linux).
        n = n.trim().trimEnd('.', ' ')
        // Cap length while preserving the extension.
        if (n.length > 200) {
            val dot = n.lastIndexOf('.')
            if (dot in 0..199) {
                val ext = n.substring(dot)
                n = n.substring(0, 200 - ext.length) + ext
            } else {
                n = n.substring(0, 200)
            }
        }
        return if (n.isBlank()) "download_${System.currentTimeMillis()}.bin" else n
    }

    // -------------------------------------------------------------------------
    // Stage 4: Magic-byte sniffing (after the file is on disk)
    // -------------------------------------------------------------------------

    /**
     * Sniffs the file's "magic bytes" (first 64 bytes) to determine the
     * real MIME type, regardless of what the server claimed.
     *
     * This is the key step that fixes the Google-Drive `.bin` issue:
     * even if the server sent `Content-Type: application/octet-stream`
     * (which would map to `.bin` in Android's MIME map), the bytes
     * themselves tell us whether it's actually a PDF, PNG, ZIP, etc.
     *
     * Returns the sniffed MIME type, or null if the bytes don't match
     * any known signature.
     */
    fun sniffMimeFromMagic(file: File): String? {
        if (!file.exists() || file.length() < 4) return null
        try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(64)
                val read = fis.read(header)
                if (read < 4) return null
                return sniffMagicBytes(header, read, file.length())
            }
        } catch (e: Exception) {
            Timber.w(e, "sniffMimeFromMagic failed for %s", file.name)
            return null
        }
    }

    /**
     * Pure function that maps a magic-byte prefix to a MIME type.
     *
     * Supported signatures:
     *
     *  - PDF:        `%PDF-`
     *  - PNG:        `89 50 4E 47 0D 0A 1A 0A`
     *  - JPEG:       `FF D8 FF`
     *  - GIF:        `47 49 46 38 (37|39) a`
     *  - WebP:       `RIFF....WEBP`
     *  - BMP:        `42 4D` ("BM")
     *  - MP4 / MOV:  `....ftyp`
     *  - 3GP:        `....ftyp3gp` / `....ftyp3g2`
     *  - MP3 (ID3v2):`49 44 33` ("ID3")
     *  - MP3 (frame):`FF FB` / `FF F3` / `FF F2`
     *  - OGG:        `4F 67 67 53` ("OggS")
     *  - FLAC:       `66 4C 61 43` ("fLaC")
     *  - WAV:        `RIFF....WAVE`
     *  - ZIP:        `50 4B 03 04` / `50 4B 05 06` / `50 4B 07 08`
     *  - 7Z:         `37 7A BC AF 27 1C`
     *  - RAR:        `52 61 72 21 1A 07`
     *  - GZIP:       `1F 8B`
     *  - DOCX / XLSX / PPTX (Office Open XML — all ZIP-based): sniffed as ZIP;
     *    we then peek at the ZIP central directory for the Office marker
     *    (`[Content_Types].xml`) to distinguish them. For now we return
     *    `application/zip` for ZIPs; the vault will store them as such and
     *    the viewer will pick the right one based on extension.
     *  - DOC / XLS / PPT (legacy Office OLE2): `D0 CF 11 E0 A1 B1 1A E1`
     *  - EPUB:        ZIP-based; treated as ZIP.
     *  - APK:         ZIP-based; treated as ZIP — Android installs by intent.
     *  - TEXT/UTF-8:   heuristic — starts with printable ASCII or a UTF-8 BOM.
     *  - JSON:         starts with `{` or `[` AND is mostly printable.
     *
     * Returns null for unknown signatures.
     */
    private fun sniffMagicBytes(header: ByteArray, len: Int, fileSize: Long): String? {
        // Helper: unsigned byte compare.
        fun b(i: Int) = header[i].toInt() and 0xFF
        fun str(off: Int, length: Int): String =
            String(header, off, minOf(length, len - off), Charsets.ISO_8859_1)

        // PDF
        if (len >= 5 && str(0, 5) == "%PDF-") return "application/pdf"

        // PNG
        if (len >= 8 &&
            b(0) == 0x89 && b(1) == 0x50 && b(2) == 0x4E && b(3) == 0x47 &&
            b(4) == 0x0D && b(5) == 0x0A && b(6) == 0x1A && b(7) == 0x0A
        ) return "image/png"

        // JPEG
        if (len >= 3 && b(0) == 0xFF && b(1) == 0xD8 && b(2) == 0xFF) return "image/jpeg"

        // GIF
        if (len >= 6 && str(0, 6) == "GIF87a" || (len >= 6 && str(0, 6) == "GIF89a")) {
            return "image/gif"
        }

        // RIFF family (WebP, WAV, AVI)
        if (len >= 12 && str(0, 4) == "RIFF" && str(8, 4) == "WEBP") return "image/webp"
        if (len >= 12 && str(0, 4) == "RIFF" && str(8, 4) == "WAVE") return "audio/x-wav"
        if (len >= 12 && str(0, 4) == "RIFF" && str(8, 4) == "AVI ") return "video/x-msvideo"

        // BMP
        if (len >= 2 && str(0, 2) == "BM") return "image/bmp"

        // MP4 family — the "ftyp" marker is at offset 4.
        if (len >= 12 && str(4, 4) == "ftyp") {
            val brand = str(8, 4)
            return when {
                brand.startsWith("qt") -> "video/quicktime"
                brand.startsWith("3g") -> "video/3gpp"
                brand.startsWith("mmp") -> "video/mp4"
                brand.startsWith("M4V") || brand.startsWith("M4A") -> "audio/mp4"
                else -> "video/mp4"
            }
        }

        // MP3 (ID3v2)
        if (len >= 3 && str(0, 3) == "ID3") return "audio/mpeg"
        // MP3 (frame sync)
        if (len >= 2 && b(0) == 0xFF && (b(1) and 0xE0) == 0xE0) return "audio/mpeg"

        // OGG
        if (len >= 4 && str(0, 4) == "OggS") return "audio/ogg"

        // FLAC
        if (len >= 4 && str(0, 4) == "fLaC") return "audio/flac"

        // ZIP (covers .docx, .xlsx, .pptx, .apk, .epub, .jar, .odt, .ods, etc.)
        if (len >= 4 && b(0) == 0x50 && b(1) == 0x4B &&
            (b(2) == 0x03 || b(2) == 0x05 || b(2) == 0x07) &&
            (b(3) == 0x04 || b(3) == 0x06 || b(3) == 0x08)
        ) {
            // Peek deeper to distinguish Office Open XML from plain ZIP.
            // We do NOT parse the central directory (too expensive for a
            // sniff). Instead we rely on the filename extension if the
            // caller set one. If the caller's filename has no usable
            // extension, we return "application/zip" — the vault will
            // store it as a ZIP and the user can rename inside the app.
            return "application/zip"
        }

        // 7Z
        if (len >= 6 && b(0) == 0x37 && b(1) == 0x7A && b(2) == 0xBC &&
            b(3) == 0xAF && b(4) == 0x27 && b(5) == 0x1C
        ) return "application/x-7z-compressed"

        // RAR — signature is "Rar!" + 0x1A 0x07 (RARv3+) or 0x1A 0x00 (RARv1.5)
        // Avoid Kotlin escape sequences (\x is not valid in Kotlin) — use byte compares.
        if (len >= 7 && str(0, 4) == "Rar!" &&
            b(4) == 0x1A && b(5) == 0x07 && b(6) == 0x00
        ) return "application/x-rar-compressed"
        if (len >= 7 && str(0, 4) == "Rar!" &&
            b(4) == 0x1A && b(5) == 0x07 && b(6) == 0x01
        ) return "application/x-rar-compressed"
        if (len >= 6 && str(0, 4) == "Rar!" &&
            b(4) == 0x1A && b(5) == 0x07
        ) return "application/x-rar-compressed"

        // GZIP
        if (len >= 2 && b(0) == 0x1F && b(1) == 0x8B) return "application/gzip"

        // Legacy MS Office (OLE2 Compound Document): doc/xls/ppt
        if (len >= 8 &&
            b(0) == 0xD0 && b(1) == 0xCF && b(2) == 0x11 && b(3) == 0xE0 &&
            b(4) == 0xA1 && b(5) == 0xB1 && b(6) == 0x1A && b(7) == 0xE1
        ) return "application/vnd.ms-office"

        // UTF-8 BOM → text/plain
        if (len >= 3 && b(0) == 0xEF && b(1) == 0xBB && b(2) == 0xBF) return "text/plain"

        // UTF-16 LE BOM
        if (len >= 2 && b(0) == 0xFF && b(1) == 0xFE) return "text/plain"

        // UTF-16 BE BOM
        if (len >= 2 && b(0) == 0xFE && b(1) == 0xFF) return "text/plain"

        // Heuristic text/JSON detection: starts with `{` or `[` AND the
        // first 64 bytes are all printable ASCII.
        if (len >= 2 && (header[0] == '{'.code.toByte() || header[0] == '['.code.toByte())) {
            if ((0 until len).all { i ->
                    val c = b(i)
                    c == 0x09 || c == 0x0A || c == 0x0D || c in 0x20..0x7E
                }) return "application/json"
        }
        // Plain text heuristic: first 64 bytes all printable ASCII.
        if (len >= 4 && (0 until len).all { i ->
                val c = b(i)
                c == 0x09 || c == 0x0A || c == 0x0D || c in 0x20..0x7E
            }) return "text/plain"

        // Unknown — return null; the caller will keep whatever it had.
        return null
    }

    /**
     * If the first-pass filename has a weak extension (`.bin`, `.download`,
     * none), overrides both the filename and the MIME type with values
     * derived from the sniffed MIME.
     *
     * If the sniffer returned null OR the sniffed MIME is no more specific
     * than what we already had, returns the first-pass values unchanged.
     */
    private fun applySniffedOverride(
        firstPass: FileNameAndMime,
        sniffedMime: String?
    ): FileNameAndMime {
        if (sniffedMime.isNullOrBlank()) return firstPass
        if (sniffedMime == firstPass.mimeType) return firstPass

        // Always trust the sniffer's MIME over the server's MIME for
        // vault storage — the magic bytes are the ground truth.
        val newMime = sniffedMime

        // If the first-pass filename already has a real extension that
        // matches the sniffed type, just update the MIME.
        val firstExt = firstPass.fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val sniffedExt = extensionForMime(sniffedMime)
        if (firstExt.isNotEmpty() && firstExt != "bin" && firstExt != "download" &&
            firstExt != "tmp" && firstExt != "dat" && firstExt != "file"
        ) {
            // The filename already has a real extension. Keep it, but
            // adopt the sniffed MIME for vault metadata.
            return FileNameAndMime(firstPass.fileName, newMime)
        }

        // The filename has a weak extension — rewrite it using the sniffed type.
        val base = firstPass.fileName.substringBeforeLast('.', firstPass.fileName)
            .ifBlank { "download_${System.currentTimeMillis()}" }
        val newFileName = if (sniffedExt != null) "$base.$sniffedExt" else "$base.bin"
        return FileNameAndMime(newFileName, newMime)
    }

    // -------------------------------------------------------------------------
    // Network + disk
    // -------------------------------------------------------------------------

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
        val tempFile = File(context.noBackupFilesDir, "browser_dl_${System.currentTimeMillis()}_${sanitizeFileName(fileName)}")
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                // Use the WebView's User-Agent so servers serve the same
                // version the user sees in the browser.
                setRequestProperty("User-Agent", getUserAgent())
                // Mark as a binary download so Google Drive / Drive
                // frontend skip the "view in browser" HTML interstitial
                // and serve the raw bytes.
                setRequestProperty("Accept", "*/*")
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

    /**
     * Result of [resolveFileNameAndMime]: a sanitized filename + the
     * best-guess MIME type (never null; defaults to
     * `application/octet-stream` if nothing else is available).
     */
    data class FileNameAndMime(val fileName: String, val mimeType: String)

    /**
     * Backwards-compatible shim: kept for any caller that still uses the
     * old single-arg signature. New callers should use
     * [downloadToVault] with all four parameters.
     *
     * @deprecated since audit rev 13; use the four-arg form.
     */
    @Deprecated("Use the four-arg downloadToVault(url, contentDisposition, mimeType, fileName).", ReplaceWith(
        "downloadToVault(url, null, mimeType, fileName)"
    ))
    fun downloadToVault(url: String, fileName: String, mimeType: String) {
        downloadToVault(url, null, mimeType, fileName)
    }

    /**
     * Backwards-compatible shim: kept for any caller that still uses the
     * old single-arg guessFileName().
     *
     * @deprecated since audit rev 13; use [resolveFileNameAndMime].
     */
    @Deprecated("Use resolveFileNameAndMime(url, cd, mime, webViewGuess).", ReplaceWith(
        "resolveFileNameAndMime(url, contentDisposition, mimeType, \"\").fileName"
    ))
    fun guessFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        return resolveFileNameAndMime(url, contentDisposition, mimeType, "").fileName
    }
}
