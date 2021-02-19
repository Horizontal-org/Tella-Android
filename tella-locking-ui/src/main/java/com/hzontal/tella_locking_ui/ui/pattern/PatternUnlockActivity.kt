package com.hzontal.tella_locking_ui.ui.pattern

import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.TellaKeysUI.getMainKeyHolder
import com.hzontal.tella_locking_ui.patternlock.ConfirmPatternActivity
import com.hzontal.tella_locking_ui.patternlock.PatternUtils
import com.hzontal.tella_locking_ui.patternlock.PatternView
import org.hzontal.tella.keys.MainKeyStore.IMainKeyLoadCallback
import org.hzontal.tella.keys.key.MainKey
import timber.log.Timber
import javax.crypto.spec.PBEKeySpec


class PatternUnlockActivity : ConfirmPatternActivity() {

    private lateinit var mNewPassphrase: String
    private var isPatternCorrect = false

    override fun isPatternCorrect(pattern: MutableList<PatternView.Cell>?): Boolean {
        mNewPassphrase = PatternUtils.patternToSha1String(pattern)

        TellaKeysUI.getMainKeyStore().load(config.wrapper, PBEKeySpec(mNewPassphrase.toCharArray()), object : IMainKeyLoadCallback {
            override fun onReady(mainKey: MainKey) {
                Timber.d("*** MainKeyStore.IMainKeyLoadCallback.onReady")
                getMainKeyHolder().set(mainKey);
                TellaKeysUI.getCredentialsCallback().onSuccessfulUnlock(this@PatternUnlockActivity)
                isPatternCorrect = true
            }

            override fun onError(throwable: Throwable) {
                Timber.d(throwable, "*** MainKeyStore.UnlockRegistry.IUnlocker.onError")
                TellaKeysUI.getCredentialsCallback().onUnSuccessfulUnlock()
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