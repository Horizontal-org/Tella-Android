package com.hzontal.tella_locking_ui.ui.pattern

import android.os.Bundle
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.ReturnActivity
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.TellaKeysUI.getMainKeyHolder
import com.hzontal.tella_locking_ui.common.ErrorMessageUtil
import com.hzontal.tella_locking_ui.patternlock.ConfirmPatternActivity
import com.hzontal.tella_locking_ui.patternlock.PatternUtils
import com.hzontal.tella_locking_ui.patternlock.PatternView
import org.hzontal.tella.keys.MainKeyStore.IMainKeyLoadCallback
import org.hzontal.tella.keys.key.MainKey
import timber.log.Timber
import java.lang.Error
import javax.crypto.spec.PBEKeySpec


private const val TAG = "PatternUnlockActivity"

class PatternUnlockActivity : ConfirmPatternActivity() {
    private var isPatternCorrect = false
    private lateinit var backBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    override fun isPatternCorrect(pattern: MutableList<PatternView.Cell>?): Boolean {
        val passphrase = PatternUtils.patternToSha1String(pattern).toCharArray()

        TellaKeysUI.getMainKeyStore()
            .load(config.wrapper, PBEKeySpec(passphrase), object : IMainKeyLoadCallback {

                override fun onReady(mainKey: MainKey) {
                    Timber.d("*** MainKeyStore.IMainKeyLoadCallback.onReady"+mainKey.key)
                    getMainKeyHolder().set(mainKey);
                    onSuccessfulUnlock()
                    ErrorMessageUtil.resetUnlockAttempts()
                    isPatternCorrect = true
                }

                override fun onError(throwable: Throwable) {
                    Timber.d(throwable, "*** MainKeyStore.UnlockRegistry.IUnlocker.onError")
                    TellaKeysUI.getCredentialsCallback().onUnSuccessfulUnlock(TAG, throwable)
                    isPatternCorrect = false
                }
            })

        return isPatternCorrect
    }

    override fun onWrongPattern() {
        super.onWrongPattern()
        showErrorMessage()
    }

    private fun showErrorMessage() {
        val numFailedAttempts = TellaKeysUI.getNumFailedAttempts()
        val isShowRemainingAttempts = TellaKeysUI.isShowRemainingAttempts()

        if (numFailedAttempts == 0L ) {
            mMessageText.text = getString(R.string.LockPatternConfirm_Message_WrongPattern)
        } else {

           mMessageText.text = ErrorMessageUtil.generateErrorMessage(this,R.string.incorrect_pattern,R.string.LockPatternConfirm_Message_WrongPattern,isShowRemainingAttempts)
        }
    }


    override fun onConfirmed() {
        super.onConfirmed()
        finish()
    }

    private fun initView() {
        mLeftButton.isVisible = false
        mRightButton.isVisible = false

        when (returnActivity) {
            ReturnActivity.SETTINGS.getActivityOrder() -> {
                backBtn = findViewById(R.id.backBtn)
                backBtn.isVisible = true
                backBtn.setOnClickListener { finish() }
                mMessageText.text = getString(R.string.LockPatternSet_Settings_DrawCurrentPattern)
            }

            ReturnActivity.CAMOUFLAGE.getActivityOrder() -> {
                backBtn = findViewById(R.id.backBtn)
                backBtn.isVisible = true
                backBtn.setOnClickListener { finish() }
                mMessageText.text =
                    getString(R.string.LockPatternSet_Settings_DrawCurrentPatternToChangeCamouflage)
            }

            else -> {
            }
        }
    }
}