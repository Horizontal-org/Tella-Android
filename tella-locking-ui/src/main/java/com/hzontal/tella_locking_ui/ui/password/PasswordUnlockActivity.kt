package com.hzontal.tella_locking_ui.ui.password

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.common.extensions.onChange
import com.hzontal.tella_locking_ui.ui.password.base.BasePasswordActivity
import org.hzontal.tella.keys.MainKeyStore
import org.hzontal.tella.keys.key.MainKey
import timber.log.Timber
import javax.crypto.spec.PBEKeySpec

class PasswordUnlockActivity : BasePasswordActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        topImageView.background = ContextCompat.getDrawable(this,R.drawable.tella_logo_dark_bg)
        enterPasswordTextView.isVisible = false
        passwordMsgTextView.text = getText(R.string.enter_password_tella)
        passwordEditText.onChange { passwordMsgTextView.text = getText(R.string.enter_password_tella) }
    }

    override fun onSuccessSetPassword(password: String) {
        TellaKeysUI.getMainKeyStore().load(config.wrapper, PBEKeySpec(password.toCharArray()), object : MainKeyStore.IMainKeyLoadCallback {
            override fun onReady(mainKey: MainKey) {
                Timber.d("*** MainKeyStore.IMainKeyLoadCallback.onReady")
                TellaKeysUI.getMainKeyHolder().set(mainKey);
                TellaKeysUI.getCredentialsCallback().onSuccessfulUnlock(this@PasswordUnlockActivity)
            }
            override fun onError(throwable: Throwable) {
                Timber.d(throwable, "*** MainKeyStore.UnlockRegistry.IUnlocker.onError")
                onFailureSetPassword(getString(R.string.incorrect_password_error_msg))
                TellaKeysUI.getCredentialsCallback().onUnSuccessfulUnlock()
            }
        })
    }

    override fun onFailureSetPassword(error: String) {
        passwordMsgTextView.text = error
        passwordEditText.setTextColor(ContextCompat.getColor(this,R.color.wa_red_error));
    }
}