package com.hzontal.tella_locking_ui.ui.password

import android.app.Activity
import android.content.Intent
import com.hzontal.tella_locking_ui.FINISH_ACTIVITY_REQUEST_CODE
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.ui.password.base.BasePasswordActivity

internal const val CONFIRM_PASSWORD = "confirm_password"

class SetPasswordActivity : BasePasswordActivity() {

    override fun onSuccessSetPassword(password: String) {
        val intent = Intent(this, ConfirmPasswordActivity::class.java)
        intent.putExtra(CONFIRM_PASSWORD, password)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivityForResult(intent, FINISH_ACTIVITY_REQUEST_CODE)
        overridePendingTransition(R.anim.`in`, R.anim.out)
    }

    override fun onFailureSetPassword(error: String) {}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FINISH_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) finish()
    }
}