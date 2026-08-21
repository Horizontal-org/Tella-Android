package com.hzontal.tella_locking_ui.ui.pin

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.ReturnActivity
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.common.ErrorMessageUtil
import com.hzontal.tella_locking_ui.patternlock.ConfirmPatternActivity
import com.hzontal.tella_locking_ui.ui.pin.base.BasePinActivity
import org.hzontal.shared_ui.security.QuickDeletePinManager
import org.hzontal.tella.keys.MainKeyStore
import org.hzontal.tella.keys.key.MainKey
import javax.crypto.spec.PBEKeySpec

private const val TAG = "PinUnlockActivity"

class PinUnlockActivity : BasePinActivity() {
    private lateinit var backBtn: ImageView
    private val enterCurrentPinRes = R.string.LockPinSet_Settings_EnterCurrentPin
    private val enterPinToCamouflageRes =
        R.string.LockPinSet_Settings_EnterCurrentPinToChangeCamouflage
    private val enterPinToUnlockRes = R.string.UnlockPin_Message_EnterPin
    private var mNumFailedAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()

        mNumFailedAttempts =
            savedInstanceState?.getInt(ConfirmPatternActivity.KEY_NUM_FAILED_ATTEMPTS) ?: 0
    }

    private fun initView() {
        pinMsgText.visibility = View.GONE
        pinLeftButton.visibility = View.GONE
        pinRightButton.visibility = View.GONE
        pinTopImageView.background = ContextCompat.getDrawable(this, R.drawable.tella_logo_dark_bg)
        when (returnActivity) {
            ReturnActivity.SETTINGS.getActivityOrder() -> {
                backBtn = findViewById(R.id.backBtn)
                backBtn.visibility = View.VISIBLE
                backBtn.setOnClickListener { finish() }
                setUnlockText(enterCurrentPinRes)
            }

            ReturnActivity.CAMOUFLAGE.getActivityOrder() -> {
                backBtn = findViewById(R.id.backBtn)
                backBtn.visibility = View.VISIBLE
                backBtn.setOnClickListener { finish() }
                setUnlockText(enterPinToCamouflageRes)
            }

            else -> {
                setUnlockText(enterPinToUnlockRes)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ConfirmPatternActivity.KEY_NUM_FAILED_ATTEMPTS, mNumFailedAttempts)
    }

    /**
     * 2025-08-20 (audit-fix rev 8): Wire up the Quick Delete PIN trigger.
     *
     * BEFORE attempting the normal unlock, check whether the entered PIN
     * matches the Quick Delete PIN. If it does, trigger the destructive
     * wipe via [TellaKeysUI.getCredentialsCallback.onFailedAttempts] (which
     * calls `ActivityManager.clearApplicationUserData()` in MyApplication).
     * Do NOT proceed with unlock — the Quick Delete PIN is a duress PIN,
     * not a real unlock credential.
     *
     * Only check on the normal unlock path (not SETTINGS / CAMOUFLAGE
     * return activities) — the user is changing their lock config there
     * and shouldn't accidentally trigger a wipe by entering their
     * Quick Delete PIN.
     */
    override fun onSuccessSetPin(pin: String?) {
        // 2025-08-20 (audit-fix rev 8): Quick Delete PIN trigger.
        if (returnActivity == ReturnActivity.SETTINGS.getActivityOrder() ||
            returnActivity == ReturnActivity.CAMOUFLAGE.getActivityOrder()) {
            // Skip the quick-delete check on Settings / Camouflage flows —
            // the user is changing their lock config, not unlocking.
        } else if (pin != null && QuickDeletePinManager.isSet(this) &&
            QuickDeletePinManager.matches(this, pin)) {
            // Duress PIN entered — trigger the destructive wipe.
            // onFailedAttempts(0) → MyApplication calls
            // ActivityManager.clearApplicationUserData() which wipes the
            // app's /data directory (vault DB, forms, servers, prefs).
            TellaKeysUI.getCredentialsCallback().onFailedAttempts(0L)
            finish()
            return
        }

        TellaKeysUI.getMainKeyStore().load(config.wrapper,
            PBEKeySpec(pin?.toCharArray()),
            object : MainKeyStore.IMainKeyLoadCallback {
                override fun onReady(mainKey: MainKey) {
                    TellaKeysUI.getMainKeyHolder().set(mainKey)
                    onSuccessfulUnlock()
                    ErrorMessageUtil.resetUnlockAttempts()
                    finish()
                }

                override fun onError(throwable: Throwable) {
                    onFailureSetPin(getString(R.string.LockPinConfirm_Message_Error_IncorrectPin))
                    TellaKeysUI.getCredentialsCallback().onUnSuccessfulUnlock(TAG, throwable)
                }
            })
    }

    private fun setUnlockText(res: Int) {
        pinTopText.text = getString(res)
        pinEditText.setHint(res)
    }

    override fun onFailureSetPin(error: String) {
        if (TellaKeysUI.getNumFailedAttempts() == 0L) {
            pinTopText.setTextColor(ContextCompat.getColor(this, R.color.light_red))
            pinTopText.text = error
            pinTopText.requestFocus()
            pinTopText.announceForAccessibility(error)
        } else {
            onWrongPattern()
        }
    }

    override fun onPinChange(pinLength: Int, intermediatePin: String?) {
        super.onPinChange(pinLength, intermediatePin)
        pinTopText.setTextColor(ContextCompat.getColor(this, R.color.wa_white))

        val pinTextRes = when (returnActivity) {
            ReturnActivity.SETTINGS.getActivityOrder() -> {
                enterCurrentPinRes
            }

            ReturnActivity.CAMOUFLAGE.getActivityOrder() -> {
                enterPinToCamouflageRes
            }

            else -> {
                enterPinToUnlockRes
            }
        }
        setUnlockText(pinTextRes)
    }

    private fun onWrongPattern() {
        showErrorMessage()
    }

    private fun showErrorMessage() {
        val error = ErrorMessageUtil.generateErrorMessage(
            this,
            R.string.incorrect_pin,
            R.string.LockPinConfirm_Message_Error_IncorrectPin,
            TellaKeysUI.isShowRemainingAttempts()
        )
        pinTopText.text = error
        pinTopText.requestFocus()
        pinTopText.announceForAccessibility(error)
    }


}