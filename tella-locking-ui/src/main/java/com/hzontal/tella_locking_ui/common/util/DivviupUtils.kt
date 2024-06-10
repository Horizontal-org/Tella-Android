package com.hzontal.tella_locking_ui.common.util

import android.content.Context
import com.hzontal.tella_locking_ui.R
import org.divviup.android.Client
import org.divviup.android.TaskId
import org.hzontal.shared_ui.data.CommonPreferences
import timber.log.Timber
import java.net.URI
import java.util.concurrent.Executors

object DivviupUtils {

    fun runUnlockEvent(context: Context) {
        if (!CommonPreferences.hasAcceptedAnalytics()) return
        Executors.newSingleThreadExecutor().execute {
            try {
                val taskId = TaskId.parse(context.getString(R.string.divviup_count_unlocks_id))
                val leaderEndpoint: URI = URI.create(context.getString(R.string.divviup_count_unlocks_leader))
                val helperEndpoint: URI = URI.create(context.getString(R.string.divviup_count_unlocks_helper))
                val timePrecisionSeconds: Long = context.resources.getInteger(R.integer.divviup_count_unlocks_timePrecisionSeconds).toLong()
                val client = Client.createPrio3Count(
                    context, leaderEndpoint, helperEndpoint, taskId, timePrecisionSeconds
                )
                client.sendMeasurement(true)
                Timber.i("Measurement sent to divviup")
            } catch (e: Exception) {
                Timber.e(e, "Sending runUnlockEvent failed")
            }
        }

    }

}