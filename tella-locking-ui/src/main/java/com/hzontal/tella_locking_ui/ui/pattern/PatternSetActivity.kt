package com.hzontal.tella_locking_ui.ui.pattern

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.patternlock.ConfirmSetPatternActivity
import com.hzontal.tella_locking_ui.patternlock.PatternUtils
import com.hzontal.tella_locking_ui.patternlock.PatternView
import com.hzontal.tella_locking_ui.patternlock.SetPatternActivity
import com.hzontal.tella_locking_ui.ui.ConfirmCredentialsActivity
import org.hzontal.tella.keys.MainKeyStore
import org.hzontal.tella.keys.config.UnlockRegistry
import org.hzontal.tella.keys.key.MainKey
import timber.log.Timber
import javax.crypto.spec.PBEKeySpec

class PatternSetActivity : SetPatternActivity() {
    @SuppressLint("StringFormatInvalid")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMessageText.text = getString(R.string.pl_pattern_too_short, minPatternSize)
        mTopImageView.background = ContextCompat.getDrawable(this,R.drawable.pattern_draw_bg)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onConfirmed() {
        super.onConfirmed()
        Timber.d("** We've finished first MainKey saving - now we need to proceed with application **")
    }

    override fun getMinPatternSize(): Int {
        return 6
    }

    override fun onPatternDetected(newPattern: MutableList<PatternView.Cell>) {
        super.onPatternDetected(newPattern)

        when (mStage) {
             Stage.Confirm,Stage.ConfirmWrong -> {
                if (newPattern == mPattern) {
                    updateStage(Stage.ConfirmCorrect);
                } else {
                    updateStage(Stage.ConfirmWrong);
                }
            }
        }
    }


}