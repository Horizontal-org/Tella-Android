package rs.readahead.washington.mobile.views.activity

import android.os.Bundle
import android.os.Handler
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.util.CamouflageManager
import rs.readahead.washington.mobile.util.CleanInsightUtils
import rs.readahead.washington.mobile.views.base_ui.BaseActivity

private const val SPLASH_TIMEOUT_MS = 1000L
@AndroidEntryPoint
class SplashActivity : BaseActivity() {

    private val cm = CamouflageManager.getInstance()

    private lateinit var handler : Handler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (cm.isDefaultLauncherActivityAlias) setContentView(R.layout.splash_layout)
        handler = Handler()
        initView()
        CleanInsightUtils.measureVisit()
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

    private fun goToMainActivity(){
        MyApplication.startMainActivity(this@SplashActivity)
        finish()
    }
}