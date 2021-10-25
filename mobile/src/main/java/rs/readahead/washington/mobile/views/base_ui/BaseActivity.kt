package rs.readahead.washington.mobile.views.base_ui

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.util.LocaleManager
import rs.readahead.washington.mobile.util.LockTimeoutManager

abstract class BaseActivity : AppCompatActivity() {
    var isManualOrientation = false
    private lateinit var container: ViewGroup
    private lateinit var loading: View
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // start with preventing showing screen in tasks?
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
        //        WindowManager.LayoutParams.FLAG_SECURE);
        if (!isManualOrientation && !resources.getBoolean(R.bool.isTablet)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        initLoading()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleManager.getInstance().getLocalizedContext(base))
    }

    fun showToast(@StringRes resId: Int) {
        showToast(getString(resId))
    }

    fun showToast(string: String?) {
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
    }

    private fun initLoading() {
        container = findViewById<View>(android.R.id.content) as ViewGroup
        loading =
            LayoutInflater.from(this).inflate(R.layout.baseactivity_activity, container, false)
        loading.setOnTouchListener { _, _ -> false }
    }

    fun toggleLoading(show: Boolean) {
        if (!isDestroyed) {
            container.removeView(loading)
            if (show) {
                container.addView(loading)
            }
        }
    }

    fun changeTemporaryTimeout(){
        if (LockTimeoutManager().lockTimeout == LockTimeoutManager.IMMEDIATE_SHUTDOWN) {
            MyApplication.getMainKeyHolder().timeout  = LockTimeoutManager.FIVE_MUNITES_SHUTDOWN
        }
    }

    fun replaceFragmentNoAddToBackStack(fragment: Fragment, cont: Int) {
        val className = fragment.javaClass.name
        supportFragmentManager
            .beginTransaction()
            .add(cont, fragment, className)
            .commitAllowingStateLoss()
    }


    fun addFragment(fragmentToHide: BaseFragment, fragment: BaseFragment, container: Int) {
        val className = fragment.javaClass.name
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.`in`,
                R.anim.out,
                R.anim.left_to_right,
                R.anim.right_to_left
            )
            .hide(fragmentToHide)
            .add(container, fragment, className)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    fun addFragment(fragment: Fragment, container: Int) {
        val className = fragment.javaClass.name
        supportFragmentManager
            .beginTransaction()
            .add(container, fragment, className)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }
}