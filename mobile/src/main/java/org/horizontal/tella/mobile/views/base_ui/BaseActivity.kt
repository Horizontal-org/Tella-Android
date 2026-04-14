package org.horizontal.tella.mobile.views.base_ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.sharedpref.Preferences
import org.horizontal.tella.mobile.util.BottomMessageManager
import org.horizontal.tella.mobile.util.LocaleManager
import org.horizontal.tella.mobile.util.LockTimeoutManager
import org.horizontal.tella.mobile.util.ThemeStyleManager
import org.horizontal.tella.mobile.util.divviup.DivviupUtils
import org.horizontal.tella.mobile.util.setupForAccessibility
import javax.inject.Inject

@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity() {
    var isManualOrientation = false
    private lateinit var container: ViewGroup
    private lateinit var loading: View
    @Inject
    lateinit var divviupUtils: DivviupUtils
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                BottomMessageManager.events.collect { stringRes ->
                    DialogUtils.showBottomMessage(this@BaseActivity, getString(stringRes), false)
                }
            }
        }
        // Use Google-recommended API for edge-to-edge (avoids deprecated setStatusBarColor path)
        WindowCompat.enableEdgeToEdge(window)
        // Set window background to transparent so fragment backgrounds show through
        // This prevents white bars from showing in system bar areas when edge-to-edge is enabled
        window.setBackgroundDrawableResource(android.R.color.transparent)
        setThemeStyle()
        supportFragmentManager.setupForAccessibility(this)
        // start with preventing showing screen in tasks?
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
        //        WindowManager.LayoutParams.FLAG_SECURE);
        if (!isManualOrientation && !resources.getBoolean(R.bool.isTablet)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        maybeRestoreExitTimeOut()
        initLoading()
    }

    fun applyEdgeToEdge(view: View) {
        // Window already configured by enableEdgeToEdge() in onCreate.
        // Apply insets so content is not drawn under system bars.
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
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

    @SuppressLint("ClickableViewAccessibility")
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

    fun maybeChangeTemporaryTimeout(confirm: () -> Unit) {
        if (LockTimeoutManager().lockTimeout == LockTimeoutManager.IMMEDIATE_SHUTDOWN || LockTimeoutManager().lockTimeout == LockTimeoutManager.ONE_MINUTES_SHUTDOWN) {
            showTimeOutChangeDialog(confirm)
        } else {
            confirm.invoke()
        }
    }

    fun maybeChangeTemporaryTimeout() {
        if (LockTimeoutManager().lockTimeout == LockTimeoutManager.IMMEDIATE_SHUTDOWN || LockTimeoutManager().lockTimeout == LockTimeoutManager.ONE_MINUTES_SHUTDOWN) {
            MyApplication.getMainKeyHolder().timeout =
                LockTimeoutManager.THREE_MINUTES_SHUTDOWN
            Preferences.setTempTimeout(true)
        }
    }

    private fun maybeRestoreExitTimeOut() {
        if (Preferences.isExitTimeout()) {
            MyApplication.getMainKeyHolder().timeout = Preferences.getLockTimeout()
            Preferences.setTempTimeout(false)
        }
    }

    fun replaceFragmentNoAddToBackStack(fragment: Fragment, cont: Int) {
        val className = fragment.javaClass.name
        supportFragmentManager
            .beginTransaction()
            .add(cont, fragment, className)
            .commitAllowingStateLoss()
    }

    fun addFragment(fragmentToHide: Fragment, fragment: Fragment, container: Int) {
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

    fun addFragment(container: Int, fragment: Fragment, tag: String) {
        val existingFragment = supportFragmentManager.findFragmentByTag(tag)
        if (existingFragment == null) {
            supportFragmentManager.beginTransaction()
                .add(container, fragment, tag)
                .addToBackStack(null)
                .commit()
        }
    }

    fun addFragmentWithAnimation(fragment: Fragment, container: Int) {
        val className = fragment.javaClass.name
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.`in`,
                R.anim.out,
                R.anim.left_to_right,
                R.anim.right_to_left
            )
            .setCustomAnimations(
                R.anim.`in`,
                R.anim.out,
                R.anim.left_to_right,
                R.anim.right_to_left
            )
            .add(container, fragment, className)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    override fun onResume() {
        super.onResume()
        maybeEnableSecurityScreen()
    }

    private fun maybeEnableSecurityScreen() {
        if (Preferences.isSecurityScreenEnabled()) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    private fun showTimeOutChangeDialog(confirm: () -> Unit) {
        BottomSheetUtils.showStandardSheet(
            fragmentManager = supportFragmentManager,
            getString(R.string.Timeout_Warning_Title),
            getString(R.string.Timeout_Warning_Description),
            getString(R.string.action_continue),
            getString(R.string.action_cancel),
            onConfirmClick = {
                MyApplication.getMainKeyHolder().timeout =
                    LockTimeoutManager.THREE_MINUTES_SHUTDOWN
                Preferences.setTempTimeout(true)
                confirm.invoke()
            },
            onCancelClick = null
        )
    }

    open fun setThemeStyle() {
        this.theme.applyStyle(ThemeStyleManager.getThemeStyle(this), true)
    }
}