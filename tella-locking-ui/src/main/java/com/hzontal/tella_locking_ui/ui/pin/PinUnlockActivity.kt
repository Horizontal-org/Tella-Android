package com.hzontal.tella_locking_ui.ui.pin

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.ReturnActivity
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.ui.pin.base.BasePinActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
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

    override fun onSuccessSetPin(pin: String?) {
        TellaKeysUI.getMainKeyStore().load(config.wrapper,
            PBEKeySpec(pin?.toCharArray()),
            object : MainKeyStore.IMainKeyLoadCallback {
                override fun onReady(mainKey: MainKey) {
                    TellaKeysUI.getMainKeyHolder().set(mainKey)
                    onSuccessfulUnlock()
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
        pinTopText.setTextColor(ContextCompat.getColor(this, R.color.wa_red_error))
        pinTopText.text = error
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

}