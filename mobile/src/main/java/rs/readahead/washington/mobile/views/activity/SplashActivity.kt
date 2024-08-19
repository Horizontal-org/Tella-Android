package rs.readahead.washington.mobile.views.activity

import android.os.Bundle
import android.os.Handler
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.util.CamouflageManager
import rs.readahead.washington.mobile.views.base_ui.BaseActivity

private const val SPLASH_TIMEOUT_MS = 1000L

class SplashActivity : BaseActivity() {

    private val cm by lazy { CamouflageManager.getInstance() }
    private val handler by lazy { Handler() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (cm.isDefaultLauncherActivityAlias) {
            setContentView(R.layout.splash_layout)
        }
        initView()
    }

    private fun initView() {
        if (cm.isDefaultLauncherActivityAlias) {
            handler.postDelayed({
                goToMainActivity()
            }, SPLASH_TIMEOUT_MS)
        } else {
            goToMainActivity()
        }
    }

    private fun goToMainActivity() {
        MyApplication.startMainActivity(this@SplashActivity)
        finish()
    }
}