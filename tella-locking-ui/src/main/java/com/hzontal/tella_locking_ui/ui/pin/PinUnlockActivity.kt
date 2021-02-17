package com.hzontal.tella_locking_ui.ui.pin

import android.os.Bundle
import androidx.core.view.isVisible
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.ui.pin.base.BasePinActivity
import org.hzontal.tella.keys.MainKeyStore
import org.hzontal.tella.keys.key.MainKey
import timber.log.Timber
import javax.crypto.spec.PBEKeySpec

class PinUnlockActivity  : BasePinActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinLeftButton.isVisible = false
        pinTopText.text = getString(R.string.enter_pin_unlock_tella)
    }

    override fun onSuccessSetPin(pin: String?) {
        super.onSuccessSetPin(pin)
        TellaKeysUI.getMainKeyStore().load(config.wrapper, PBEKeySpec(pin?.toCharArray()), object : MainKeyStore.IMainKeyLoadCallback {
            override fun onReady(mainKey: MainKey) {
                Timber.d("*** MainKeyStore.IMainKeyLoadCallback.onReady")
                TellaKeysUI.getMainKeyHolder().set(mainKey);
                TellaKeysUI.getCredentialsCallback().onSuccessfulUnlock(this@PinUnlockActivity)
            }

            override fun onError(throwable: Throwable) {
                Timber.d(throwable, "*** MainKeyStore.UnlockRegistry.IUnlocker.onError")
                TellaKeysUI.getCredentialsCallback().onUnSuccessfulUnlock()
                pinTopText.text = getString(R.string.incorrect_pin_error_msg)
            }
        })

    }

    override fun onPinChange(pinLength: Int, intermediatePin: String?) {
        super.onPinChange(pinLength, intermediatePin)
        pinTopText.text = getString(R.string.enter_pin_unlock_tella)
    }


}