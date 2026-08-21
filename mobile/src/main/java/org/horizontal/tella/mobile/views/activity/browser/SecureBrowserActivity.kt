package org.horizontal.tella.mobile.views.activity.browser

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity
import timber.log.Timber

/**
 * Secure In-App Browser for Tella.
 *
 * 2026-08-20 (audit rev 12): A minimalist, heavily-sandboxed WebView that
 * intercepts downloads and routes them directly into Tella's encrypted
 * vault — preventing external browser traces (cache, history, cookies)
 * from leaking outside Tella's secure environment.
 *
 * ## Security constraints (per spec)
 *
 * 1. **Tech Stack:** Kotlin + native Android `WebView` only. NO Chrome
 *    Custom Tabs, NO GeckoView, NO new third-party dependencies.
 * 2. **Security:** `allowFileAccess = false`, `allowContentAccess = false`.
 *    Strictly cancel ALL SSL errors (`handler.cancel()`), never
 *    `handler.proceed()`.
 * 3. **Forensic Cleanliness:** On destroy, aggressively clear history,
 *    cache, cookies, and DOM storage. Leave zero traces.
 * 4. **Download Flow:** Intercept via `setDownloadListener`. Do NOT use
 *    Android's `DownloadManager`. Do NOT write to public external storage.
 *
 * ## Download → Vault flow
 *
 * 1. `setDownloadListener` extracts the filename + URL + MIME type.
 * 2. [VaultDownloadInterceptor] fetches the file via `HttpURLConnection`
 *    into a temporary file in `context.noBackupFilesDir` (NOT public
 *    external storage).
 * 3. The temp file is handed to Tella's existing vault encryption
 *    pipeline via [MediaFileHandler.importDownloadedFile] →
 *    `RxVault.builder(stream).setMimeType(...).setName(...).build(parentId)`
 *    → `BaseVault.baseCreate` → `CipherStreamUtils.getEncryptedOutputStream`.
 * 4. The temporary unencrypted file is immediately and securely deleted
 *    after successful vault ingestion (overwrite with zeros + delete).
 * 5. A toast confirms success/failure to the user.
 *
 * ## Orientation
 *
 * Sets `isManualOrientation = true` so `BaseActivity` does not force
 * portrait — the browser follows the device orientation (same as the
 * PDF reader, see rev 9 fix).
 */
@AndroidEntryPoint
class SecureBrowserActivity : BaseLockActivity() {

    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var goButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var forwardButton: ImageButton
    private lateinit var homeButton: ImageButton

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var downloadInterceptor: VaultDownloadInterceptor

    override fun onCreate(savedInstanceState: Bundle?) {
        isManualOrientation = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secure_browser)

        val toolbar = findViewById<Toolbar>(R.id.browserToolbar)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        toolbar.title = getString(R.string.browser_title)

        urlBar = findViewById(R.id.urlBar)
        progressBar = findViewById(R.id.browserProgressBar)
        goButton = findViewById(R.id.goButton)
        backButton = findViewById(R.id.backButton)
        forwardButton = findViewById(R.id.forwardButton)
        homeButton = findViewById(R.id.homeButton)

        webView = findViewById(R.id.webView)
        downloadInterceptor = VaultDownloadInterceptor(this, scope)

        setupWebView()
        setupControls()

        val initialUrl = intent.getStringExtra(EXTRA_URL) ?: DEFAULT_URL
        webView.loadUrl(initialUrl)
        urlBar.setText(initialUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mediaPlaybackRequiresUserGesture = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            saveFormData = false
            savePassword = false
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)

        webView.webViewClient = SecureWebViewClient()
        webView.webChromeClient = SecureWebChromeClient()

        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            Timber.d("Browser download intercepted: url=%s, mimetype=%s", url, mimetype)
            val fileName = downloadInterceptor.guessFileName(url, contentDisposition, mimetype)
            scope.launch {
                downloadInterceptor.downloadToVault(url, fileName, mimetype)
            }
        })
    }

    private fun setupControls() {
        goButton.setOnClickListener {
            var url = urlBar.text.toString().trim()
            if (url.isEmpty()) return@setOnClickListener
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            webView.loadUrl(url)
        }
        urlBar.setOnEditorActionListener { v, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT ||
                (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                goButton.performClick()
                true
            } else false
        }
        backButton.setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }
        forwardButton.setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
        }
        homeButton.setOnClickListener {
            webView.loadUrl(DEFAULT_URL)
        }
    }

    private inner class SecureWebViewClient : WebViewClient() {
        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            // 2026-08-20 (audit rev 12): CRITICAL — never call handler.proceed().
            // Cancel the request so the user is not exposed to a MITM attack.
            Timber.w("SSL error cancelled: %s", error?.toString())
            handler?.cancel()
            runOnUiThread {
                Toast.makeText(this@SecureBrowserActivity, R.string.browser_ssl_error, Toast.LENGTH_SHORT).show()
            }
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            return false
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            urlBar.setText(url)
            backButton.isEnabled = webView.canGoBack()
            forwardButton.isEnabled = webView.canGoForward()
        }
    }

    private inner class SecureWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            progressBar.progress = newProgress
            progressBar.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroy() {
        forensicCleanup()
        super.onDestroy()
        scope.cancel()
    }

    private fun forensicCleanup() {
        try {
            webView.stopLoading()
            webView.clearHistory()
            webView.clearCache(true)
            webView.clearFormData()
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            webView.removeAllViews()
            webView.destroy()
            Timber.d("Browser forensic cleanup complete")
        } catch (e: Exception) {
            Timber.e(e, "Browser forensic cleanup failed")
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        const val EXTRA_URL = "browser_url"
        private const val DEFAULT_URL = "https://duckduckgo.com"
    }
}
