package rs.readahead.washington.mobile.util

import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import java.util.*

object CleanInsightUtils {

    private const val CAMPAIGN_ID = "test"
    private const val CATEGORY = "app-state"
    private val cleanInsights by lazy { MyApplication.getCleanInsights() }

    fun measureEvent() {
        if (Preferences.hasAcceptedImprovements()) return
        try {
            cleanInsights?.let {
                val isCamouflageEnabled = !CamouflageManager.getInstance().isDefaultLauncherActivityAlias
                it.measureEvent(CATEGORY, "Opening the app", CAMPAIGN_ID)
                it.measureEvent(CATEGORY, "Servers connected to Tella", CAMPAIGN_ID)
                it.measureEvent(CATEGORY, "Quick delete", CAMPAIGN_ID, Preferences.isQuickExit().toString())
                it.measureEvent(CATEGORY, "Camouflage enabled", CAMPAIGN_ID, isCamouflageEnabled.toString())
                it.measureEvent(CATEGORY, "Language", CAMPAIGN_ID, LocaleManager.getInstance().languageSetting)
                it.persist()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun measureTimeSpentEvent(start: Long) {
        try {
            cleanInsights?.let {
                val timeSpend = (System.currentTimeMillis() - start) / 1000
                it.measureEvent(CATEGORY, "Time spent on the app", CAMPAIGN_ID, timeSpend.toString())
                it.persist()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun measureVisit() {
        if (Preferences.hasAcceptedImprovements()) return
        try {
            cleanInsights?.let {
                it.measureVisit(Collections.singletonList("Main"), "Opening the app")
                it.persist()
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