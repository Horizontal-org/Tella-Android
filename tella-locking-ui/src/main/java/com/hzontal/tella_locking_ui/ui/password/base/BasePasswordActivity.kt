package com.hzontal.tella_locking_ui.ui.password.base

import android.graphics.Rect
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.common.BaseActivity
import com.hzontal.tella_locking_ui.common.extensions.onChange

abstract class BasePasswordActivity   : BaseActivity() , View.OnClickListener, OnValidatePasswordClickListener {

    private lateinit var passwordClickView : View
    lateinit var passwordEditText: EditText
    private lateinit var passwordEyeImageView: AppCompatImageView
    lateinit var enterPasswordTextView : TextView
    lateinit var topImageView: AppCompatImageView
    lateinit var passwordLeftButton: TextView
    lateinit var passwordMsgTextView: TextView
    private lateinit var passwordRightButton: TextView
    private val keyboardStatus = MutableLiveData<Pair<Boolean, Double>>()
    private val container: ViewGroup by lazy {
        findViewById<ViewGroup>(android.R.id.content)
    }
    private var isPasswordMode = true
    private lateinit var guideBottom : Guideline
    private lateinit var guideTop : Guideline
    private var mPassword = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password)
        initView()
        initObservers()
        initListeners()
        toggleKeyBoard()
    }

    private fun initView(){
        guideBottom = findViewById(R.id.guide_bottom)
        guideTop = findViewById(R.id.guideHTop)
        passwordClickView = findViewById(R.id.passwordClickView)
        passwordEditText = findViewById(R.id.password_editText)
        passwordEyeImageView = findViewById(R.id.password_eye)
        topImageView = findViewById(R.id.password_TopImg)
        passwordLeftButton = findViewById(R.id.password_left_button)
        passwordRightButton = findViewById(R.id.password_right_button)
        enterPasswordTextView = findViewById(R.id.password_enterTV)
        passwordMsgTextView = findViewById(R.id.password_msgTV)
    }

    private fun initListeners(){
        passwordClickView.setOnClickListener(this)
        passwordLeftButton.setOnClickListener(this)
        passwordRightButton.setOnClickListener(null)
    }

    private fun initObservers(){
        keyboardStatus.observe(this, Observer {
            keyBoardState(it.first, it.second)
        })
        passwordEditText.onChange { password ->
             mPassword = password
             hiLightLeftButton(password.length>5)
            passwordEditText.setTextColor(ContextCompat.getColor(this,R.color.wa_white))
        }
    }

    private fun keyBoardState(isOpened: Boolean, keyboardHeight: Double) {
        guideBottom.setGuidelinePercent((1 - keyboardHeight).toFloat())
        guideTop.setGuidelinePercent( if (isOpened) 0.05F else 0.25F)
    }

    private fun toggleKeyBoard(){
        container.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            container.getWindowVisibleDisplayFrame(r)
            val screenHeight = container.rootView.height
            val keypadHeight = screenHeight - r.bottom
            if (keypadHeight > screenHeight * 0.15) {
                //keyboard Open
                keyboardStatus.postValue(
                        Pair(
                                true,
                                (keypadHeight.toDouble() / (screenHeight * 1.1))
                        )
                )
            } else {
                //keyboard closed
                keyboardStatus.postValue(Pair(false, 0.0))

            }
        }
    }
    private fun onRightButtonClickListener(){
        onSuccessSetPassword(mPassword)
    }
    private fun onLeftButtonClickListener(){
        finish()
        overridePendingTransition(0, 0)
    }
    private fun hiLightLeftButton(isHiLighted : Boolean){
        passwordRightButton.setTextColor(if (isHiLighted) ContextCompat.getColor(this, R.color.wa_white) else ContextCompat.getColor(this, R.color.wa_white_40))
        passwordRightButton.setOnClickListener{if (isHiLighted) this else null }
    }

    private fun onShowPasswordClickListener() {
        isPasswordMode = !isPasswordMode
        passwordEditText.transformationMethod = if (isPasswordMode) PasswordTransformationMethod() else null
        passwordEyeImageView.background = if (isPasswordMode) ContextCompat.getDrawable(this@BasePasswordActivity, R.drawable.eye) else ContextCompat.getDrawable(this@BasePasswordActivity, R.drawable.eye_off)
    }

    override fun onClick(view: View?) {
        when(view?.id){
            (R.id.password_right_button) -> {onRightButtonClickListener()}
            (R.id.password_left_button) ->{onLeftButtonClickListener()}
            (R.id.passwordClickView) -> {onShowPasswordClickListener()}

        }
    }
}