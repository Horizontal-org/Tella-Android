package rs.readahead.washington.mobile.util.divviup

import android.content.Context
import com.hzontal.tella_locking_ui.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.divviup.android.Client
import org.divviup.android.TaskId
import org.hzontal.shared_ui.data.CommonPreferences
import timber.log.Timber
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DivviupUtils @Inject constructor(private val context: Context) {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun runUwaziSentEvent() {
        if (!CommonPreferences.hasAcceptedAnalytics() || isDebugBuild()) return
        ioScope.launch {
            try {
                sendCountMeasurement(
                    taskId = R.string.divviup_count_uwazi_sent_id,
                    leader = R.string.divviup_leader,
                    helper = R.string.divviup_helper,
                    timePrecision = R.integer.divviup_count_unlocks_timePrecisionSeconds,
                    isCount = true
                )
                Timber.d("Divviup runUwaziSentEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runUwaziSentEvent failed")
            }
        }
    }


    fun runReportSentEvent() {
        if (!CommonPreferences.hasAcceptedAnalytics() || isDebugBuild()) return
        ioScope.launch {
            try {
                sendCountMeasurement(
                    taskId = R.string.divviup_count_report_sent_id,
                    leader = R.string.divviup_leader,
                    helper = R.string.divviup_helper,
                    timePrecision = R.integer.divviup_count_unlocks_timePrecisionSeconds,
                    isCount = true
                )
                Timber.d("Divviup runReportSentEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runReportSentEvent failed")
            }
        }
    }


    fun runQuickDeleteEvent() {
        if (!CommonPreferences.hasAcceptedAnalytics() || isDebugBuild()) return
        ioScope.launch {
            try {
                sendCountMeasurement(
                    taskId = R.string.divviup_count_quick_delete_id,
                    leader = R.string.divviup_leader,
                    helper = R.string.divviup_helper,
                    timePrecision = R.integer.divviup_count_unlocks_timePrecisionSeconds,
                    isCount = true
                )
                Timber.d("Divviup runQuickDeleteEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runQuickDeleteEvent failed")
            }
        }
    }

    fun runUnlockEvent() {
        if (!CommonPreferences.hasAcceptedAnalytics() || isDebugBuild()) return
        ioScope.launch {
            try {
                sendCountMeasurement(
                    taskId = R.string.divviup_count_unlocks_id,
                    leader = R.string.divviup_leader,
                    helper = R.string.divviup_helper,
                    timePrecision = R.integer.divviup_count_unlocks_timePrecisionSeconds,
                    isCount = true
                )
                Timber.d("Divviup runUnlockEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runUnlockEvent failed")
            }
        }
    }

    fun runInstallEvent() {
        if (!CommonPreferences.hasAcceptedAnalytics() || CommonPreferences.isInstallMetricSent() || isDebugBuild()) return
        ioScope.launch {
            try {
                sendCountMeasurement(
                    taskId = R.string.divviup_count_installs_id,
                    leader = R.string.divviup_leader,
                    helper = R.string.divviup_helper,
                    timePrecision = R.integer.divviup_count_unlocks_timePrecisionSeconds,
                    isCount = true
                )
                CommonPreferences.setInstallMetricSent(true)
                Timber.d("Divviup runInstallEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runInstallEvent failed")
            }
        }
    }

    fun runTimeSpentEvent(milliseconds: Long) {
        ioScope.launch {
            try {
                sendSumMeasurement(
                    taskId = R.string.divviup_time_spent_id,
                    leader = R.string.divviup_leader,
                    helper = R.string.divviup_helper,
                    timePrecision = R.integer.divviup_count_unlocks_timePrecisionSeconds,
                    sumValue = milliseconds
                )
                Timber.d("Divviup runTimeSpentEvent measurement sent: %s", milliseconds)
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runTimeSpentEvent failed")
            }
        }
    }

    fun runPhotoTakenEvent() {
        if (!CommonPreferences.hasAcceptedAnalytics() || isDebugBuild()) return
        ioScope.launch {
            try {
                sendCountMeasurement(
                    taskId = R.string.divviup_count_photos_id,
                    leader = R.string.divviup_leader,
                    helper = R.string.divviup_helper,
                    timePrecision = R.integer.divviup_count_unlocks_timePrecisionSeconds,
                    isCount = true
                )
                Timber.d("Divviup runPhotoTakenEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runPhotoTakenEvent failed")
            }
        }
    }

    fun runVideoTakenEvent() {
        if (!CommonPreferences.hasAcceptedAnalytics() || isDebugBuild()) return
        ioScope.launch {
            try {
                sendCountMeasurement(
                    taskId = R.string.divviup_count_videos_id,
                    leader = R.string.divviup_leader,
                    helper = R.string.divviup_helper,
                    timePrecision = R.integer.divviup_count_unlocks_timePrecisionSeconds,
                    isCount = true
                )
                Timber.d("Divviup runVideoTakenEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runVideoTakenEvent failed")
            }
        }
    }


    fun runAudioTakenEvent() {
        if (!CommonPreferences.hasAcceptedAnalytics() || isDebugBuild()) return
        ioScope.launch {
            try {
                sendCountMeasurement(
                    taskId = R.string.divviup_count_audios_id,
                    leader = R.string.divviup_leader,
                    helper = R.string.divviup_helper,
                    timePrecision = R.integer.divviup_count_unlocks_timePrecisionSeconds,
                    isCount = true
                )
                Timber.d("Divviup runAudioTakenEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runAudioTakenEvent failed")
            }
        }
    }


    fun runODKSentEvent() {
        if (!CommonPreferences.hasAcceptedAnalytics() || isDebugBuild()) return
        ioScope.launch {
            try {
                sendCountMeasurement(
                    taskId = R.string.divviup_count_odk_sent_id,
                    leader = R.string.divviup_leader,
                    helper = R.string.divviup_helper,
                    timePrecision = R.integer.divviup_count_unlocks_timePrecisionSeconds,
                    isCount = true
                )
                Timber.d("Divviup runODKSentEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runODKSentEvent failed")
            }
        }
    }

    fun runFileImportEvent() {
        if (!CommonPreferences.hasAcceptedAnalytics() || isDebugBuild()) return
        ioScope.launch {
            try {
                sendCountMeasurement(
                    taskId = R.string.divviup_count_imports_id,
                    leader = R.string.divviup_leader,
                    helper = R.string.divviup_helper,
                    timePrecision = R.integer.divviup_count_unlocks_timePrecisionSeconds,
                    isCount = true
                )
                Timber.d("Divviup runFileImportEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runFileImportEvent failed")
            }
        }
    }


    fun runCamouflageEnabledEvent() {
        if (!CommonPreferences.hasAcceptedAnalytics() || isDebugBuild()) return
        ioScope.launch {
            try {
                sendCountMeasurement(
                    taskId = R.string.divviup_count_camouflage_id,
                    leader = R.string.divviup_leader,
                    helper = R.string.divviup_helper,
                    timePrecision = R.integer.divviup_count_unlocks_timePrecisionSeconds,
                    isCount = true
                )
                Timber.d("Divviup runCamouflageEnabledEvent measurement sent")
            } catch (e: Exception) {
                Timber.e(e, "Divviup sending runCamouflageEnabledEvent failed")
            }
        }
    }


    private suspend fun sendCountMeasurement(
        taskId: Int,
        leader: Int,
        helper: Int,
        timePrecision: Int,
        isCount: Boolean
    ) {
        withContext(Dispatchers.IO) {
            val taskIdValue = TaskId.parse(context.getString(taskId))
            val leaderEndpoint = URI.create(context.getString(leader))
            val helperEndpoint = URI.create(context.getString(helper))
            val timePrecisionSeconds = context.resources.getInteger(timePrecision).toLong()
            val client = Client.createPrio3Count(
                context, leaderEndpoint, helperEndpoint, taskIdValue, timePrecisionSeconds
            )
            client.sendMeasurement(isCount)
        }
    }

    private suspend fun sendSumMeasurement(
        taskId: Int,
        leader: Int,
        helper: Int,
        timePrecision: Int,
        sumValue: Long
    ) {
        withContext(Dispatchers.IO) {
            val taskIdValue = TaskId.parse(context.getString(taskId))
            val leaderEndpoint = URI.create(context.getString(leader))
            val helperEndpoint = URI.create(context.getString(helper))
            val timePrecisionSeconds = context.resources.getInteger(timePrecision).toLong()
            val client = Client.createPrio3Sum(
                context, leaderEndpoint, helperEndpoint, taskIdValue, timePrecisionSeconds, 32
            )
            client.sendMeasurement(sumValue)
        }
    }

    private fun isDebugBuild(): Boolean {
        return context.getString(R.string.divviup_leader)
            .isEmpty() || context.getString(R.string.divviup_helper).isEmpty()
    }
}
