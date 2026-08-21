package com.hzontal.tella_locking_ui.ui

import android.os.Bundle
import android.widget.Button
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.common.BaseActivity

class ConfirmCredentialsActivity : BaseActivity() {

    private lateinit var goToTellaButton : Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.confirm_unlocking_activity)
        initView()
    }

    private fun initView(){
        goToTellaButton = findViewById(R.id.finishUnlockingBtn)
        goToTellaButton.setOnClickListener {
            TellaKeysUI.getCredentialsCallback().onLockConfirmed(this@ConfirmCredentialsActivity)
            finish()
        }
    }

}