package rs.readahead.washington.mobile.views.activity

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.WebView
import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.mvp.contract.IMediaFileViewerPresenterContract
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity
import rs.readahead.washington.mobile.views.fragment.ShareDialogFragment

class WebViewerActivity : BaseLockActivity(),
    IMediaFileViewerPresenterContract.IView,
    ShareDialogFragment.IShareDialogFragmentHandler {

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

    override fun onMediaExported() {
    }

    override fun onExportError(error: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun onExportStarted() {
        TODO("Not yet implemented")
    }

    override fun onExportEnded() {
        TODO("Not yet implemented")
    }

    override fun onMediaFileDeleted() {
        TODO("Not yet implemented")
    }

    override fun onMediaFileDeletionError(throwable: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun onMediaFileRename(vaultFile: VaultFile?) {
        TODO("Not yet implemented")
    }

    override fun onMediaFileRenameError(throwable: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun getContext(): Context {
        TODO("Not yet implemented")
    }

    override fun sharingMediaMetadataSelected() {
        TODO("Not yet implemented")
    }

    override fun sharingMediaOnlySelected() {
        TODO("Not yet implemented")
    }
}