package com.hzontal.tella_locking_ui.ui.password

import android.os.Bundle
import org.hzontal.shared_ui.utils.DialogUtils
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.ui.password.base.BasePasswordActivity
import org.hzontal.tella.keys.MainKeyStore
import org.hzontal.tella.keys.config.UnlockRegistry
import org.hzontal.tella.keys.key.MainKey
import timber.log.Timber
import javax.crypto.spec.PBEKeySpec

class ConfirmPasswordActivity : BasePasswordActivity() {
    private val mConfirmPassword by lazy { intent.getStringExtra(CONFIRM_PASSWORD) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTopText(getString(R.string.LockPasswordConfirm_Message_ConfirmPassword))
        setMessageText(getString(R.string.LockPasswordConfirm_Message_EnterPasswordAgain))

    }

    override fun onSuccessSetPassword(password: String) {
        if (password == mConfirmPassword) {
            val keySpec = PBEKeySpec(password.toCharArray())
            TellaKeysUI.getUnlockRegistry().setActiveMethod(this@ConfirmPasswordActivity, UnlockRegistry.Method.TELLA_PASSWORD)
            val config = TellaKeysUI.getUnlockRegistry().getActiveConfig(this@ConfirmPasswordActivity)
            TellaKeysUI.getMainKeyStore().store(generateOrGetMainKey(), config.wrapper, keySpec, object : MainKeyStore.IMainKeyStoreCallback {
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
            onFailureSetPassword(getString(R.string.LockPasswordConfirm_Message_Error_PasswordsNotMatch))

    }

    override fun onFailureSetPassword(error: String) {
        hideKeyboard()
        DialogUtils.showBottomMessage(this, error, false)
    }

}