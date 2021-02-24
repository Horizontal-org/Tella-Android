package rs.readahead.washington.mobile.views.activity

import android.os.Bundle
import android.os.Handler
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseActivity

private const val SPLASH_TIMEOUT_MS = 1000L
class SplashActivity : BaseActivity() {
    private lateinit var handler : Handler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_layout)
        handler = Handler()
        initView()
    }

    private fun initView() {
        handler.postDelayed({
            MyApplication.startMainActivity(this@SplashActivity)
            finish()
        }, SPLASH_TIMEOUT_MS)
    }


}