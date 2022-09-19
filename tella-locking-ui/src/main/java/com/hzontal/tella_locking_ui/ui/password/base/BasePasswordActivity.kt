package com.hzontal.tella_locking_ui.ui.password.base

import android.app.Activity
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.common.BaseActivity
import com.hzontal.tella_locking_ui.common.extensions.onChange

abstract class BasePasswordActivity : BaseActivity(), View.OnClickListener, OnValidatePasswordClickListener {

    private lateinit var passwordClickView: View
    lateinit var passwordEditText: EditText
    private lateinit var passwordEyeImageView: AppCompatImageView
    lateinit var enterPasswordTextView: TextView
    lateinit var topImageView: AppCompatImageView
    lateinit var passwordLeftButton: TextView
    lateinit var passwordMsgTextView: TextView
    lateinit var passwordRightButton: TextView
    private var isPasswordMode = true
    var isHiLighted = false
    var mPassword = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password)
        initView()
        initObservers()
        initListeners()
    }

    private fun initView() {
        passwordClickView = findViewById(R.id.passwordClickView)
        passwordEditText = findViewById(R.id.password_editText)
        passwordEyeImageView = findViewById(R.id.password_eye)
        topImageView = findViewById(R.id.password_TopImg)
        passwordLeftButton = findViewById(R.id.password_left_button)
        passwordRightButton = findViewById(R.id.password_right_button)
        enterPasswordTextView = findViewById(R.id.password_enterTV)
        passwordMsgTextView = findViewById(R.id.password_msgTV)
        passwordLeftButton.text = if(isFromSettings) getString(R.string.LockSelect_Action_Cancel) else getString(R.string.LockSelect_Action_Back)
    }

    private fun initListeners() {
        passwordClickView.setOnClickListener(this)
        passwordLeftButton.setOnClickListener(this)
        passwordRightButton.setOnClickListener(null)
    }

    private fun initObservers() {
        passwordEditText.onChange { password ->
            mPassword = password
            hiLightLeftButton(password.length > 5)
            passwordEditText.setTextColor(ContextCompat.getColor(this, R.color.wa_white))
        }
    }

    fun hideKeyboard() {
        val imm =
                getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(this)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun onRightButtonClickListener() {
        onSuccessSetPassword(mPassword)
    }

    private fun onLeftButtonClickListener() {
        finish()
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left)
    }

    private fun hiLightLeftButton(isHiLighted: Boolean) {
        this.isHiLighted = isHiLighted
        passwordRightButton.setTextColor(if (isHiLighted) ContextCompat.getColor(this, R.color.wa_white) else ContextCompat.getColor(this, R.color.wa_white_40))
        passwordRightButton.setOnClickListener(if (isHiLighted) this else null)
    }

    private fun onShowPasswordClickListener() {
        isPasswordMode = !isPasswordMode
        passwordEditText.transformationMethod = if (isPasswordMode) PasswordTransformationMethod() else null
        passwordEyeImageView.background = if (isPasswordMode) ContextCompat.getDrawable(this@BasePasswordActivity, R.drawable.eye) else ContextCompat.getDrawable(this@BasePasswordActivity, R.drawable.eye_off)
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            (R.id.password_right_button) -> {
                onRightButtonClickListener()
            }
            (R.id.password_left_button) -> {
                onLeftButtonClickListener()
            }
            (R.id.passwordClickView) -> {
                onShowPasswordClickListener()
            }

        }
    }

    fun setTopText(text: String) {
        enterPasswordTextView.text = text
    }

    fun setMessageText(text: String) {
        passwordMsgTextView.text = text
    }
}