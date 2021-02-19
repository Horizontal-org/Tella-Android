package com.hzontal.tella_locking_ui.ui.password

import android.content.Intent
import com.hzontal.tella_locking_ui.ui.password.base.BasePasswordActivity
import com.hzontal.tella_locking_ui.ui.pin.ConfirmPinActivity

internal const val CONFIRM_PASSWORD = "confirm_password"

class SetPasswordActivity : BasePasswordActivity() {

    override fun onSuccessSetPassword(password: String) {
        val intent = Intent(this, ConfirmPinActivity::class.java)
        intent.putExtra(CONFIRM_PASSWORD, password)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    override fun onFailureSetPassword(error: String) {

    }
}