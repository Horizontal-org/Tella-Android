package com.hzontal.tella_locking_ui.common

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import com.hzontal.tella_locking_ui.ONBOARDING_CLASS_NAME
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.ui.ConfirmCredentialsActivity
import com.hzontal.tella_locking_ui.ui.SuccessUpdateDialog
import org.hzontal.tella.keys.config.UnlockConfig
import org.hzontal.tella.keys.config.UnlockRegistry
import org.hzontal.tella.keys.key.MainKey
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

open class BaseActivity : AppCompatActivity() {
    protected val isFromSettings by lazy { intent.getBooleanExtra(IS_FROM_SETTINGS, false) }
    private var isConfirmSettingsUpdate: Boolean = false
    protected val config: UnlockConfig by lazy { TellaKeysUI.getUnlockRegistry().getActiveConfig(this) }
    protected val registry: UnlockRegistry by lazy { TellaKeysUI.getUnlockRegistry() }
    var startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) { finish()}
    }
    var finishActivity = MutableLiveData<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("** %s: %s **", javaClass, "onCreate()")
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.`in`, R.anim.out)
        CommonStates.finishUpdateActivity.observe(this, Observer {isFinished->
            if (isFinished) {
                TellaKeysUI.getCredentialsCallback().onUpdateUnlocking()
                finish()
            }
        })
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onResume() {
        Timber.d("** %s: %s **", javaClass, "onResume()")
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
        if (isFromSettings) {
            val intent = Intent(this, Class.forName(ONBOARDING_CLASS_NAME))
            intent.putExtra(IS_FROM_SETTINGS, true)
            startActivity(intent)
        } else {
            TellaKeysUI.getCredentialsCallback().onSuccessfulUnlock(this)
        }
    }

    protected fun onSuccessConfirmUnlock() {
        if (isConfirmSettingsUpdate) {
            val dialog = SuccessUpdateDialog()
            dialog.show(supportFragmentManager, "SUCCESS_DIALOG")
            val executor = Executors.newSingleThreadScheduledExecutor()
            val hideDialog = Runnable {
                CommonStates.finishUpdateActivity.postValue(true)
                dialog.dismiss()
            }
            executor.schedule(hideDialog, 3, TimeUnit.SECONDS);
        } else {
            startActivity(Intent(this, ConfirmCredentialsActivity::class.java))
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
}