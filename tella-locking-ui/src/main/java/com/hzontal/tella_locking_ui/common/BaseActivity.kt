package com.hzontal.tella_locking_ui.common

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import com.hzontal.tella_locking_ui.ONBOARDING_CLASS_NAME
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.ui.ConfirmCredentialsActivity
import com.hzontal.tella_locking_ui.ui.SuccessUpdateDialog
import org.hzontal.tella.keys.config.UnlockConfig
import org.hzontal.tella.keys.config.UnlockRegistry
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

open class BaseActivity : AppCompatActivity() {
    protected val isFromSettings by lazy { intent.getBooleanExtra(IS_FROM_SETTINGS,false)  }
    protected var isConfirmSettingsUpdate : Boolean = false
    protected val config: UnlockConfig by lazy { TellaKeysUI.getUnlockRegistry().getActiveConfig(this) }
    protected val registry: UnlockRegistry by lazy { TellaKeysUI.getUnlockRegistry() }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("** %s: %s **", javaClass, "onCreate()")
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.`in`, R.anim.out)
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

    protected fun onSuccessfulUnlock(){
        if (isFromSettings){
            val intent = Intent(this, Class.forName(ONBOARDING_CLASS_NAME))
            intent.putExtra(IS_FROM_SETTINGS,true)
            startActivity(intent)
        }
        else{
            TellaKeysUI.getCredentialsCallback().onSuccessfulUnlock(this)
        }
    }

    protected fun onSuccessConfirmUnlock(){
       if (isConfirmSettingsUpdate){
           val dialog =  SuccessUpdateDialog()
           dialog.show(supportFragmentManager,"SUCCESS_DIALOG")
           val executor = Executors.newSingleThreadScheduledExecutor()
           val hideDialog =  Runnable {
               TellaKeysUI.getCredentialsCallback().onSuccessfulUnlock(this)
               dialog.dismiss()
           }
           executor.schedule(hideDialog, 3, TimeUnit.SECONDS);
       }else{
           startActivity(Intent(this, ConfirmCredentialsActivity::class.java))
           finishAffinity()
       }
    }
}