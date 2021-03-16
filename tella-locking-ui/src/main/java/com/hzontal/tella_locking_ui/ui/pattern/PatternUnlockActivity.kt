package com.hzontal.tella_locking_ui.ui.pattern

import android.os.Bundle
import androidx.core.view.isVisible
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.TellaKeysUI.getMainKeyHolder
import com.hzontal.tella_locking_ui.patternlock.ConfirmPatternActivity
import com.hzontal.tella_locking_ui.patternlock.PatternUtils
import com.hzontal.tella_locking_ui.patternlock.PatternView
import org.hzontal.tella.keys.MainKeyStore.IMainKeyLoadCallback
import org.hzontal.tella.keys.key.MainKey
import timber.log.Timber
import javax.crypto.spec.PBEKeySpec

private const val TAG = "PatternUnlockActivity"
class PatternUnlockActivity : ConfirmPatternActivity() {
    private var isPatternCorrect = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLeftButton.isVisible = false
        mRightButton.isVisible = false
    }
    override fun isPatternCorrect(pattern: MutableList<PatternView.Cell>?): Boolean {
        val passphrase = PatternUtils.patternToSha1String(pattern).toCharArray()

        TellaKeysUI.getMainKeyStore().load(config.wrapper, PBEKeySpec(passphrase), object : IMainKeyLoadCallback {
            override fun onReady(mainKey: MainKey) {
                Timber.d("*** MainKeyStore.IMainKeyLoadCallback.onReady")
                getMainKeyHolder().set(mainKey)
                TellaKeysUI.getCredentialsCallback().onSuccessfulUnlock(this@PatternUnlockActivity)
                isPatternCorrect = true
            }

            override fun onError(throwable: Throwable) {
                Timber.d(throwable, "*** MainKeyStore.UnlockRegistry.IUnlocker.onError")
                TellaKeysUI.getCredentialsCallback().onUnSuccessfulUnlock(TAG,throwable)
                isPatternCorrect = false
            }
        })

        return isPatternCorrect
    }

    override fun onConfirmed() {
        super.onConfirmed()
        finish()
    }
}