package com.hzontal.tella_locking_ui.ui.password

import android.content.Intent
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.common.CommonStates
import com.hzontal.tella_locking_ui.ui.password.base.BasePasswordActivity

internal const val CONFIRM_PASSWORD = "confirm_password"

class SetPasswordActivity : BasePasswordActivity() {

    override fun onSuccessSetPassword(password: String) {
        val intent = Intent(this, ConfirmPasswordActivity::class.java)
        intent.putExtra(CONFIRM_PASSWORD, password)
        startActivity(intent)
        overridePendingTransition(R.anim.`in`,R.anim.out)
    }

    override fun onFailureSetPassword(error: String) {

    }

    override fun onDestroy() {
        super.onDestroy()
        CommonStates.finishUpdateActivity.postValue(false)
    }
}