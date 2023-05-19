package com.hzontal.tella_locking_ui.ui.password

import android.os.Bundle
import android.view.KeyEvent
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.ReturnActivity
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.common.extensions.onChange
import com.hzontal.tella_locking_ui.ui.password.base.BasePasswordActivity
import org.hzontal.tella.keys.MainKeyStore
import org.hzontal.tella.keys.key.MainKey
import timber.log.Timber
import javax.crypto.spec.PBEKeySpec

private const val TAG = "PasswordUnlockActivity"

class PasswordUnlockActivity : BasePasswordActivity() {


    private lateinit var backBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {

        topImageView.background = ContextCompat.getDrawable(this, R.drawable.tella_logo_dark_bg)
        enterPasswordTextView.isVisible = false
        passwordLeftButton.isVisible = false
        passwordRightButton.isVisible = false
        passwordEditText.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                if (isHiLighted)
                    onSuccessSetPassword(mPassword)
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        when (returnActivity) {
            ReturnActivity.SETTINGS.getActivityOrder() -> {
                backBtn = findViewById(R.id.backBtn)
                backBtn.isVisible = true
                backBtn.setOnClickListener { finish() }
                passwordMsgTextView.text = getString(R.string.LockPasswordSet_Settings_EnterCurrentPassword)
                passwordEditText.hint = getString(R.string.LockPasswordSet_Settings_EnterCurrentPassword)
            }
            ReturnActivity.CAMOUFLAGE.getActivityOrder() -> {
                backBtn = findViewById(R.id.backBtn)
                backBtn.isVisible = true
                backBtn.setOnClickListener { finish() }
                passwordMsgTextView.text = getString(R.string.LockPasswordSet_Settings_EnterCurrentPasswordToChangeCamouflage)
                passwordEditText.hint = getString(R.string.LockPasswordSet_Settings_EnterCurrentPasswordToChangeCamouflage)
            }
            else -> {
                passwordMsgTextView.text = getText(R.string.UnlockPassword_Message_EnterPassword)
                passwordEditText.hint = getString(R.string.UnlockPassword_Message_EnterPassword)
                passwordEditText.onChange {
                    passwordMsgTextView.text = getText(R.string.UnlockPassword_Message_EnterPassword)
                }
            }
        }
    }

    override fun onSuccessSetPassword(password: String) {
        TellaKeysUI.getMainKeyStore().load(config.wrapper, PBEKeySpec(password.toCharArray()), object : MainKeyStore.IMainKeyLoadCallback {
            override fun onReady(mainKey: MainKey) {
                Timber.d("*** MainKeyStore.IMainKeyLoadCallback.onReady")
                TellaKeysUI.getMainKeyHolder().set(mainKey);
                onSuccessfulUnlock()
                finish()
            }

            override fun onError(throwable: Throwable) {
                Timber.d(throwable, "*** MainKeyStore.UnlockRegistry.IUnlocker.onError")
                onFailureSetPassword(getString(R.string.LockPasswordSet_Message_Error_IncorrectPassword))
                TellaKeysUI.getCredentialsCallback().onUnSuccessfulUnlock(TAG, throwable)
            }
        })
    }

    override fun onFailureSetPassword(error: String) {
        passwordMsgTextView.text = error
        passwordEditText.setTextColor(ContextCompat.getColor(this, R.color.wa_red_error));
    }
}