package com.hzontal.tella_locking_ui.ui.password

import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.ui.password.base.BasePasswordActivity
import com.hzontal.tella_locking_ui.ui.utils.DialogUtils
import org.hzontal.tella.keys.MainKeyStore
import org.hzontal.tella.keys.key.MainKey
import timber.log.Timber
import javax.crypto.spec.PBEKeySpec

class ConfirmPasswordActivity : BasePasswordActivity() {
    private val mConfirmPassword by lazy { intent.getStringExtra(CONFIRM_PASSWORD) }

    override fun onSuccessSetPassword(password: String) {
        if (password == mConfirmPassword) {
            val mainKey = MainKey.generate()
            val keySpec = PBEKeySpec(password.toCharArray())
            val config = TellaKeysUI.getUnlockRegistry().getActiveConfig(this@ConfirmPasswordActivity)

            TellaKeysUI.getMainKeyStore().store(mainKey, config.wrapper, keySpec, object : MainKeyStore.IMainKeyStoreCallback {
                override fun onSuccess(mainKey: MainKey) {
                    Timber.d("** MainKey stored: %s **", mainKey)
                    // here, we store MainKey in memory -> unlock the app
                    TellaKeysUI.getMainKeyHolder().set(mainKey)
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
        DialogUtils.showBottomMessage(this, error, false)
    }

}