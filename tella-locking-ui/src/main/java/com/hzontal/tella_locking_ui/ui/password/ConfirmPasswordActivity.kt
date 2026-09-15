package com.hzontal.tella_locking_ui.ui.password

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import org.hzontal.shared_ui.utils.DialogUtils
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
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
        passwordEditText.hint = getString(R.string.LockPasswordConfirm_Message_ConfirmPassword)
        passwordMsgTextView.gravity = Gravity.CENTER
        passwordMsgTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        setMessageText(getString(R.string.LockPasswordConfirm_Message_EnterPasswordAgain))
    }

    override fun onSuccessSetPassword(password: String) {
        if (password == mConfirmPassword) {
            val keySpec = PBEKeySpec(password.toCharArray())
            val config = TellaKeysUI.getUnlockRegistry()
                .getRegisteredConfig(UnlockRegistry.Method.TELLA_PASSWORD)
            val persist = persist@{
                val mainKey = generateOrGetMainKey() ?: return@persist
                TellaKeysUI.getMainKeyStore().store(mainKey, config.wrapper, keySpec, object : MainKeyStore.IMainKeyStoreCallback {
                    override fun onSuccess(mainKey: MainKey) {
                        Timber.d("** MainKey stored: %s **", mainKey)
                        TellaKeysUI.getUnlockRegistry().setActiveMethod(
                            this@ConfirmPasswordActivity,
                            UnlockRegistry.Method.TELLA_PASSWORD
                        )
                        TellaKeysUI.getMainKeyHolder().set(mainKey)
                        onSuccessConfirmUnlock()
                    }

                    override fun onError(throwable: Throwable) {
                        Timber.e(throwable, "** MainKey store error **")
                        onFailureSetPassword("General error occurred")
                    }
                })
            }
            if (isFromSettings) persist() else showOnboardingProtectSheet(persist)
        } else
            onFailureSetPassword(getString(R.string.LockPasswordConfirm_Message_Error_PasswordsNotMatch))
    }

    override fun onFailureSetPassword(error: String) {
        hideKeyboard()
        DialogUtils.showBottomMessage(this, error, false)
    }

    override fun onPasswordBackPressed() {
        if (!isFromSettings) {
            startActivity(
                Intent(this, SetPasswordActivity::class.java).putExtra(IS_FROM_SETTINGS, false)
            )
        }
        super.onPasswordBackPressed()
    }
}