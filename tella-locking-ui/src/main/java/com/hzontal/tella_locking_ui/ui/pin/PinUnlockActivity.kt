package com.hzontal.tella_locking_ui.ui.pin

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
                pinTopText.text = getString(R.string.settings_current_pin_msg)
            }
            ReturnActivity.CAMOUFLAGE.getActivityOrder() -> {
                backBtn = findViewById(R.id.backBtn)
                backBtn.visibility = View.VISIBLE
                backBtn.setOnClickListener { finish() }
                pinTopText.text = getString(R.string.settings_current_camo_pin_msg)
            }
            else -> {
                pinTopText.text = getString(R.string.enter_pin_unlock_tella)
            }
        }
    }

    override fun onSuccessSetPin(pin: String?) {
        TellaKeysUI.getMainKeyStore().load(config.wrapper, PBEKeySpec(pin?.toCharArray()), object : MainKeyStore.IMainKeyLoadCallback {
            override fun onReady(mainKey: MainKey) {
                TellaKeysUI.getMainKeyHolder().set(mainKey);
                onSuccessfulUnlock()
                finish()
            }

            override fun onError(throwable: Throwable) {
                onFailureSetPin(getString(R.string.incorrect_pin_error_msg))
                TellaKeysUI.getCredentialsCallback().onUnSuccessfulUnlock(TAG, throwable)
            }
        })
    }

    override fun onFailureSetPin(error: String) {
        pinTopText.setTextColor(ContextCompat.getColor(this, R.color.wa_red_error))
        pinTopText.text = error
    }

    override fun onPinChange(pinLength: Int, intermediatePin: String?) {
        super.onPinChange(pinLength, intermediatePin)
        pinTopText.setTextColor(ContextCompat.getColor(this, R.color.wa_white))
        pinTopText.text =  getString(
            when (returnActivity) {
                ReturnActivity.SETTINGS.getActivityOrder() -> R.string.settings_current_pin_msg
                ReturnActivity.CAMOUFLAGE.getActivityOrder() -> R.string.settings_current_camo_pin_msg
                else -> R.string.enter_pin_unlock_tella
            })
    }

}