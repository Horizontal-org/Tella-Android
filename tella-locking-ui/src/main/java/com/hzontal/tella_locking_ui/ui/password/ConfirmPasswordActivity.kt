package com.hzontal.tella_locking_ui.ui.password

import android.os.Bundle
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.ui.password.base.BasePasswordActivity
import com.hzontal.tella_locking_ui.ui.utils.DialogUtils
import org.hzontal.tella.keys.MainKeyStore
import org.hzontal.tella.keys.config.UnlockRegistry
import org.hzontal.tella.keys.key.LifecycleMainKey
import org.hzontal.tella.keys.key.MainKey
import timber.log.Timber
import javax.crypto.spec.PBEKeySpec

class ConfirmPasswordActivity : BasePasswordActivity() {
    private val mConfirmPassword by lazy { intent.getStringExtra(CONFIRM_PASSWORD) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTopText(getString(R.string.confirm_password))
        setMessageText(getString(R.string.confirm_password_msg))

    }

    override fun onSuccessSetPassword(password: String) {
        if (password == mConfirmPassword) {
            var mainKey: MainKey
            try {
                isConfirmSettingsUpdate = true
                mainKey = TellaKeysUI.getMainKeyHolder().get()
            } catch (e: LifecycleMainKey.MainKeyUnavailableException) {
                mainKey = MainKey.generate()
            }
            val keySpec = PBEKeySpec(password.toCharArray())
            TellaKeysUI.getUnlockRegistry().setActiveMethod(this@ConfirmPasswordActivity, UnlockRegistry.Method.TELLA_PASSWORD)
            val config = TellaKeysUI.getUnlockRegistry().getActiveConfig(this@ConfirmPasswordActivity)
            TellaKeysUI.getMainKeyStore().store(mainKey, config.wrapper, keySpec, object : MainKeyStore.IMainKeyStoreCallback {
                override fun onSuccess(mainKey: MainKey) {
                    Timber.d("** MainKey stored: %s **", mainKey)
                    // here, we store MainKey in memory -> unlock the app
                    TellaKeysUI.getMainKeyHolder().set(mainKey)
                    onSuccessConfirmUnlock()
                }

                override fun onError(throwable: Throwable) {
                    Timber.e(throwable, "** MainKey store error **")
                    onFailureSetPassword("General error occurred")
                }
            })
        } else
            onFailureSetPassword(getString(R.string.confirm_password_error_msg))

    }

    override fun onFailureSetPassword(error: String) {
        hideKeyboard()
        DialogUtils.showBottomMessage(this, error, false)
    }

}