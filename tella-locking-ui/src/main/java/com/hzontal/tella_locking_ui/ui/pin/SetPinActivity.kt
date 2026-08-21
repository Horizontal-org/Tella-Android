package com.hzontal.tella_locking_ui.ui.pin

import android.app.Activity
import android.content.Intent
import com.hzontal.tella_locking_ui.FINISH_ACTIVITY_REQUEST_CODE
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.ui.pin.base.BasePinActivity

const val CONFIRM_PIN = "confirm_pin"
class SetPinActivity : BasePinActivity() {

    override fun onSuccessSetPin(pin: String?) {
        val intent = Intent(this, ConfirmPinActivity::class.java)
        intent.putExtra(CONFIRM_PIN, pin)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra(IS_FROM_SETTINGS, isFromSettings)
        startActivityForResult(intent, FINISH_ACTIVITY_REQUEST_CODE)
        overridePendingTransition(R.anim.`in`, R.anim.out)
    }

    override fun onFailureSetPin(error: String) {
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FINISH_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) finish()
    }
}