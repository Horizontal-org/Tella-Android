package com.hzontal.tella_locking_ui.ui.pin

import android.os.Bundle
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.ui.pin.base.BasePinActivity
import com.hzontal.tella_locking_ui.ui.utils.DialogUtils
import org.hzontal.tella.keys.MainKeyStore
import org.hzontal.tella.keys.config.UnlockRegistry
import org.hzontal.tella.keys.key.LifecycleMainKey
import org.hzontal.tella.keys.key.MainKey
import timber.log.Timber
import javax.crypto.spec.PBEKeySpec

class ConfirmPinActivity  : BasePinActivity() {

    private val mConfirmPin by lazy { intent.getStringExtra(CONFIRM_PIN) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinTopText.text = getString(R.string.confirm_pin)
        pinMsgText.text = getString(R.string.confirm_pin_msg)
    }

    override fun onSuccessSetPin(pin: String?) {
       if (mConfirmPin == pin) {
           var mainKey: MainKey
           try {
               isConfirmSettingsUpdate = true
               mainKey = TellaKeysUI.getMainKeyHolder().get()
           } catch (e: LifecycleMainKey.MainKeyUnavailableException) {
               mainKey = MainKey.generate()
           }
           val keySpec = PBEKeySpec(pin?.toCharArray())
           TellaKeysUI.getUnlockRegistry().setActiveMethod(this@ConfirmPinActivity,UnlockRegistry.Method.TELLA_PIN)
           val config = TellaKeysUI.getUnlockRegistry().getActiveConfig(this@ConfirmPinActivity)
           TellaKeysUI.getMainKeyStore().store(mainKey, config.wrapper, keySpec, object : MainKeyStore.IMainKeyStoreCallback {
               override fun onSuccess(mainKey: MainKey) {
                   Timber.d("** MainKey stored: %s **", mainKey)
                   // here, we store MainKey in memory -> unlock the app
                   TellaKeysUI.getMainKeyHolder().set(mainKey)
                   onSuccessConfirmUnlock()
               }
               override fun onError(throwable: Throwable) {
                   onFailureSetPin("General error occurred")
                   Timber.e(throwable, "** MainKey store error **")
               }
           })
        }
       else{
            onFailureSetPin(getString(R.string.confirm_pin_error_msg))
       }
    }

    override fun onFailureSetPin(error: String) {
        DialogUtils.showBottomMessage(this,error,false)
    }

}