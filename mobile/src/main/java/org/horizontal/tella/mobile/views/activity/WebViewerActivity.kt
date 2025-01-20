package org.horizontal.tella.mobile.views.activity

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.WebView
import com.hzontal.tella_vault.VaultFile
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.activity.viewer.PhotoViewerActivity
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity

class WebViewerActivity : BaseLockActivity(){

    private lateinit var webView: WebView
    private var vaultFile: VaultFile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_viewer)
        clearCookies()
        initView()
    }

    private fun initView() {
        webView = findViewById(R.id.webView)
        initData()
    }

    private fun initData(){
        if (intent.hasExtra(PhotoViewerActivity.VIEW_PHOTO)) {
            val vaultFile = intent.extras!![PhotoViewerActivity.VIEW_PHOTO] as VaultFile?
            if (vaultFile != null) {
                this.vaultFile = vaultFile
            }
        }
        showData()
    }

    private fun clearCookies() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } else {
            val cookieSyncMonger = CookieSyncManager.createInstance(this)
            cookieSyncMonger.startSync()
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookie()
            cookieManager.removeSessionCookie()
            cookieSyncMonger.stopSync()
            cookieSyncMonger.sync()
        }
    }
    private fun showData(){
        webView.loadData(vaultFile?.thumb.toString(),vaultFile?.mimeType,"utf-8")
    }
}