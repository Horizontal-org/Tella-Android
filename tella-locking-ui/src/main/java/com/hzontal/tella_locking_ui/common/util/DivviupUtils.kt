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
        if (context.getString(R.string.divviup_count_unlocks_id).isEmpty() || context.getString(
                R.string.divviup_leader
            ).isEmpty() || context.getString(R.string.divviup_helper).isEmpty()
        ) return
        Executors.newSingleThreadExecutor().execute {
            try {
                val taskId = TaskId.parse(context.getString(R.string.divviup_count_unlocks_id))
                val leaderEndpoint: URI = URI.create(context.getString(R.string.divviup_leader))
                val helperEndpoint: URI = URI.create(context.getString(R.string.divviup_helper))
                val timePrecisionSeconds: Long =
                    context.resources.getInteger(R.integer.divviup_count_unlocks_timePrecisionSeconds)
                        .toLong()
                val client = Client.createPrio3Count(
                    context, leaderEndpoint, helperEndpoint, taskId, timePrecisionSeconds
                )
                client.sendMeasurement(true)
                Timber.d("Divviup runUnlockEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runUnlockEvent failed")
            }
        }

    }

}