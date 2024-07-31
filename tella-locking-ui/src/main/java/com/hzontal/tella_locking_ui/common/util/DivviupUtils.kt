package com.hzontal.tella_locking_ui.common.util

import android.content.Context
import com.hzontal.tella_locking_ui.R
import org.divviup.android.Client
import org.divviup.android.TaskId
import org.hzontal.shared_ui.data.CommonPreferences
import org.hzontal.tella.keys.util.Preferences
import timber.log.Timber
import java.net.URI
import java.util.concurrent.Executors

object DivviupUtils {

    fun runUnlockEvent(context: Context) {
        if (!CommonPreferences.hasAcceptedAnalytics() || isDebugBuild(context)) return
        val lastTimeSpent = Preferences.loadLong(
            context,
            Preferences.CLEAR_KEY_MS,
            0L
        ) - CommonPreferences.getUnlockTime()
        if (lastTimeSpent > 0) {
            runTimeSpentEvent(context, lastTimeSpent)
        }
        CommonPreferences.setUnlockTime(System.currentTimeMillis())

        Executors.newSingleThreadExecutor().execute {
            try {
                val taskId = TaskId.parse(context.getString(R.string.divviup_count_unlocks_id))
                val leaderEndpoint: URI =
                    URI.create(context.getString(R.string.divviup_leader))
                val helperEndpoint: URI =
                    URI.create(context.getString(R.string.divviup_helper))
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

    /**
     * This should be called only once when user enables sending metrics
     * Which means that the installation is done and the sending of analytics is approved
     **/
    fun runInstallEvent(context: Context) {
        // If sending analytics is not accepted or install metric is sent, just return
        if (!CommonPreferences.hasAcceptedAnalytics() || CommonPreferences.isInstallMetricSent() || isDebugBuild(context)) return
        Executors.newSingleThreadExecutor().execute {
            try {
                val taskId = TaskId.parse(context.getString(R.string.divviup_count_installs_id))
                val leaderEndpoint: URI =
                    URI.create(context.getString(R.string.divviup_leader))
                val helperEndpoint: URI =
                    URI.create(context.getString(R.string.divviup_helper))
                val timePrecisionSeconds: Long =
                    context.resources.getInteger(R.integer.divviup_count_unlocks_timePrecisionSeconds)
                        .toLong()
                val client = Client.createPrio3Count(
                    context, leaderEndpoint, helperEndpoint, taskId, timePrecisionSeconds
                )
                client.sendMeasurement(true)
                CommonPreferences.setInstallMetricSent(true)
                Timber.d("Divviup runInstallEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runInstallEvent failed")
            }
        }
    }

    /**
     * This task is called on Lock
     * and sending time spent since unlock
     **/
    fun runTimeSpentEvent(context: Context, milliseconds: Long) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val taskId = TaskId.parse(context.getString(R.string.divviup_time_spent_id))
                val leaderEndpoint: URI =
                    URI.create(context.getString(R.string.divviup_leader))
                val helperEndpoint: URI =
                    URI.create(context.getString(R.string.divviup_helper))
                val timePrecisionSeconds: Long =
                    context.resources.getInteger(R.integer.divviup_count_unlocks_timePrecisionSeconds)
                        .toLong()
                val client = Client.createPrio3Sum(
                    context, leaderEndpoint, helperEndpoint, taskId, timePrecisionSeconds, 32
                )
                client.sendMeasurement(milliseconds)
                Timber.d("Divviup runTimeSpentEvent measurement sent %s", milliseconds)
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runTimeSpentEvent failed")
            }
        }
    }

    /**
     * This task is called when photo is taken
     * using Camera
     **/
    fun runPhotoTakenEvent(context: Context) {
        if (!CommonPreferences.hasAcceptedAnalytics() || isDebugBuild(context)) return
        Executors.newSingleThreadExecutor().execute {
            try {
                val taskId = TaskId.parse(context.getString(R.string.divviup_count_photos_id))
                val leaderEndpoint: URI =
                    URI.create(context.getString(R.string.divviup_leader))
                val helperEndpoint: URI =
                    URI.create(context.getString(R.string.divviup_helper))
                val timePrecisionSeconds: Long =
                    context.resources.getInteger(R.integer.divviup_count_unlocks_timePrecisionSeconds)
                        .toLong()
                val client = Client.createPrio3Count(
                    context, leaderEndpoint, helperEndpoint, taskId, timePrecisionSeconds
                )
                client.sendMeasurement(true)
                Timber.d("Divviup runPhotoTakenEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runPhotoTakenEvent failed")
            }
        }
    }

    /**
     * This task is called when video is taken
     * using Camera
     **/
    fun runVideoTakenEvent(context: Context) {
        if (!CommonPreferences.hasAcceptedAnalytics() || isDebugBuild(context)) return
        Executors.newSingleThreadExecutor().execute {
            try {
                val taskId = TaskId.parse(context.getString(R.string.divviup_count_videos_id))
                val leaderEndpoint: URI =
                    URI.create(context.getString(R.string.divviup_leader))
                val helperEndpoint: URI =
                    URI.create(context.getString(R.string.divviup_helper))
                val timePrecisionSeconds: Long =
                    context.resources.getInteger(R.integer.divviup_count_unlocks_timePrecisionSeconds)
                        .toLong()
                val client = Client.createPrio3Count(
                    context, leaderEndpoint, helperEndpoint, taskId, timePrecisionSeconds
                )
                client.sendMeasurement(true)
                Timber.d("Divviup runVideoTakenEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runVideoTakenEvent failed")
            }
        }
    }

    /**
     * This task is called when audio is taken
     * using Audio Recorder
     **/
    fun runAudioTakenEvent(context: Context) {
        if (!CommonPreferences.hasAcceptedAnalytics() || isDebugBuild(context)) return
        Executors.newSingleThreadExecutor().execute {
            try {
                val taskId = TaskId.parse(context.getString(R.string.divviup_count_audios_id))
                val leaderEndpoint: URI =
                    URI.create(context.getString(R.string.divviup_leader))
                val helperEndpoint: URI =
                    URI.create(context.getString(R.string.divviup_helper))
                val timePrecisionSeconds: Long =
                    context.resources.getInteger(R.integer.divviup_count_unlocks_timePrecisionSeconds)
                        .toLong()
                val client = Client.createPrio3Count(
                    context, leaderEndpoint, helperEndpoint, taskId, timePrecisionSeconds
                )
                client.sendMeasurement(true)
                Timber.d("Divviup runAudioTakenEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runAudioTakenEvent failed")
            }
        }
    }

    /**
     * This task is called when file is imported
     **/
    fun runFileImportEvent(context: Context) {
        if (!CommonPreferences.hasAcceptedAnalytics() || isDebugBuild(context)) return
        Executors.newSingleThreadExecutor().execute {
            try {
                val taskId = TaskId.parse(context.getString(R.string.divviup_count_imports_id))
                val leaderEndpoint: URI =
                    URI.create(context.getString(R.string.divviup_leader))
                val helperEndpoint: URI =
                    URI.create(context.getString(R.string.divviup_helper))
                val timePrecisionSeconds: Long =
                    context.resources.getInteger(R.integer.divviup_count_unlocks_timePrecisionSeconds)
                        .toLong()
                val client = Client.createPrio3Count(
                    context, leaderEndpoint, helperEndpoint, taskId, timePrecisionSeconds
                )
                client.sendMeasurement(true)
                Timber.d("Divviup runFileImportEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runFileImportEvent failed")
            }
        }
    }

    fun isDebugBuild(context: Context): Boolean {
        return context.getString(R.string.divviup_leader).isEmpty() || context.getString(R.string.divviup_helper).isEmpty()
    }

}