package com.hzontal.tella_locking_ui.ui.settings

import android.content.Intent
import android.os.Bundle
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.common.BaseActivity
import com.hzontal.tella_locking_ui.ui.DeviceCredentialsUnlockActivity
import com.hzontal.tella_locking_ui.ui.PasswordUnlockActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternSetActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternUnlockActivity
import org.hzontal.tella.keys.config.UnlockRegistry

class UnlockRedirectActivity  : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        redirectToUnlockScreen()
    }

    private fun redirectToUnlockScreen(){
        if (TellaKeysUI.getUnlockRegistry().getActiveMethod(this).toString().isEmpty()){
            startActivity(Intent(this, LockTypeActivity::class.java))
            return
        }
        val isMainKeyExist = TellaKeysUI.getMainKeyStore().isStored
        when(TellaKeysUI.getUnlockRegistry().getActiveMethod(this)){
            UnlockRegistry.Method.TELLA_PATTERN -> {startActivity(Intent(this,if (!isMainKeyExist) PatternSetActivity::class.java else PatternUnlockActivity::class.java) )}
            UnlockRegistry.Method.TELLA_PASSWORD ->  {startActivity(Intent(this, PasswordUnlockActivity::class.java))}
            UnlockRegistry.Method.DEVICE_CREDENTIALS-> {startActivity(Intent(this, DeviceCredentialsUnlockActivity::class.java))}
            UnlockRegistry.Method.DEVICE_CREDENTIALS_BIOMETRICS -> {}
            UnlockRegistry.Method.TELLA_PIN ->{}
            else ->{}
        }
        finish()
    }
}