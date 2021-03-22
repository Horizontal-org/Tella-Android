package com.hzontal.tella_locking_ui.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.common.CommonStates
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SuccessUpdateDialog : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CommonStates.finishUpdateActivity.postValue(true)
        finishActivity()
        setContentView(R.layout.dialog_update_lock_success)
    }

    private fun finishActivity(){
        val executor = Executors.newSingleThreadScheduledExecutor()
        val hideDialog = Runnable {
            CommonStates.finishUpdateActivity.postValue(false)
            finish()
        }
        executor.schedule(hideDialog, 3, TimeUnit.SECONDS);
    }
}