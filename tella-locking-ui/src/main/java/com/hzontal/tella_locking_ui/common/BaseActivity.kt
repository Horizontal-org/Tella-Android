package com.hzontal.tella_locking_ui.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.TellaKeysUI
import org.hzontal.tella.keys.config.UnlockConfig
import org.hzontal.tella.keys.config.UnlockRegistry
import timber.log.Timber

open class BaseActivity : AppCompatActivity() {

    protected val config: UnlockConfig by lazy { TellaKeysUI.getUnlockRegistry().getActiveConfig(this) }
    protected val registry: UnlockRegistry by lazy { TellaKeysUI.getUnlockRegistry() }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("** %s: %s **", javaClass, "onCreate()")
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.`in`, R.anim.out)
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
}