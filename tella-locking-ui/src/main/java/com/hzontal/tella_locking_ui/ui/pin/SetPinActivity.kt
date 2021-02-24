package com.hzontal.tella_locking_ui.ui.pin

import android.content.Intent
import com.hzontal.tella_locking_ui.ui.pin.base.BasePinActivity

const val CONFIRM_PIN = "confirm_pin"
class SetPinActivity : BasePinActivity() {

    override fun onSuccessSetPin(pin: String?) {
        val intent = Intent(this, ConfirmPinActivity::class.java)
        intent.putExtra(CONFIRM_PIN,pin)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    override fun onFailureSetPin(pin: String) {
    }

}