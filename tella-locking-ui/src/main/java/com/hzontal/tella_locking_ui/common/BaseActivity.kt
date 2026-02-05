package com.hzontal.tella_locking_ui.common

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.hzontal.tella_locking_ui.IS_CAMOUFLAGE
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import com.hzontal.tella_locking_ui.IS_ONBOARD_LOCK_SET
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.RETURN_ACTIVITY
import com.hzontal.tella_locking_ui.ReturnActivity
import com.hzontal.tella_locking_ui.TellaKeysUI
import org.hzontal.tella.keys.config.UnlockConfig
import org.hzontal.tella.keys.config.UnlockRegistry
import org.hzontal.tella.keys.key.MainKey
import timber.log.Timber

const val SET_SECURITY_SCREEN = "set_security_screen"
private const val SHARED_PREFS_NAME = "washington_shared_prefs"

open class BaseActivity : AppCompatActivity() {
    protected val isFromSettings by lazy { intent.getBooleanExtra(IS_FROM_SETTINGS, false) }
    protected val returnActivity by lazy { intent.getIntExtra(RETURN_ACTIVITY, 0) }
    private var isConfirmSettingsUpdate: Boolean = false
    protected val config: UnlockConfig by lazy {
        TellaKeysUI.getUnlockRegistry().getActiveConfig(this)
    }
    protected val registry: UnlockRegistry by lazy { TellaKeysUI.getUnlockRegistry() }


    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("** %s: %s **", javaClass, "onCreate()")
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE
        )
        overridePendingTransition(R.anim.`in`, R.anim.out)
    }

    fun applyEdgeToEdge(view: View) {
        // Window already configured by enableEdgeToEdge() in onCreate. Apply insets so content is not drawn under system bars.
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    override fun onResume() {
        Timber.d("** %s: %s **", javaClass, "onResume()")
        maybeEnableSecurityScreen()
        super.onResume()
    }

    override fun onPause() {
        Timber.d("** %s: %s **", javaClass, "onPause()")
        super.onPause()
    }

    override fun onStop() {
        Timber.d("** %s: %s **", javaClass, "onStop()")
        super.onStop()
    }

    override fun onDestroy() {
        Timber.d("** %s: %s **", javaClass, "onDestroy()")
        super.onDestroy()
    }

    protected fun onSuccessfulUnlock() {
        when (returnActivity) {
            ReturnActivity.SETTINGS.getActivityOrder() -> {
                val intent = Intent(this, Class.forName(ReturnActivity.SETTINGS.activityName))
                intent.putExtra(IS_FROM_SETTINGS, true)
                startActivity(intent)
            }

            ReturnActivity.CAMOUFLAGE.getActivityOrder() -> {
                val intent = Intent(this, Class.forName(ReturnActivity.CAMOUFLAGE.activityName))
                intent.putExtra(IS_CAMOUFLAGE, true)
                startActivity(intent)
            }

            else -> {
                TellaKeysUI.getCredentialsCallback().onSuccessfulUnlock(this)
            }
        }
    }

    protected fun onSuccessConfirmUnlock() {
        if (isConfirmSettingsUpdate) {
            TellaKeysUI.getCredentialsCallback().onUpdateUnlocking()
            TellaKeysUI.getCredentialsCallback().onLockUpdateSuccess(this)
            setResult(RESULT_OK)
            finish()
        } else {
            val intent = Intent(this, Class.forName(ReturnActivity.SETTINGS.activityName))
            intent.putExtra(IS_ONBOARD_LOCK_SET, true)
            startActivity(intent)
            finishAffinity()
        }
    }

    protected fun generateOrGetMainKey(): MainKey {
        return if (TellaKeysUI.getMainKeyStore().isStored) {
            isConfirmSettingsUpdate = true
            TellaKeysUI.getMainKeyHolder().get()
        } else {
            MainKey.generate()
        }
    }

    private fun maybeEnableSecurityScreen() {
        if (getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).getBoolean(
                SET_SECURITY_SCREEN, false
            )
        ) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }
}