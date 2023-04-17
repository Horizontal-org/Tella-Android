package com.hzontal.tella_locking_ui.ui.pin.base

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.common.BaseActivity
import com.hzontal.tella_locking_ui.ui.pin.edit_text.NoImeEditText
import org.hzontal.shared_ui.pinview.PinLockListener
import org.hzontal.shared_ui.pinview.PinLockView

abstract class BasePinActivity : BaseActivity(), PinLockListener, View.OnClickListener, OnSetPinClickListener {

    private lateinit var pinLockView: PinLockView
    lateinit var pinLeftButton: TextView
    lateinit var pinRightButton: TextView
    lateinit var pinTopText: TextView
    lateinit var pinMsgText: TextView
    lateinit var pinTopImageView: AppCompatImageView
    lateinit var pinEditText: NoImeEditText
    private lateinit var pinEyeImageView: ImageView
    private lateinit var pinClickView: View
    private var isPasswordMode = true
    private var mPIN: String? = ""
    private var isRightButtonHighLighted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)
        initView()
    }

    private fun initView() {
        pinLockView = findViewById(R.id.pin_lock_view)
        pinLockView.minPinLength = 6
        pinLockView.setPinLockListener(this)
        pinLeftButton = findViewById(R.id.pin_left_button)
        pinRightButton = findViewById(R.id.pin_right_button)
        pinEditText = findViewById(R.id.pin_editText)
        pinEyeImageView = findViewById(R.id.pin_eye)
        pinTopText = findViewById(R.id.pin_enterTV)
        pinMsgText = findViewById(R.id.pin_msgTV)
        pinClickView = findViewById(R.id.pinClickView)
        pinTopImageView = findViewById(R.id.pin_TopImg)
        pinLeftButton.text =
            getText(if (!isFromSettings) R.string.LockSelect_Action_Back else R.string.LockSelect_Action_Cancel)
        initListeners()
    }

    private fun initListeners() {
        pinClickView.setOnClickListener(this)
        pinRightButton.setOnClickListener(null)
        pinLeftButton.setOnClickListener(this)
    }

    override fun onEmpty() {
        pinEditText.setText("")
    }

    override fun onPinConfirmation(pin: String?) {
        onSuccessSetPin(pin)
    }

    override fun onPinChange(pinLength: Int, intermediatePin: String?) {
        isRightButtonHighLighted = pinLength.compareTo(pinLockView.minPinLength - 1) == 1
        hiLightLeftButton(isRightButtonHighLighted)
        mPIN = intermediatePin
        pinEditText.setText(intermediatePin)
    }

    private fun onLeftButtonClickListener() {
        finish()
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left)
    }

    private fun onRightButtonClickListener() {
        onSuccessSetPin(mPIN)
    }

    private fun onSowPasswordClickListener() {
        isPasswordMode = !isPasswordMode
        pinEditText.transformationMethod = if (isPasswordMode) PasswordTransformationMethod() else null
        pinEyeImageView.background = if (isPasswordMode) ContextCompat.getDrawable(this@BasePinActivity, R.drawable.eye) else ContextCompat.getDrawable(this@BasePinActivity, R.drawable.eye_off)
        pinClickView.contentDescription = if (isPasswordMode) getString(R.string.action_show_pin) else getString(R.string.action_hide_pin)


    }

    private fun hiLightLeftButton(isHiLighted: Boolean) {
        pinRightButton.setTextColor(if (isHiLighted) ContextCompat.getColor(this, R.color.wa_white) else ContextCompat.getColor(this, R.color.wa_white_40))
        pinRightButton.setOnClickListener(if (isHiLighted) this else null)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.pinClickView -> {
                onSowPasswordClickListener()
            }
            R.id.pin_right_button -> {
                onRightButtonClickListener()
            }
            R.id.pin_left_button -> {
                onLeftButtonClickListener()
            }
        }
    }

}