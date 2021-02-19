package com.hzontal.tella_locking_ui.ui.pin

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.ui.pin.base.BasePinActivity
import org.hzontal.tella.keys.MainKeyStore
import org.hzontal.tella.keys.key.MainKey
import javax.crypto.spec.PBEKeySpec

class PinUnlockActivity  : BasePinActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinLeftButton.isVisible = false
        pinTopText.text = getString(R.string.enter_pin_unlock_tella)
    }

    override fun onSuccessSetPin(pin: String?) {
        TellaKeysUI.getMainKeyStore().load(config.wrapper, PBEKeySpec(pin?.toCharArray()), object : MainKeyStore.IMainKeyLoadCallback {
            override fun onReady(mainKey: MainKey) {
                TellaKeysUI.getMainKeyHolder().set(mainKey);
                TellaKeysUI.getCredentialsCallback().onSuccessfulUnlock(this@PinUnlockActivity)
            }

            override fun onError(throwable: Throwable) {
                onFailureSetPin(getString(R.string.incorrect_pin_error_msg))
            }
        })
    }

    override fun onFailureSetPin(error: String) {
        TellaKeysUI.getCredentialsCallback().onUnSuccessfulUnlock()
        pinTopText.setTextColor(ContextCompat.getColor(this,R.color.wa_red_error))
        pinTopText.text = error
    }

    override fun onPinChange(pinLength: Int, intermediatePin: String?) {
        super.onPinChange(pinLength, intermediatePin)
        pinTopText.setTextColor(ContextCompat.getColor(this,R.color.wa_white))
        pinTopText.text = getString(R.string.enter_pin_unlock_tella)
    }

}