package com.hzontal.tella_locking_ui.ui.pin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.hzontal.tella_locking_ui.FINISH_ACTIVITY_REQUEST_CODE
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.ui.pin.base.BasePinActivity

const val CONFIRM_PIN = "confirm_pin"
class SetPinActivity : BasePinActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinMsgText.text = pinHintAsBullets(getString(R.string.LockPinSet_Message_Hint))
    }

    override fun onSuccessSetPin(pin: String?) {
        val intent = Intent(this, ConfirmPinActivity::class.java)
        intent.putExtra(CONFIRM_PIN, pin)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        launchConfirmActivity(intent)
    }

    override fun onFailureSetPin(error: String) {
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FINISH_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) finish()
    }

    private fun pinHintAsBullets(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.contains('•')) return trimmed
        val parts = trimmed
            .split(Regex("(?<=[.。])\\s+"), limit = 2)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return if (parts.size >= 2) {
            "• ${parts[0]}\n• ${parts[1]}"
        } else {
            trimmed
        }
    }
}