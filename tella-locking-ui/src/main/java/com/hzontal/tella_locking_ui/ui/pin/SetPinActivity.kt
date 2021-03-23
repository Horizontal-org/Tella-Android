package com.hzontal.tella_locking_ui.ui.pin

import android.content.Intent
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.common.CommonStates
import com.hzontal.tella_locking_ui.ui.pin.base.BasePinActivity

const val CONFIRM_PIN = "confirm_pin"
class SetPinActivity : BasePinActivity() {

    override fun onSuccessSetPin(pin: String?) {
        val intent = Intent(this, ConfirmPinActivity::class.java)
        intent.putExtra(CONFIRM_PIN,pin)
        startActivity(intent)
        overridePendingTransition(R.anim.`in`,R.anim.out)
    }

    override fun onFailureSetPin(error: String) {
    }

    override fun onDestroy() {
        super.onDestroy()
        CommonStates.finishUpdateActivity.postValue(false)
    }

}