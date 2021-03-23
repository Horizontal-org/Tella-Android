package com.hzontal.tella_locking_ui.ui

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hzontal.tella_locking_ui.FINISH_ACTIVITY_REQUEST_CODE
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.common.CommonStates
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SuccessUpdateDialog : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.`in`, R.anim.out)
        finishActivity()
        setContentView(R.layout.dialog_update_lock_success)
    }

    private fun finishActivity(){
        finishActivity(FINISH_ACTIVITY_REQUEST_CODE)
        val executor = Executors.newSingleThreadScheduledExecutor()
        val hideDialog = Runnable {
            CommonStates.finishUpdateActivity.postValue(true)
            setResult(Activity.RESULT_OK)
            finish()
        }
        executor.schedule(hideDialog, 3, TimeUnit.SECONDS);
    }
}