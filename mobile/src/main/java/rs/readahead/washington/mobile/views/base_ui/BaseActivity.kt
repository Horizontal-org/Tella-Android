package rs.readahead.washington.mobile.views.base_ui

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.util.LocaleManager

  abstract class BaseActivity : AppCompatActivity() {
    var isManualOrientation = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // start with preventing showing screen in tasks?
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
        //        WindowManager.LayoutParams.FLAG_SECURE);
        if (!isManualOrientation && !resources.getBoolean(R.bool.isTablet)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
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