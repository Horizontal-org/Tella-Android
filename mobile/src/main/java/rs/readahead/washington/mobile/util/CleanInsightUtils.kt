package rs.readahead.washington.mobile.util

import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import timber.log.Timber
import java.util.*

object CleanInsightUtils {

    enum class ServerType { SERVER_TELLA, SERVER_UWAZI, SERVER_COLLECT }

    private const val CAMPAIGN_ID = "test"
    private const val CATEGORY = "app-state"
    private const val YES = "Yes"
    private const val NO = "No"
    private val cleanInsights by lazy { MyApplication.getCleanInsights() }

    fun measureEvent() {
        if (!Preferences.hasAcceptedImprovements()) return
        try {
            cleanInsights?.let {
                val isCamouflageEnabled = if (!CamouflageManager.getInstance().isDefaultLauncherActivityAlias) YES else NO
                val isQuickExit = if (Preferences.isQuickExit()) YES else NO
                it.measureEvent(CATEGORY, "Opening the app", CAMPAIGN_ID)
                it.measureEvent(CATEGORY, "Quick delete", CAMPAIGN_ID, isQuickExit)
                it.measureEvent(CATEGORY, "Camouflage enabled", CAMPAIGN_ID, isCamouflageEnabled)
                it.measureEvent(CATEGORY, "Language", CAMPAIGN_ID, LocaleManager.getInstance().languageSetting ?: "EN")
                it.persistAndSend()
                Timber.e("measureEvent")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun measureEvent(serverType: ServerType) {
        if (!Preferences.hasAcceptedImprovements()) return
        try {
            cleanInsights?.let {
                it.measureEvent(CATEGORY, "Connected server", CAMPAIGN_ID, serverType.name)
                it.persistAndSend()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun measureTimeSpentEvent(start: Long) {
        if (!Preferences.hasAcceptedImprovements()) return
        try {
            cleanInsights?.let {
                val timeSpend = (System.currentTimeMillis() - start) / 1000
                it.measureEvent(CATEGORY, "Time spent on the app", CAMPAIGN_ID, timeSpend.toString())
                it.persistAndSend()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun measureVisit() {
        if (!Preferences.hasAcceptedImprovements()) return
        try {
            cleanInsights?.let {
                it.measureVisit(Collections.singletonList("Main"), CAMPAIGN_ID)
                it.persistAndSend()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun grantCampaign(isChecked: Boolean) {
        if (isChecked) MyApplication.getCleanInsights()?.grant(CAMPAIGN_ID)
        else MyApplication.getCleanInsights()?.deny(CAMPAIGN_ID)
    }
}