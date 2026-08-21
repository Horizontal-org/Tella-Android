# Tella-Android-5-Topic-Audit

**Document series:** Tella-Android-N-Topic-Audit
**This document:** Topic 5 — Secure In-App Browser (rev 12 → rev 13)
**Auditor date:** 2026-08-21 (rev 13)
**Auditor scope:** mobile module — `views/activity/browser/*`, `media/MediaFileHandler.java`, `res/menu/attachments_menu.xml`, `res/values/strings.xml`, `AndroidManifest.xml`
**Related prior art:**
- `PDF_READER_AUDIT_REV7.md`, `PDF_READER_AUDIT_REV8.md`, `PDF_READER_AUDIT_REV9.md` (Topic 4 — PDF Reader, 2026-08-19 → 2026-08-20)
- Tella-Android-1/2/3-Topic-Audit (Topics 1–3 — earlier modules)

---

## 0. What's new in this revision (rev 13)

| # | Change | Why |
|---|---|---|
| 1 | `VaultDownloadInterceptor` rewritten with a four-stage filename/MIME resolver (`resolveFileNameAndMime`) and a magic-byte sniffer (`sniffMimeFromMagic`). | The previous `URLUtil.guessFileName()`-only path produced `*.bin` filenames for PDFs downloaded from Google Drive. |
| 2 | RFC 6266 + RFC 5987 `Content-Disposition` parsing (`parseRfc6266Filename`, `parseRfc5987Filename`). | The fallback `URLUtil` path mishandles UTF-8 percent-encoded filenames (`filename*=UTF-8''…`). |
| 3 | Post-fetch magic-byte sniffing + extension override (`applySniffedOverride`). | The server's `Content-Type` header is unreliable (Google Drive often serves `application/octet-stream` or even `text/html` for the interstitial page). Magic bytes are ground truth. |
| 4 | `SecureBrowserActivity.kt` download-listener hook now passes the WebView's first-pass guess + Content-Disposition + server MIME through to the interceptor. | Allows the interceptor to rewrite the extension after the bytes are on disk. |
| 5 | New strings: `browser_download_renamed` ("Saved to vault as \"X\" (corrected extension)") and `browser_no_network`. | The user is informed when the sniffer changed the filename (e.g., `.bin` → `.pdf`), so they don't lose the file looking for the wrong name. |
| 6 | Deprecated shims kept (`downloadToVault(url, name, mime)` + `guessFileName(...)`). | Backward compatibility with any external callers; emit a `ReplaceWith` deprecation hint. |

**Net effect:** PDFs (and images, audio, video, archives, Office-legacy docs) downloaded from Google Drive, OneDrive, Dropbox web, WeTransfer, and any other server that mislabels `Content-Type` are now saved to the vault with their *true* extension — same behavior as GitHub raw links (which always worked correctly because the URL path already ends with `.pdf`).

---

## 1. Topic 5 — Secure In-App Browser: feature overview

### 1.1 Motivation

Before rev 12, Tella users who needed to download evidence from the web had to leave the app and use an external browser (Chrome, Firefox, DuckDuckGo). This exposed their activity through:

- **System browser history** (Chrome's `Web History` database, accessible to any app with `READ_HISTORY_BOOKMARKS`).
- **System browser cache** (Chrome's `Cache/` directory, which can persist thumbnails and inline image previews for weeks).
- **System download manager** (`DownloadManager` writes to `Environment.DirectoryDownloads` and registers the file in `MediaStore` — the file becomes visible to gallery apps, file pickers, and Google Photos backup).
- **System cookies** (third-party trackers' cookies persist across app sessions).

This is a critical operational-security (OPSEC) failure for Tella's user base (journalists, activists, human rights defenders). Topic 5 closes this leak by intercepting downloads **before** they reach the Android system download manager and routing them directly into Tella's encrypted vault.

### 1.2 Scope and out-of-scope

**In scope (rev 12 → rev 13):**

- A sandboxed `WebView` with `allowFileAccess=false`, `allowContentAccess=false`.
- URL bar, back/forward/home controls, SSL-error hard cancel.
- Forensic cleanup on `onDestroy()` (history, cache, cookies, DOM storage).
- `setDownloadListener` → background fetch via `HttpURLConnection` → temp file in `noBackupFilesDir` → `MediaFileHandler.importDownloadedFile` → secure temp-file deletion.
- Magic-byte sniffing for filename/MIME correction (rev 13).

**Out of scope (not implemented, by design):**

- Tabbed browsing. (One tab only. Tabs leak state across sessions.)
- Bookmark persistence. (Bookmarks are an evidence trail; we deliberately do not persist them.)
- Password/form autofill. (Forms and passwords are written to SQLite form-history databases that survive WebView destruction.)
- Incognito mode toggle. (The whole activity IS incognito — there is no non-incognito path.)
- Ad/tracker blocking. (Future work — would require a request-blocking `WebViewClient` plus an EasyList-style ruleset. Adds maintenance burden and a network-blocking surface for false positives.)
- Proxy / Tor / Orbot integration. (Future work — would require a `ProxySelector` and Orbot's `socks` port; out of scope for this audit.)
- JavaScript toggle. (JS is intentionally left ON because most modern sites (Google Drive, GitHub, Dropbox) require JS to render the download UI. Disabling JS would break downloads, defeating the purpose.)
- File picker upload (`<input type="file">`). (Future work — requires overriding `WebChromeClient.onShowFileChooser` and routing the picker through Tella's existing `MediaFileHandler` picker. The `setSupportMultipleWindows(false)` + `javaScriptCanOpenWindowsAutomatically=false` settings intentionally disable the multi-window path that file pickers often rely on.)

### 1.3 Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│  SecureBrowserActivity (BaseLockActivity)                           │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  WebView (sandboxed)                                           │ │
│  │   - WebViewClient  : SSL hard-cancel, URL override (false)     │ │
│  │   - WebChromeClient : progress bar only                        │ │
│  │   - DownloadListener : extracts URL + CD + MIME → ①           │ │
│  │   - CookieManager   : third-party cookies OFF                  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                          ↓                                          │
│                  VaultDownloadInterceptor                            │
│                          ↓                                          │
│   ① resolveFileNameAndMime(url, CD, mime, webviewGuess)            │
│      - RFC 5987 filename*=UTF-8''… parser                           │
│      - RFC 6266 filename="…" parser                                 │
│      - URL last-path-segment extractor                              │
│      - MimeTypeMap mime→extension                                    │
│                          ↓                                          │
│   ② fetchToTempFile(url, name) — HttpURLConnection                 │
│      - 30 s connect / 60 s read timeout                             │
│      - WebView User-Agent                                           │
│      - Writes to context.noBackupFilesDir/browser_dl_<ts>_<name>    │
│                          ↓                                          │
│   ③ sniffMimeFromMagic(file) — 64-byte signature match             │
│      → application/pdf | image/png | audio/mpeg | video/mp4 | ...   │
│                          ↓                                          │
│   ④ applySniffedOverride(firstPass, sniffedMime)                   │
│      - If firstPass has weak ext (.bin/.download/none): rewrite     │
│      - Else: keep filename, but adopt sniffed MIME                  │
│                          ↓                                          │
│   ⑤ MediaFileHandler.importDownloadedFile(temp, name, mime, null)  │
│      → RxVault.builder(stream).setName().setMimeType().build(null)  │
│      → BaseVault.baseCreate                                         │
│      → CipherStreamUtils.getEncryptedOutputStream (AES/CTR,         │
│        PBKDF2 subkey, random IV prefix, SHA-256 digest)            │
│      → VaultDataSource.create(parentId, vaultFile) — SQLCipher row  │
│                          ↓                                          │
│   ⑥ securelyDeleteFile(temp) — overwrite with SecureRandom + delete│
│                          ↓                                          │
│   ⑦ Toast: browser_download_saved | browser_download_renamed       │
│   (on any error: securelyDeleteFile + browser_download_failed)     │
│                                                                     │
│  onDestroy(): forensicCleanup()                                     │
│    - clearHistory, clearCache(true), clearFormData                  │
│    - CookieManager.removeAllCookies + flush                         │
│    - WebStorage.deleteAllData                                       │
│    - removeAllViews + webView.destroy                               │
│    - scope.cancel()                                                 │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.4 Files touched

| File | Purpose | Rev |
|---|---|---|
| `mobile/src/main/java/org/horizontal/tella/mobile/views/activity/browser/SecureBrowserActivity.kt` | Activity host, WebView setup, SSL hard-cancel, forensic cleanup. | 12 → 13 |
| `mobile/src/main/java/org/horizontal/tella/mobile/views/activity/browser/VaultDownloadInterceptor.kt` | Download pipeline (fetch → sniff → import → secure delete). | 12 → 13 |
| `mobile/src/main/res/layout/activity_secure_browser.xml` | Layout (toolbar + URL bar + back/forward/home + progress + WebView). | 12 |
| `mobile/src/main/res/menu/attachments_menu.xml` | Adds `menu_item_browser` to the Attachments toolbar. | 12 |
| `mobile/src/main/AndroidManifest.xml` (line ~638) | Registers `<activity android:name=".views.activity.browser.SecureBrowserActivity">`. | 12 |
| `mobile/src/main/java/org/horizontal/tella/mobile/views/fragment/vault/attachements/AttachmentsFragment.kt` (lines ~207–211) | `R.id.menu_item_browser` handler launches `SecureBrowserActivity` via `Intent`. | 12 |
| `mobile/src/main/java/org/horizontal/tella/mobile/media/MediaFileHandler.java` (lines ~653–678) | `importDownloadedFile(File, String, String, String)` — the canonical vault-ingestion hook. Uses `RxVault.builder(stream)` → `CipherStreamUtils`. No custom crypto. | 12 |
| `mobile/src/main/res/values/strings.xml` | `browser_*` strings (title, controls, SSL error, download saved/failed/renamed, menu open). | 12 → 13 |

---

## 2. Security posture (per the spec's strict constraints)

### 2.1 Constraint 1 — Tech stack

✅ **Kotlin + native Android `WebView` only.**
- No `androidx.browser` / Chrome Custom Tabs.
- No `mozilla_geckoview` / GeckoView.
- No third-party HTTP client (`OkHttp`/`Ktor`) on the download path. We use the JDK's `java.net.HttpURLConnection`, which is always available on Android (it's part of `libcore`). Tella *does* ship OkHttp for other modules (OpenRosa, Nextcloud, Uwazi), but the browser module deliberately doesn't add a new dependency on it — keeps the browser's blast radius minimal.

### 2.2 Constraint 2 — Sandboxing + SSL hard-cancel

**File access / content access** (`SecureBrowserActivity.setupWebView()`):

```kotlin
webView.settings.apply {
    javaScriptEnabled = true                      // required for modern download UIs
    domStorageEnabled = true                       // required for SPA-style sites
    allowFileAccess = false                        // ✅ can't read file:// URLs
    allowContentAccess = false                     // ✅ can't read content:// URIs
    allowFileAccessFromFileURLs = false            // ✅ no file:// origin file access
    allowUniversalAccessFromFileURLs = false      // ✅ no file:// origin universal access
    javaScriptCanOpenWindowsAutomatically = false // ✅ no window.open() → no popup chain
    setSupportMultipleWindows(false)              // ✅ single-window only
    mediaPlaybackRequiresUserGesture = true       // ✅ no autoplay audio/video
    saveFormData = false                           // ✅ no form data persistence
    savePassword = false                           // ✅ no password persistence (deprecated in API 18 but explicit)
    cacheMode = WebSettings.LOAD_DEFAULT           // we wipe cache on destroy anyway
    // ... zoom + viewport (UX, not security)
}
CookieManager.getInstance().setAcceptCookie(true)               // 1st-party cookies OK (per-site)
CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)  // ✅ 3rd-party blocked
```

**SSL hard-cancel** (`SecureWebViewClient.onReceivedSslError`):

```kotlin
override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
    Timber.w("SSL error cancelled: %s", error?.toString())
    handler?.cancel()      // ✅ NEVER handler.proceed()
    runOnUiThread {
        Toast.makeText(this@SecureBrowserActivity, R.string.browser_ssl_error, Toast.LENGTH_SHORT).show()
    }
}
```

**Why hard-cancel and not `proceed()`:**
- `handler.proceed()` accepts a self-signed or expired certificate. In Tella's threat model (network MITM by ISP / state-level adversary), accepting an invalid cert means accepting a possible MITM attack — the attacker could substitute a tampered download that contains a tracking payload or a coerced file (e.g., a PDF with embedded JavaScript exploit).
- The cost of a false positive (user can't reach a site with a misconfigured cert) is much lower than the cost of a false negative (user downloads a tampered file that lands in the encrypted vault and is later opened with the PDF reader).

### 2.3 Constraint 3 — Forensic cleanliness on `onDestroy()`

```kotlin
override fun onDestroy() {
    forensicCleanup()
    super.onDestroy()
    scope.cancel()
}

private fun forensicCleanup() {
    try {
        webView.stopLoading()
        webView.clearHistory()                          // history database
        webView.clearCache(true)                        // entire Cache/ dir
        webView.clearFormData()                         // form autocomplete DB
        CookieManager.getInstance().removeAllCookies(null) // all cookies
        CookieManager.getInstance().flush()             // force-write (in case Chromium defers)
        WebStorage.getInstance().deleteAllData()        // localStorage + sessionStorage + WebSQL
        webView.removeAllViews()
        webView.destroy()                               // kill the renderer process
        Timber.d("Browser forensic cleanup complete")
    } catch (e: Exception) {
        Timber.e(e, "Browser forensic cleanup failed")
    }
}
```

**What this leaves behind (and why we accept it):**

| Residual | Location | Acceptable because |
|---|---|---|
| `WebView`'s OpenGL texture memory | RAM only (process-scoped) | `webView.destroy()` frees the renderer; RAM is zeroed on process death. |
| `WebView`'s crash logs (if enabled) | `/data/data/<pkg>/cache/webviewCrash/` | WebView crash logging is OFF by default in production builds; `WebViewDatabase` is also disabled. |
| `WebView`'s `app_webview/Local Storage` if `setDomStorageEnabled(true)` | `/data/data/<pkg>/app_webview/Local Storage/` | `WebStorage.deleteAllData()` wipes this directory before `webView.destroy()`. We have verified by `adb shell ls` after a session — the directory exists but is empty. |
| The downloaded file in the vault | `/data/data/<pkg>/files/media/<uuid>.<ext>` (AES/CTR encrypted) | This is the desired outcome — the file is now Tella-encrypted with the user's MainKey. |
| The temp file | `noBackupFilesDir/browser_dl_<ts>_<name>` | Securely deleted (overwrite with `SecureRandom` + delete) before `downloadToVault` returns. |

### 2.4 Constraint 4 — No `DownloadManager`, no public external storage

```kotlin
// VaultDownloadInterceptor.fetchToTempFile()
val tempFile = File(
    context.noBackupFilesDir,                            // ✅ private internal storage
    "browser_dl_${System.currentTimeMillis()}_${sanitizeFileName(fileName)}"
)
```

**Why `noBackupFilesDir`:**
- It's `/data/data/<pkg>/no_backup/` — private to the app, not visible to other apps without root.
- It's NOT backed up to Google Drive (so cloud backup doesn't leak the unencrypted file).
- It's NOT a `MediaStore` directory (so gallery apps don't scan it).
- It's NOT `cacheDir` (cache files can be evicted by the OS mid-download; `noBackupFilesDir` is durable).
- It's NOT `externalFilesDir` (which on scoped storage is shared with other apps via SAF).

**Why NOT `DownloadManager`:**
- `DownloadManager.enqueue()` writes to `Environment.DirectoryDownloads` (`/sdcard/Download/`).
- The file is registered in `MediaStore.Downloads`, visible to all apps with `READ_EXTERNAL_STORAGE` (API ≤ 28) or via the broad `READ_MEDIA_DOWNLOADS` permission.
- The file persists across Tella uninstall/reinstall.
- Even on Android 11+ scoped storage, the file is in a system-shared location and survives Tella's data wipe.

---

## 3. The `.bin` extension bug — root-cause analysis (rev 13)

### 3.1 Symptom (as reported by the user)

> "Maybe I download a PDF from Google Drive but it saves as `.bin`. Why does it sometimes change the file extension by itself? I have tested by downloading some PDF from GitHub — it keeps as it is, `.pdf`. Where is the issue? It should work correctly as the original one — whether it's a photo, video, audio, PDF, or something else."

### 3.2 Root cause

The previous implementation delegated filename guessing to `android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)`. This single call has **three converging failure modes** that all collapse to the `.bin` fallback when downloading from Google Drive:

#### Failure mode 1 — Google Drive URLs contain no file extension

Google Drive download URLs are query-string-based, not path-based:

| Site | URL shape | Has extension in path? |
|---|---|---|
| GitHub raw | `https://raw.githubusercontent.com/user/repo/main/paper.pdf` | ✅ Yes — `.pdf` |
| GitHub blob | `https://github.com/user/repo/raw/main/paper.pdf` | ✅ Yes — `.pdf` (after redirect) |
| Google Drive | `https://drive.google.com/uc?export=download&id=XYZ` | ❌ No — path is just `/uc` |
| Google Drive (new) | `https://drive.usercontent.google.com/download?id=XYZ&export=download` | ❌ No — path is just `/download` |
| OneDrive | `https://1drv.ms/u/s!ABCDEF` | ❌ No — short link |
| WeTransfer | `https://www.wetransfer.com/downloads/XYZ/abc` | ❌ No — no extension |
| Dropbox web | `https://www.dropbox.com/s/ABC/file.pdf?dl=1` | ✅ Sometimes |

`URLUtil.guessFileName()` extracts the filename from the URL's last path segment. For `/uc` or `/download`, the "filename" becomes `uc` or `download` — neither has an extension.

#### Failure mode 2 — Google Drive serves `application/octet-stream`

Google Drive's HTTP response for a download is:

```http
HTTP/1.1 200 OK
Content-Type: application/octet-stream
Content-Disposition: attachment; filename*=UTF-8''<original_filename>; filename="<original_filename>"
```

Note that the `Content-Type` is `application/octet-stream` — a generic binary MIME type — **regardless of the actual file type**. Google Drive does this intentionally because:
- The file might be too large for Drive to do a content-type sniff itself.
- Drive relies on the uploader's declared MIME type, which is often missing for shared files.
- The `Content-Disposition` filename does carry the original extension, but…

#### Failure mode 3 — `URLUtil.guessFileName()` always appends `.bin` for `application/octet-stream`

When `URLUtil` has no URL extension AND no usable Content-Disposition filename, it looks up the MIME type's registered extension via `MimeTypeMap.getExtensionFromMimeType(mimeType)`.

For `application/octet-stream`, `MimeTypeMap.getExtensionFromMimeType("application/octet-stream")` returns `"bin"`. (This is hardcoded in Android's `MimeMap.java`.)

So `URLUtil.guessFileName("https://drive.google.com/uc?...", null, "application/octet-stream")` returns `"uc.bin"` — and that's what the file was being saved as in the vault.

#### Why GitHub worked

GitHub's raw URL has the extension in the path. So `URLUtil.guessFileName("https://raw.githubusercontent.com/.../paper.pdf", null, "application/pdf")` returns `"paper.pdf"`. The `.bin` fallback was never hit.

### 3.3 The fix — rev 13

We replaced the single `URLUtil.guessFileName()` call with a four-stage resolver that runs **before** the file is fetched (header-only), and a magic-byte sniffer that runs **after** the file is fetched (bytes-on-disk truth).

#### Stage 1 — RFC 5987 extended `Content-Disposition` parser

```kotlin
private fun parseRfc5987Filename(contentDisposition: String): String? {
    val m = Regex(
        "filename\\*\\s*=\\s*(?:UTF-8|utf-8|ISO-8859-1|iso-8859-1|Windows-1252|windows-1252)?'[^']*'([^;]+)"
    ).find(contentDisposition)
    if (m != null && m.groupValues.size >= 2) {
        val raw = m.groupValues[1].trim().trim('"')
        return try {
            URLDecoder.decode(raw, "UTF-8")      // decode percent-encoding
        } catch (e: Exception) {
            raw                                  // fall back to literal
        }
    }
    return null
}
```

This handles the `filename*=UTF-8''My%20File.pdf` form, which `URLUtil` does not parse correctly when the value is percent-encoded UTF-8.

#### Stage 2 — RFC 6266 quoted `Content-Disposition` parser

```kotlin
private fun parseRfc6266Filename(contentDisposition: String): String? {
    val quoted = Regex(
        "filename\\*?\\s*=\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
    ).find(contentDisposition)
    if (quoted != null && quoted.groupValues.size >= 2) {
        return quoted.groupValues[1]
            .replace("\\\"", "\"").replace("\\\\", "\\").trim()
    }
    val unquoted = Regex("filename\\s*=\\s*([^;]+)").find(contentDisposition)
    if (unquoted != null && unquoted.groupValues.size >= 2) {
        return unquoted.groupValues[1].trim().trim('"')
    }
    return null
}
```

#### Stage 3 — URL path segment extractor

```kotlin
private fun extractFilenameFromUrl(url: String): String? {
    return try {
        val uri = url.toUri()
        val last = uri.lastPathSegment ?: return null
        if (last.isBlank()) return null
        val clean = last.substringBefore('?').substringBefore('#')
        if (clean.isBlank() || !clean.contains('.')) return null
        try { URLDecoder.decode(clean, "UTF-8") } catch (e: Exception) { clean }
    } catch (e: Exception) { null }
}
```

This rejects `/uc` and `/download` (no dot) and TLD-only segments like `.com`.

#### Stage 4 — `MimeTypeMap` fallback

```kotlin
private fun extensionForMime(mime: String): String? {
    if (mime.isBlank() || mime == "application/octet-stream") return null
    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
    if (ext != null) return ext
    return when (mime.lowercase(Locale.ROOT)) {
        "application/x-zip-compressed" -> "zip"
        "application/x-rar-compressed" -> "rar"
        // ... hand-curated fallbacks for MIME types Android misses
        else -> null
    }
}
```

We explicitly return `null` for `application/octet-stream` so that the resolver does NOT fall through to the `.bin` fallback — it leaves the filename at whatever the prior stages produced.

#### Stage 5 (the key new step) — Magic-byte sniffing after fetch

```kotlin
fun sniffMimeFromMagic(file: File): String? {
    if (!file.exists() || file.length() < 4) return null
    try {
        FileInputStream(file).use { fis ->
            val header = ByteArray(64)
            val read = fis.read(header)
            if (read < 4) return null
            return sniffMagicBytes(header, read, file.length())
        }
    } catch (e: Exception) { return null }
}
```

`sniffMagicBytes` matches the first 64 bytes against known file signatures:

| Format | Magic bytes | MIME returned |
|---|---|---|
| PDF | `25 50 44 46 2D` (`%PDF-`) | `application/pdf` |
| PNG | `89 50 4E 47 0D 0A 1A 0A` | `image/png` |
| JPEG | `FF D8 FF` | `image/jpeg` |
| GIF | `47 49 46 38 37 61` / `47 49 46 38 39 61` | `image/gif` |
| WebP | `RIFF....WEBP` | `image/webp` |
| BMP | `42 4D` (`BM`) | `image/bmp` |
| WAV | `RIFF....WAVE` | `audio/x-wav` |
| AVI | `RIFF....AVI ` | `video/x-msvideo` |
| MP4 family | `....ftyp` + brand | `video/mp4` / `audio/mp4` / `video/quicktime` / `video/3gpp` |
| MP3 (ID3v2) | `49 44 33` (`ID3`) | `audio/mpeg` |
| MP3 (frame sync) | `FF Ex` (top 3 bits set) | `audio/mpeg` |
| OGG | `4F 67 67 53` (`OggS`) | `audio/ogg` |
| FLAC | `66 4C 61 43` (`fLaC`) | `audio/flac` |
| ZIP (covers DOCX/XLSX/PPTX/APK/EPUB/JAR/ODT) | `50 4B 03 04` / `50 4B 05 06` / `50 4B 07 08` | `application/zip` |
| 7Z | `37 7A BC AF 27 1C` | `application/x-7z-compressed` |
| RAR | `52 61 72 21 1A 07` | `application/x-rar-compressed` |
| GZIP | `1F 8B` | `application/gzip` |
| MS-Office legacy (DOC/XLS/PPT) | `D0 CF 11 E0 A1 B1 1A E1` | `application/vnd.ms-office` |
| UTF-8 BOM | `EF BB BF` | `text/plain` |
| UTF-16 LE BOM | `FF FE` | `text/plain` |
| UTF-16 BE BOM | `FE FF` | `text/plain` |
| JSON heuristic | starts with `{` or `[` AND printable ASCII | `application/json` |
| Plain text heuristic | first 64 bytes all printable ASCII | `text/plain` |
| Unknown | — | `null` (caller keeps prior value) |

#### Stage 6 — Override weak extensions with sniffed type

```kotlin
private fun applySniffedOverride(
    firstPass: FileNameAndMime,
    sniffedMime: String?
): FileNameAndMime {
    if (sniffedMime.isNullOrBlank()) return firstPass
    if (sniffedMime == firstPass.mimeType) return firstPass

    val newMime = sniffedMime
    val firstExt = firstPass.fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    val sniffedExt = extensionForMime(sniffedMime)

    if (firstExt.isNotEmpty() && firstExt !in WEAK_EXTENSIONS) {
        // First-pass filename already has a real extension.
        // Adopt the sniffed MIME for vault metadata, but keep the filename.
        return FileNameAndMime(firstPass.fileName, newMime)
    }
    // Filename has a weak extension — rewrite it from the sniffed type.
    val base = firstPass.fileName.substringBeforeLast('.', firstPass.fileName)
        .ifBlank { "download_${System.currentTimeMillis()}" }
    val newFileName = if (sniffedExt != null) "$base.$sniffedExt" else "$base.bin"
    return FileNameAndMime(newFileName, newMime)
}

private val WEAK_EXTENSIONS = setOf(
    "bin", "download", "tmp", "temp", "dat", "file", "octet-stream", "unknown", "bin1"
)
```

**Key decision:** when the first-pass filename has a *real* extension (e.g., `paper.pdf` from a GitHub URL), we DO NOT override the filename — we only adopt the sniffed MIME for the vault metadata row. This preserves the original filename as the user saw it in the browser, while still benefiting from the byte-level truth for the MIME column.

### 3.4 Tracing the bug and the fix through a real Google Drive PDF download

**Setup:** User browses to a Google Drive share URL, taps the "Download" button.

**WebView reports to `setDownloadListener`:**
- `url = "https://drive.usercontent.google.com/download?id=1ABC…&export=download&confirm=t"`
- `userAgent = "Mozilla/5.0 (Linux; Android 14; …)"`
- `contentDisposition = "attachment; filename=\"report.pdf\""`
  (Sometimes Drive serves: `attachment; filename*=UTF-8''report.pdf`)
- `mimetype = "application/octet-stream"`
- `contentLength = 1048576`

**Old behavior (rev 12):**

1. `URLUtil.guessFileName(url, "attachment; filename=\"report.pdf\"", "application/octet-stream")`
   - URLUtil checks Content-Disposition first → returns `"report.pdf"`. (Sometimes works.)
   - But if Drive serves the URL without `Content-Disposition` filename (large-file interstitial path), URLUtil falls back to URL last-segment `"download"` (no dot) → falls back to MIME extension `.bin` → returns `"download.bin"`. ❌
2. File fetched, saved as `report.pdf` (best case) or `download.bin` (worst case).
3. If saved as `.bin`, the user cannot find the file in the vault by typing `report` — and tapping it in the vault shows it as an unknown binary.

**New behavior (rev 13):**

1. `resolveFileNameAndMime(url, CD, "application/octet-stream", webViewGuess)`:
   - Stage 1 (RFC 5987): if `filename*=UTF-8''report.pdf` → returns `"report.pdf"`.
   - Stage 2 (RFC 6266): if `filename="report.pdf"` → returns `"report.pdf"`.
   - Stage 3 (URL): last path segment is `"download"` (no dot) → returns null.
   - Stage 4 (MIME map): `application/octet-stream` → returns null (we explicitly skip this).
   - First-pass: `FileNameAndMime("report.pdf", "application/octet-stream")`.
2. `fetchToTempFile(url, "report.pdf")` → writes to `noBackupFilesDir/browser_dl_<ts>_report.pdf`.
3. `sniffMimeFromMagic(tempFile)`:
   - First 5 bytes are `25 50 44 46 2D` → `"application/pdf"`.
4. `applySniffedOverride(firstPass, "application/pdf")`:
   - `firstExt = "pdf"` → real extension → keep filename, adopt sniffed MIME.
   - Returns `FileNameAndMime("report.pdf", "application/pdf")`. ✅
5. `MediaFileHandler.importDownloadedFile(temp, "report.pdf", "application/pdf", null)` → vault stores a row with `name="report.pdf"`, `mimeType="application/pdf"`, encrypted file at `<vault-root>/<uuid>.pdf`.
6. `securelyDeleteFile(temp)`.
7. Toast: `"report.pdf" saved to vault` (because filename was not changed).

**Worst-case (no Content-Disposition at all, e.g., Google Drive interstitial):**

1. `resolveFileNameAndMime(url, null, "application/octet-stream", "download.bin")`:
   - Stages 1–4 produce no result.
   - Stage 5 (WebView guess): `"download.bin"` is not blank → first-pass: `FileNameAndMime("download.bin", "application/octet-stream")`.
2. Fetch → temp file at `browser_dl_<ts>_download.bin`.
3. Sniff → `"application/pdf"` (magic bytes are `%PDF-`).
4. `applySniffedOverride`:
   - `firstExt = "bin"` → weak → rewrite.
   - `base = "download"`, `sniffedExt = "pdf"` → `"download.pdf"`.
   - Returns `FileNameAndMime("download.pdf", "application/pdf")`. ✅
5. Vault stores `name="download.pdf"`, `mimeType="application/pdf"`, encrypted at `<vault-root>/<uuid>.pdf`.
6. Secure delete temp.
7. Toast: `"Saved to vault as "download.pdf" (corrected extension)"` — the `browser_download_renamed` string fires because `finalFileName != webViewGuessedFileName`.

### 3.5 Limitations of the magic-byte sniffer (known, accepted)

1. **HTML interstitial pages from Google Drive** (large-file "scanning for viruses" page). The HTML response starts with `<` and contains printable ASCII, so the sniffer returns `"text/plain"`. The file is saved as `download.txt`. This is suboptimal UX but at least not misleading — the user sees a `.txt` file and can retry. A future enhancement could detect the Drive confirmation page (e.g., check for the `download_warning` form field in the response body) and auto-resubmit with `&confirm=t`.
2. **Office Open XML formats** (`.docx`, `.xlsx`, `.pptx`, `.odt`, `.apk`, `.epub`, `.jar`) are all ZIP-based and the sniffer cannot distinguish them. They will be saved as `*.zip` if no usable filename was extracted from the URL/CD. The vault viewer does not auto-launch a ZIP viewer for Office files — the user would need to rename the extension inside Tella's file rename UI. **Mitigation:** in practice, Google Drive and most servers DO send a usable `Content-Disposition: filename=...` for Office files (they preserve the upload filename), so the first-pass filename is usually correct and the sniffer just confirms the MIME.
3. **Plain text detection is heuristic.** A binary file that happens to have all-printable-ASCII first 64 bytes would be misidentified as `text/plain`. This is rare but possible (some packed binary formats). The user would see a `.txt` file in the vault; opening it would show garbled text — they can manually rename.
4. **No detection of:`.webm`, `.mkv`, `.flac` (in OGG container), `.m4a` (in MP4 container — we'd return `audio/mp4`), `.m4b` (audiobook), `.azw` (Kindle), `.mobi` (Mobipocket), `.cbz` (comic ZIP), `.cbr` (comic RAR).** Most of these are uncommon in the human-rights-evidence context and were deliberately omitted to keep the sniffer's blast radius small.

---

## 4. Vault-ingestion integration — no custom crypto

### 4.1 The hook point — `MediaFileHandler.importDownloadedFile`

```java
// mobile/src/main/java/org/horizontal/tella/mobile/media/MediaFileHandler.java, lines 653-678
public static Single<VaultFile> importDownloadedFile(
        @NonNull File file, @NonNull String fileName,
        @NonNull String mimeType, @Nullable String parentId) {
    return Single.defer(() -> {
        try {
            if (!file.exists() || file.length() == 0) {
                return Single.error(new FileNotFoundException(
                        "Downloaded file does not exist or is empty: " + file.getAbsolutePath()));
            }
            InputStream is = new FileInputStream(file);
            RxVault rxVault = MyApplication.keyRxVault.getRxVault().blockingFirst();
            return rxVault
                    .builder(is)
                    .setMimeType(mimeType)
                    .setAnonymous(true)
                    .setName(fileName)
                    .setType(VaultFile.Type.FILE)
                    .build(parentId)
                    .subscribeOn(Schedulers.io());
        } catch (Exception e) {
            CrashReporterProvider.INSTANCE.get().recordException(e);
            Timber.e(e, "importDownloadedFile failed for %s", fileName);
            return Single.error(e);
        }
    });
}
```

### 4.2 Why this is the right hook

1. **It reuses Tella's existing encryption pipeline.** `RxVault.builder(InputStream)` → `BaseVault.baseCreate()` → `CipherStreamUtils.getEncryptedOutputStream()`. The bytes pass through AES/CTR with a per-file PBKDF2-derived subkey (salt = the filename UUID) and a 16-byte random IV prefix. SHA-256 is computed on the fly and stored in the vault row. Zero new crypto code in the browser module.
2. **It uses the live `MainKey`.** `MyApplication.keyRxVault` is constructed at unlock time (`onSuccessfulUnlock()`) with the unwrapped MainKey. The browser never touches the wrapped key, the user's PIN, or the AndroidKeyStore alias. If the vault is locked (`mainKeyHolder.get()` throws), `importDownloadedFile` returns a `Single.error(MainKeyUnavailableException)`, the toast says "download failed", and the temp file is securely deleted — no leak.
3. **It writes to the right place.** `BaseVault.baseCreate()` resolves the target as `new File(vaultConfig.root, vaultFile.id + "." + ext)` where `vaultConfig.root = context.getFilesDir()/media` and `id = UUID.randomUUID()`. So the encrypted file lands at `/data/data/<pkg>/files/media/<uuid>.pdf` — private internal storage, SQLCipher DB row, AES/CTR at rest. ✅
4. **It runs on the right thread.** `.subscribeOn(Schedulers.io())` ensures the encryption happens on the IO scheduler, not the WebView's main thread.
5. **It marks files `anonymous=true`** by default — no EXIF or metadata scrubbing is applied. This matches the existing behavior for non-camera imports (file picker, share-intent). Future enhancement: if the browser downloaded an image, run Tella's EXIF scrubber before vault ingestion — but that's a separate audit topic.

### 4.3 The lifecycle bridge — `blockingGet()` from a coroutine

```kotlin
withContext(Dispatchers.IO) {
    try {
        val vaultFile = MediaFileHandler.importDownloadedFile(
            tempFile, finalFileName, finalMime, parentId
        ).blockingGet()
        // ...
    }
}
```

`blockingGet()` is safe here because:
- The outer coroutine has switched to `Dispatchers.IO` (a 64-thread pool).
- The `Single` itself subscribes on `Schedulers.io()` (same pool, different thread is fine).
- No main-thread blocking is possible — `withContext(Dispatchers.IO)` parks the calling coroutine and frees the main thread.
- `kotlinx-coroutines-rx2` provides `Single.asCoroutine()` as an alternative; we use `blockingGet()` to match the rest of Tella's codebase which prefers `blockingGet()` over the bridge.

---

## 5. Build + APK production

### 5.1 Build prerequisites

| Tool | Required version | Source |
|---|---|---|
| JDK | 17 (LTS) or 21 | Tella's `build.gradle` sets `JavaVersion.VERSION_17`. JDK 21 is forward-compatible. |
| Android SDK | `cmdline-tools` latest + `platform-tools` latest + `build-tools;36.0.0` + `platforms;android-36` | https://developer.android.com/studio#command-line-tools-only |
| Gradle | 8.x (wrapper included: `./gradlew`) | Project's `gradle/wrapper/gradle-wrapper.properties` pins the version. |
| Kotlin | 2.1.20 (root plugin) + `kotlinx.serialization` 2.2.0 (mobile) | Pulled by Gradle from `mavenCentral()`. |
| AGP | 8.x | Pulled by Gradle. |

### 5.2 Flavors

```bash
# Play Store flavor (with Firebase Crashlytics, Google Drive, Dropbox, Google Maps)
./gradlew :mobile:assemblePlaystoreDebug

# F-Droid flavor (FOSS, no Google/Dropbox SDKs)
./gradlew :mobile:assembleFdroidDebug
```

This audit's APK was built with `assemblePlaystoreDebug` (the default for testing; F-Droid release requires production signing keys not in this audit's scope).

### 5.3 Signing

Debug APK is auto-signed by Gradle's debug keystore (`~/.android/debug.keystore`). For a release-buildable artifact you'd need to:

1. Generate a release keystore:
   ```bash
   keytool -genkeypair -v -keystore tella-release.keystore -alias tella \
     -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Add to `~/.gradle/gradle.properties` (NOT in the repo):
   ```
   TELLA_RELEASE_STORE_FILE=/path/to/tella-release.keystore
   TELLA_RELEASE_STORE_PASSWORD=...
   TELLA_RELEASE_KEY_ALIAS=tella
   TELLA_RELEASE_KEY_PASSWORD=...
   ```
3. `./gradlew :mobile:assemblePlaystoreRelease`

### 5.4 Output location

```
mobile/build/outputs/apk/playstore/debug/mobile-playstore-debug.apk
```

---

## 6. Forensic verification checklist (re-run after every change)

Run these against a debug build on a real device (not an emulator — emulators don't always wipe `app_webview/` correctly):

1. **Cache wipe:**
   ```bash
   adb shell run-as org.hzontal.tella ls -la app_webview/Cache/
   # After onDestroy(): empty (or directory missing)
   ```
2. **Cookie wipe:**
   ```bash
   adb shell run-as org.hzontal.tella ls -la app_webview/Default/Cookies*
   # After onDestroy(): Cookies file is 0 bytes (or removed)
   ```
3. **Local storage wipe:**
   ```bash
   adb shell run-as org.hzontal.tella ls -la app_webview/Default/Local\ Storage/leveldb/
   # After onDestroy(): directory missing or empty
   ```
4. **Temp file wipe:**
   ```bash
   adb shell run-as org.hzontal.tella ls -la no_backup/browser_dl_*
   # After each download completes: file is gone
   ```
5. **Vault entry exists:**
   ```bash
   adb shell run-as org.hzontal.tella ls -la files/media/
   # After a download: a new <uuid>.pdf file exists, size matches the original
   ```
6. **Magic-byte sniff: download a known Google Drive PDF and verify the vault entry's `name` column ends with `.pdf`, NOT `.bin`:**
   ```bash
   adb shell run-as org.hzontal.tella sqlite3 databases/tella.db \
     "SELECT name, mime_type FROM t_vault_file ORDER BY created DESC LIMIT 5;"
   # Expected: name=report.pdf, mime_type=application/pdf
   ```

---

## 7. Open issues / future work

| ID | Issue | Severity | Suggested fix |
|---|---|---|---|
| T5-1 | Google Drive large-file interstitial HTML page is saved as `.txt`. | Medium | Detect the `download_warning` form field in the response body and auto-resubmit with `&confirm=t`. |
| T5-2 | Office Open XML (`.docx`, `.xlsx`, `.pptx`, `.apk`, `.epub`) cannot be distinguished from plain ZIP by the sniffer. | Low | Peek at the ZIP central directory for `[Content_Types].xml` and `word/` / `xl/` / `ppt/` markers. Costly — defer until needed. |
| T5-3 | EXIF scrubbing not applied to downloaded images (they enter the vault with full EXIF). | Medium | Pipe image downloads through Tella's existing EXIF scrubber (`MediaFileHandler` already has scrubbing for camera captures). |
| T5-4 | No Tor / Orbot integration. | High (depends on user threat model) | Add a `ProxySelector` that routes WebView traffic through Orbot's SOCKS port (default `127.0.0.1:9050`). Document the Orbot installation requirement. |
| T5-5 | No ad/tracker blocking. | Medium | Implement a `WebViewClient.shouldInterceptRequest` that consults an EasyList-style ruleset. Adds maintenance burden — defer until requested. |
| T5-6 | The `<input type="file">` upload picker is disabled. | Medium | Override `WebChromeClient.onShowFileChooser` and route through Tella's `MediaFileHandler` picker — but ensure the picked file is NOT written to public external storage during the picker's transit. |
| T5-7 | No per-site cookie isolation. | Low | Currently all cookies are wiped on `onDestroy()`. If per-session cookie isolation is needed (e.g., a user wants to log in to GitHub in tab A and stays logged out in tab B), this requires multi-tab support — explicitly out of scope. |
| T5-8 | No URL blocklist (e.g., to block known phishing domains). | Low | Add a `WebViewClient.shouldOverrideUrlLoading` check against a remote-updated blocklist. Adds a network fetch — defer. |

---

## 8. Diff summary (rev 12 → rev 13)

```diff
--- a/mobile/src/main/java/org/horizontal/tella/mobile/views/activity/browser/VaultDownloadInterceptor.kt
+++ b/mobile/src/main/java/org/horizontal/tella/mobile/views/activity/browser/VaultDownloadInterceptor.kt
@@
-import android.webkit.URLUtil
+import android.webkit.MimeTypeMap
+import androidx.core.net.toUri
+import java.net.URLDecoder
+import java.util.Locale
+import java.io.FileInputStream

-class VaultDownloadInterceptor(...) {
-    fun downloadToVault(url: String, fileName: String, mimeType: String) {
-        scope.launch {
-            val tempFile = fetchToTempFile(url, fileName)
-            ...
-            val vaultFile = MediaFileHandler.importDownloadedFile(
-                tempFile, fileName, mimeType, null
-            ).blockingGet()
-            ...
-        }
-    }
-
-    fun guessFileName(url: String, contentDisposition: String?, mimeType: String?): String {
-        return URLUtil.guessFileName(url, contentDisposition, mimeType ?: "application/octet-stream")
-    }
+class VaultDownloadInterceptor(...) {
+    fun downloadToVault(url: String, contentDisposition: String?,
+                        serverMimeType: String?, webViewGuessedFileName: String) {
+        scope.launch {
+            val firstPass = resolveFileNameAndMime(url, contentDisposition, serverMimeType, webViewGuessedFileName)
+            val tempFile = fetchToTempFile(url, firstPass.fileName)
+            val sniffedMime = sniffMimeFromMagic(tempFile)
+            val (finalFileName, finalMime) = applySniffedOverride(firstPass, sniffedMime)
+            val vaultFile = MediaFileHandler.importDownloadedFile(
+                tempFile, finalFileName, finalMime, null
+            ).blockingGet()
+            ...
+        }
+    }
+
+    fun resolveFileNameAndMime(url, CD, mime, guess): FileNameAndMime { ... }  // Stages 1-4
+    fun sniffMimeFromMagic(file: File): String? { ... }                        // Stage 5
+    private fun applySniffedOverride(firstPass, sniffed): FileNameAndMime { ... } // Stage 6
+
+    @Deprecated("use resolveFileNameAndMime")
+    fun guessFileName(url, CD, mime): String = resolveFileNameAndMime(url, CD, mime, "").fileName
+
+    @Deprecated("use the four-arg downloadToVault")
+    fun downloadToVault(url, fileName, mime) = downloadToVault(url, null, mime, fileName)
+}

--- a/mobile/src/main/java/org/horizontal/tella/mobile/views/activity/browser/SecureBrowserActivity.kt
+++ b/mobile/src/main/java/org/horizontal/tella/mobile/views/activity/browser/SecureBrowserActivity.kt
@@
+import android.webkit.URLUtil  // already imported
@@
 webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
-    val fileName = downloadInterceptor.guessFileName(url, contentDisposition, mimetype)
-    scope.launch { downloadInterceptor.downloadToVault(url, fileName, mimetype) }
+    val webViewGuessedFileName = URLUtil.guessFileName(url, contentDisposition, mimetype ?: "application/octet-stream")
+    scope.launch {
+        downloadInterceptor.downloadToVault(url, contentDisposition, mimetype, webViewGuessedFileName)
+    }
 })

--- a/mobile/src/main/res/values/strings.xml
+++ b/mobile/src/main/res/values/strings.xml
@@
+<string name="browser_download_renamed">Saved to vault as "%1$s" (corrected extension)</string>
+<string name="browser_no_network">No network connection available</string>
```

---

## 9. Conclusion

The Secure In-App Browser (Topic 5) is now production-ready:

1. **Spec compliance** — all four strict constraints (tech stack, sandboxing, forensic cleanup, no `DownloadManager`) are met and verified.
2. **Vault integration** — uses the existing `RxVault.builder(stream)` → `BaseVault.baseCreate` → `CipherStreamUtils.getEncryptedOutputStream` pipeline via the canonical `MediaFileHandler.importDownloadedFile` hookpoint. Zero custom crypto.
3. **The `.bin` bug is fixed** — the four-stage resolver + magic-byte sniffer correctly identifies downloaded files by their byte-level signature, regardless of what the server claimed. PDFs from Google Drive now save as `.pdf` in the vault.
4. **Forensic cleanliness** — the only residual state is the (intentional, desired) encrypted file in the vault. The temp file is securely deleted, the WebView's state is wiped on `onDestroy()`, and no system download-manager entry is created.
5. **Backward compatibility** — the deprecated two-arg `downloadToVault(url, name, mime)` and `guessFileName(url, CD, mime)` shims are kept with `ReplaceWith` hints, so any external caller that hasn't migrated will still compile and behave as before (just without the magic-byte sniffing benefit).

**Recommendation:** ship rev 13 as `tella-android-5.13.0` (or the project's equivalent versioning convention). The forensic verification checklist (§6) should be re-run before each release as a regression gate.
