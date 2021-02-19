package com.hzontal.tella_locking_ui.ui.pin.base

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.common.BaseActivity
import com.hzontal.tella_locking_ui.ui.pin.edit_text.NoImeEditText
import com.hzontal.tella_locking_ui.ui.pin.pinview.PinLockListener
import com.hzontal.tella_locking_ui.ui.pin.pinview.PinLockView

abstract class BasePinActivity : BaseActivity(), PinLockListener, View.OnClickListener,OnSetPinClickListener{

    private lateinit var pinLockView: PinLockView
    lateinit var pinLeftButton: TextView
    private lateinit var pinRightButton: TextView
    lateinit var pinTopText: TextView
    lateinit var pinMsgText: TextView
    private lateinit var pinEditText: NoImeEditText
    private lateinit var pinEyeImageView: AppCompatImageView
    private lateinit var pinClickView : View
    private var isPasswordMode = true
    private var mPIN : String? = ""
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

    override fun onMinLengthReached(pin: String?) {
    }

    override fun onPinChange(pinLength: Int, intermediatePin: String?) {
        isRightButtonHighLighted = pinLength.compareTo(pinLockView.minPinLength -1) == 1
        hiLightLeftButton(isRightButtonHighLighted)
        mPIN = intermediatePin
        pinEditText.setText(intermediatePin)
    }

    private fun onLeftButtonClickListener() {
        finish()
        overridePendingTransition(0, 0)
    }

    private fun onRightButtonClickListener(){
        onSuccessSetPin(mPIN)
    }

    private fun onSowPasswordClickListener() {
        isPasswordMode = !isPasswordMode
        pinEditText.transformationMethod = if (isPasswordMode) PasswordTransformationMethod() else null
        pinEyeImageView.background = if (isPasswordMode) ContextCompat.getDrawable(this@BasePinActivity, R.drawable.eye) else ContextCompat.getDrawable(this@BasePinActivity, R.drawable.eye_off)
    }
    private fun hiLightLeftButton(isHiLighted : Boolean){
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
            R.id.pin_left_button ->{
                onLeftButtonClickListener()
            }
        }
    }

}