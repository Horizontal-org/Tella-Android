package com.hzontal.tella_locking_ui.ui.pattern

import android.os.Bundle
import androidx.core.view.isVisible
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.patternlock.PatternUtils
import com.hzontal.tella_locking_ui.patternlock.PatternView
import com.hzontal.tella_locking_ui.patternlock.SetPatternActivity
import org.hzontal.tella.keys.MainKeyStore
import org.hzontal.tella.keys.config.UnlockRegistry
import org.hzontal.tella.keys.key.LifecycleMainKey
import org.hzontal.tella.keys.key.MainKey
import timber.log.Timber
import javax.crypto.spec.PBEKeySpec

class PatternSetConfirmActivity : SetPatternActivity() {

    private var pattern: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateStage(Stage.Confirm)
        pattern = intent.extras?.getString(PATTERN_CELL_BYTES)
        mRightButton.isVisible = false
    }

    override fun onPatternDetected(newPattern: MutableList<PatternView.Cell>?) {
        mPattern = newPattern
        when (mStage) {
            Stage.Confirm, Stage.ConfirmWrong -> {
                if (PatternUtils.patternToSha1String(mPattern, mPattern.size) == pattern) {
                    updateStage(Stage.ConfirmCorrect);
                    onSetPattern(newPattern)
                } else {
                    updateStage(Stage.ConfirmWrong);
                }
            }
            else -> {
                super.onPatternDetected(newPattern)
            }
        }
    }

    override fun onSetPattern(pattern: MutableList<PatternView.Cell>?) {
        // Here we are storing MainKey for the first time: generate it, wrap and store
        // also, we are going to set active unlocking to be TELLA_PATTERN
        //
        // One remark: we need TELLA_PATTERN and LEGACY_TELLA_PATTERN if we are to change "char[] password"
        // generation from "pattern"
        super.onSetPattern(pattern)

        val mNewPassphrase = PatternUtils.patternToSha1String(pattern)
        // holder.unlockRegistry.setActiveMethod(applicationContext, UnlockRegistry.Method.TELLA_PATTERN)
        // I've put this in LockApp - for holder.unlockRegistry.getActiveConfig(this) to work
        TellaKeysUI.getUnlockRegistry().setActiveMethod(this@PatternSetConfirmActivity, UnlockRegistry.Method.TELLA_PATTERN)
        val keySpec = PBEKeySpec(mNewPassphrase.toCharArray())
        val config = TellaKeysUI.getUnlockRegistry().getActiveConfig(this@PatternSetConfirmActivity)

        TellaKeysUI.getMainKeyStore().store(generateOrGetMainKey(), config.wrapper, keySpec, object : MainKeyStore.IMainKeyStoreCallback {
            override fun onSuccess(mainKey: MainKey) {
                Timber.d("** MainKey stored: %s **", mainKey)
                // here, we store MainKey in memory -> unlock the app
                TellaKeysUI.getMainKeyHolder().set(mainKey)
                onSuccessConfirmUnlock()
            }

            override fun onError(throwable: Throwable) {
                Timber.e(throwable, "** MainKey store error **")
                onCanceled()
            }
        })

    }

}