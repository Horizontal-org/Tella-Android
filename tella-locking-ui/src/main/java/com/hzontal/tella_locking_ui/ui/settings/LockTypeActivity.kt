package com.hzontal.tella_locking_ui.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.common.BaseActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternSetActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternUnlockActivity
import org.hzontal.tella.keys.config.UnlockRegistry

class LockTypeActivity : BaseActivity(), View.OnClickListener {
    private lateinit var patternBtn: Button
    private lateinit var passwordBtn: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_type)
        patternBtn = findViewById(R.id.patternBtn)
        passwordBtn = findViewById(R.id.passwordBtn)
        passwordBtn.setOnClickListener(this)
        patternBtn.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.fingerPrintBtn -> {
                TellaKeysUI.getUnlockRegistry().setActiveMethod(this, UnlockRegistry.Method.DEVICE_CREDENTIALS)
            }
            R.id.passwordBtn -> {
                startActivity(Intent(this, PatternUnlockActivity::class.java))
            }
            R.id.patternBtn -> {
                TellaKeysUI.getUnlockRegistry().setActiveMethod(this, UnlockRegistry.Method.TELLA_PATTERN)
                startActivity(Intent(this@LockTypeActivity, PatternSetActivity::class.java))
            }
        }
    }


}