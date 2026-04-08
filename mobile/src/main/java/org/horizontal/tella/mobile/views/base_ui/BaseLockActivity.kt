package org.horizontal.tella.mobile.views.base_ui

import android.content.Intent
import android.view.WindowManager
import com.hzontal.tella_locking_ui.CALCULATOR_ALIAS
import com.hzontal.tella_locking_ui.CALCULATOR_ALIAS_BLUE_SKIN
import com.hzontal.tella_locking_ui.CALCULATOR_ALIAS_ORANGE_SKIN
import com.hzontal.tella_locking_ui.CALCULATOR_ALIAS_YELLOW_SKIN
import com.hzontal.tella_locking_ui.ui.password.PasswordUnlockActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternSetActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternUnlockActivity
import com.hzontal.tella_locking_ui.ui.pin.PinUnlockActivity
import com.hzontal.tella_locking_ui.ui.pin.calculator.CalculatorActivity
import android.app.Activity
import info.guardianproject.cacheword.SecretsManager
import org.hzontal.shared_ui.utils.CALCULATOR_THEME
import org.hzontal.tella.keys.config.IUnlockRegistryHolder
import org.hzontal.tella.keys.config.UnlockRegistry
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.sharedpref.Preferences
import org.horizontal.tella.mobile.util.LockTimeoutManager
import org.horizontal.tella.mobile.util.LockTimeoutManager.IMMEDIATE_SHUTDOWN
import org.horizontal.tella.mobile.views.activity.PatternUpgradeActivity

abstract class BaseLockActivity : BaseActivity() {

    companion object {

        /**
         * Same unlock entry as [restrictActivity] when the key is stored but not in memory:
         * no Settings / change-lock extras — user must unlock the app normally.
         */
        @JvmStatic
        fun launchFullAppUnlock(activity: Activity) {
            val holder = activity.applicationContext as IUnlockRegistryHolder
            val intent = when (holder.unlockRegistry.getActiveMethod(activity)) {
                UnlockRegistry.Method.TELLA_PIN -> {
                    when (Preferences.getAppAlias()) {
                        CALCULATOR_ALIAS, CALCULATOR_ALIAS_BLUE_SKIN, CALCULATOR_ALIAS_ORANGE_SKIN, CALCULATOR_ALIAS_YELLOW_SKIN
                        -> Intent(activity, CalculatorActivity::class.java).putExtra(
                            CALCULATOR_THEME,
                            Preferences.getCalculatorTheme()
                        )

                        else -> Intent(activity, PinUnlockActivity::class.java)
                    }
                }

                UnlockRegistry.Method.TELLA_PATTERN -> {
                    Intent(activity, PatternUnlockActivity::class.java)
                }

                UnlockRegistry.Method.TELLA_PASSWORD -> {
                    Intent(activity, PasswordUnlockActivity::class.java)
                }

                else -> {
                    Intent(activity, PatternUnlockActivity::class.java)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            activity.startActivity(intent)
            activity.finish()
        }
    }

    private val holder by lazy { applicationContext as IUnlockRegistryHolder }
    var isLocked = false
        private set

    fun restrictActivity() {
        if (!MyApplication.getMainKeyStore().isStored) {
            startKeySetup()
        } else {
            isLocked = !MyApplication.getMainKeyHolder().exists()
            if (isLocked) {
                startUnlockingMainKey()
            }
        }
    }

    private fun startKeySetup() {
        val intent = Intent(
            this,
            if (SecretsManager.isInitialized(this)) PatternUpgradeActivity::class.java else PatternSetActivity::class.java
        )
        this.startActivity(intent)
    }

    private fun startUnlockingMainKey() {
        launchFullAppUnlock(this)
    }

    override fun onResume() {
        Preferences.setUnlockTime(System.currentTimeMillis())
        restrictActivity()
        maybeEnableSecurityScreen()
        super.onResume()
    }

    private fun maybeRestoreTimeout() {
        // Check if a temporary timeout is currently set in preferences
        if (Preferences.isTempTimeout()) {
            // Reset the temporary timeout flag in preferences
            Preferences.setTempTimeout(false)
            // Immediately set the lock timeout to shut down the application
            LockTimeoutManager().lockTimeout = IMMEDIATE_SHUTDOWN
        }
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

    override fun onDestroy() {
        // Attempt to restore the timeout setting if it was temporarily changed
        maybeRestoreTimeout()
        // Note: onDestroy() is not always reliable, as its execution depends on system behavior
        // and can vary across devices.
        super.onDestroy()
    }
}