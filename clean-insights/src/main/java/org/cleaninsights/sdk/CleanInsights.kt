/**
 * CleanInsights.kt
 * CleanInsightsSDK
 *
 * Created by Benjamin Erhart.
 * Copyright © 2020 Guardian Project. All rights reserved.
 */
package org.cleaninsights.sdk

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow

/**
 * @param cleanInsightsConfiguration: The Configuration provided as a `Configuration` object.
 * @param store: Your implementation of a `Store`.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
open class CleanInsights(val cleanInsightsConfiguration: CleanInsightsConfiguration, private val store: Store) {

    companion object {
        private fun deserialize(jsonConfiguration: String): CleanInsightsConfiguration {
            val conf = Gson().fromJson(jsonConfiguration, CleanInsightsConfiguration::class.java)

            if (conf != null) {
                return conf
            } else {
                throw IOException("Configuration file could not be read!")
            }
        }

        private fun debug(toggle: Boolean, format: String, vararg args: Any?) {
            if (!toggle) return

            Log.d("CleanInsightsSDK", String.format(Locale.US, format, *args))
        }
    }

    var conf = cleanInsightsConfiguration
        private set

    private var persistenceCounter = 0

    private var failedSubmissionCount = 0
    private var lastFailedSubmission = Date(0)

    /**
     * @param cleanInsightsConfiguration: The Configuration provided as a `Configuration` object.
     * @param storageDir: The location where to read and persist accumulated data.
     */
    constructor(cleanInsightsConfiguration: CleanInsightsConfiguration, storageDir: File) :
            this(
                cleanInsightsConfiguration, DefaultStore(
                    hashMapOf("storageDir" to storageDir),
                    fun(message: String) { debug(cleanInsightsConfiguration.debug, message) })
            )

    /**
     * @param jsonConfiguration: The Configuration provided as a JSON string
     *      which can be deserialized to a `Configuration` object.
     * @param store: Your implementation of a `Store`.
     */
    constructor(jsonConfiguration: String, store: Store) : this(deserialize(jsonConfiguration), store)

    /**
     * @param jsonConfiguration: The Configuration provided as a JSON string
     *      which can be deserialized to a `Configuration` object.
     * @param storageDir: The location where to read and persist accumulated data.
     */
    constructor(jsonConfiguration: String, storageDir: File) : this(deserialize(jsonConfiguration), storageDir)


    /**
     * @param jsonConfigurationFile: The Configuration provided as a URL to a JSON file
     *      which can be deserialized to a `Configuration` object.
     * @param storageDir: The location where to read and persist accumulated data.
     */
    constructor(jsonConfigurationFile: File, storageDir: File) : this(jsonConfigurationFile.readText(), storageDir)

    init {
        if (!conf.check { debug("%s", it) }) {
            throw RuntimeException("Invalid configuration provided.")
        }
    }

    protected fun finalize() {
        // Don't rely on this! When used as a Singleton (which would be typical),
        // This will never be called. Instead, make the app call
        // #persist explicitly on your most important Activity's `#onStop` or on
        // `Application#onTrimMemory`, which has a good indicator of how likely it is, the app
        // will soon be terminated.
        persist()
    }

    /**
     * Track a scene visit.
     *
     * @param scenePath: A hierarchical path best describing the structure of your scenes.
     *      E.g. `['Main', 'Settings', 'Some Setting']`.
     * @param campaignId: The campaign ID as per your configuration, where this measurement belongs to.
     */
    fun measureVisit(scenePath: List<String>, campaignId: String) {
        val campaign = getCampaignIfGood(campaignId, scenePath.joinToString("/"))

        if (campaign != null) {
            var visit =
                getAndMeasure(store.visits, campaignId, campaign) { it.scenePath == scenePath }

            if (visit != null) {
                debug("Gain visit insight: %s", visit)
            } else {
                // Align first and last timestamps with campaign measurement period,
                // in order not to accidentally leak more information than promised.
                val period = campaign.currentMeasurementPeriod

                if (period != null) {
                    visit = Visit(scenePath, campaignId, period)
                    store.visits.add(visit)

                    debug("Gain visit insight: %s", visit)
                } else {
                    debug("measureVisit campaign.currentMeasurementPeriod == null! This should not happen!")
                }
            }
        }
    }

    /**
     * Track an event.
     *
     * @param category: The event category. Must not be empty. (eg. Videos, Music, Games...)
     * @param action: The event action. Must not be empty. (eg. Play, Pause, Duration, Add Playlist, Downloaded, Clicked...)
     * @param campaignId: The campaign ID as per your configuration, where this measurement belongs to.
     * @param name: The event name. OPTIONAL.
     * @param value: The event value. OPTIONAL.
     */
    @JvmOverloads
    fun measureEvent(category: String, action: String, campaignId: String, name: String? = null, value: Double? = null) {
        val campaign = getCampaignIfGood(campaignId, "$category/$action")
        if (campaign != null) {
            var event = getAndMeasure(store.events, campaignId, campaign) { it.category == category && it.action == action && it.name == name }
            if (event != null) {
                campaign.apply(value, event)
                debug("Gain event insight: %s", event)
            } else {
                // Align first and last timestamps with campaign measurement period,
                // in order not to accidentally leak more information than promised.
                val period = campaign.currentMeasurementPeriod
                if (period != null) {
                    event = Event(category, action, name, value, campaignId, period)
                    store.events.add(event)
                } else {
                    debug("measureEvent campaign.currentMeasurementPeriod == null! This should not happen! $action")
                }
            }
        }
    }

    val featureConsentSize: Int
        get() = store.consents.features.size

    val campaignConsentSize: Int
        get() = store.consents.campaigns.size

    fun getFeatureConsentByIndex(index: Int): FeatureConsent? {
        val features = store.consents.features

        return try {
            store.consents.consent(ArrayList(features.keys)[index])
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }

    fun getCampaignConsentByIndex(index: Int): CampaignConsent? {
        val campaigns = store.consents.campaigns

        return try {
            store.consents.consent(ArrayList(campaigns.keys)[index])
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }

    fun grant(feature: Feature): FeatureConsent {
        val consent = store.consents.grant(feature)
        persistAndSend()
        return consent
    }

    fun deny(feature: Feature): FeatureConsent {
        val consent = store.consents.deny(feature)
        persistAndSend()
        return consent
    }

    /**
     * Returns the consent for a given feature, if any available.
     *
     * @param feature: The feature to get the consent for.
     * @return the `FeatureConsent` for the given feature or `null`, if consent unknown.
     */
    fun consent(feature: Feature): FeatureConsent? {
        if (conf.serverSideAnonymousUsage) return FeatureConsent(feature, Consent(false))

        return store.consents.consent(feature)
    }

    /**
     * Checks the consent state of a feature.
     *
     * @param feature: The feature to check the consent state of.
     * @return the current state of consent.
     */
    fun state(feature: Feature): Consent.State {
        if (conf.serverSideAnonymousUsage) return Consent.State.Denied

        return store.consents.state(feature)
    }

    fun grant(campaignId: String): CampaignConsent? {
        val campaign = conf.campaigns[campaignId] ?: return null
        val consent = store.consents.grant(campaignId, campaign)
        persistAndSend()
        return consent
    }

    fun deny(campaignId: String): CampaignConsent? {
        if (conf.campaigns[campaignId] == null) return null
        val consent = store.consents.deny(campaignId)
        persistAndSend()
        return consent
    }

    /**
     * Checks the consent state for a campaign.
     *
     * @param campaignId: The campaign ID to check the consent for.
     * @return `true`, if consent to run a campaign was given and is now valid.
     */
    fun isCampaignCurrentlyGranted(campaignId: String): Boolean {
        return state(campaignId) == Consent.State.Granted
    }

    /**
     * Returns the consent for a given campaign, if any available.
     *
     * @param campaignId: The campaign ID to get the consent for.
     * @return the `CampaignConsent` for the given camapign or `null`, if consent unknown.
     */
    fun consent(campaignId: String): CampaignConsent? {
        if (conf.serverSideAnonymousUsage) return CampaignConsent(campaignId, Consent(true))

        val campaign = conf.campaigns[campaignId] ?: return null

        if (Date() >= campaign.getEndsDate()) return null

        return store.consents.consent(campaignId)
    }

    /**
     * Checks the consent state of a campaign.
     *
     * @param campaignId: The campaign ID to check the consent of.
     * @return the current state of consent.
     */
    fun state(campaignId: String): Consent.State {
        if (conf.serverSideAnonymousUsage) return Consent.State.Granted

        val campaign = conf.campaigns[campaignId] ?: return Consent.State.Unconfigured

        if (Date() >= campaign.getEndsDate()) return Consent.State.Unconfigured

        return store.consents.state(campaignId)
    }

    /**
     * This is a more Java-friendly implementation of `#requestConsent(campaignId, consentRequestUi, completed)`.
     */
    @JvmOverloads
    fun requestConsent(campaignId: String, consentRequestUi: JavaConsentRequestUi, handler: ConsentRequestUiCompletionHandler? = null) {
        val ui = object : ConsentRequestUi {
            override fun show(campaignId: String, campaign: Campaign, complete: ConsentRequestUiComplete) {
                consentRequestUi.show(campaignId, campaign, object : ConsentRequestUiCompletionHandler {
                    override fun completed(granted: Boolean) {
                        complete(granted)
                    }
                })
            }

            override fun show(feature: Feature, complete: ConsentRequestUiComplete) {
                consentRequestUi.show(feature, object : ConsentRequestUiCompletionHandler {
                    override fun completed(granted: Boolean) {
                        complete(granted)
                    }
                })
            }
        }

        requestConsent(campaignId, ui) { granted ->
            handler?.completed(granted)
        }
    }

    @JvmOverloads
    fun requestConsent(
        campaignId: String, consentRequestUi: ConsentRequestUi,
        completed: ConsentRequestUiComplete? = null
    ) {

        val campaign = conf.campaigns[campaignId]

        if (campaign == null) {
            debug("Cannot request consent: Campaign '%s' not configured.", campaignId)
            if (completed != null) completed(false)
            return
        }

        if (Date() >= campaign.getEndsDate()) {
            debug("Cannot request consent: End of campaign '%s' reached.", campaignId)
            if (completed != null) completed(false)
            return
        }

        if (campaign.nextTotalMeasurementPeriod == null) {
            debug("Cannot request consent: Campaign '%s' configuration seems messed up.", campaignId)
            if (completed != null) completed(false)
            return
        }

        val consent = store.consents.campaigns[campaignId]

        if (consent != null) {
            debug(
                "Already asked for consent for campaign '%s'. It was %s.",
                campaignId,
                if (consent.granted) String.format("granted between %s and %s", consent.start, consent.end)
                else String.format("denied on %s", consent.start)
            )
            if (completed != null) completed(consent.granted)
            return
        }

        val complete = { granted: Boolean ->
            if (granted) {
                store.consents.grant(campaignId, campaign)
            } else {
                store.consents.deny(campaignId)
            }

            if (completed != null) completed(granted)
        }

        MainScope().launch {
            consentRequestUi.show(campaignId, campaign, complete)
        }
    }

    /**
     * This is a more Java-friendly implementation of `#requestConsent(feature, consentRequestUi, completed)`.
     */
    @JvmOverloads
    fun requestConsent(feature: Feature, consentRequestUi: JavaConsentRequestUi, handler: ConsentRequestUiCompletionHandler? = null) {
        val ui = object : ConsentRequestUi {
            override fun show(campaignId: String, campaign: Campaign, complete: ConsentRequestUiComplete) {
                consentRequestUi.show(campaignId, campaign, object : ConsentRequestUiCompletionHandler {
                    override fun completed(granted: Boolean) {
                        complete(granted)
                    }
                })
            }

            override fun show(feature: Feature, complete: ConsentRequestUiComplete) {
                consentRequestUi.show(feature, object : ConsentRequestUiCompletionHandler {
                    override fun completed(granted: Boolean) {
                        complete(granted)
                    }
                })
            }
        }

        requestConsent(feature, ui) { granted ->
            handler?.completed(granted)
        }
    }

    @JvmOverloads
    fun requestConsent(
        feature: Feature, consentRequestUi: ConsentRequestUi,
        completed: ConsentRequestUiComplete? = null
    ) {

        val consent = store.consents.features[feature]

        if (consent != null) {
            debug(
                "Already asked for consent for feature '%s'. It was %s on %s.",
                feature.name,
                if (consent.granted) "granted" else "denied",
                consent.start
            )
            if (completed != null) completed(consent.granted)
            return
        }

        val complete = { granted: Boolean ->
            if (granted) {
                store.consents.grant(feature)
            } else {
                store.consents.deny(feature)
            }

            if (completed != null) completed(store.consents.state(feature) == Consent.State.Granted)
        }

        MainScope().launch {
            consentRequestUi.show(feature, complete)
        }
    }

    /**
     * Sends an empty body to the server for easy debugging of server-related issues like TLS and
     * CORS problems.
     *
     * **DON'T LEAVE THIS IN PRODUCTION**, once you're done fixing any server issues. There's
     * absolutely no point in pinging the server with this all the time and it will undermine your
     * privacy promise to your users!
     *
     * @param done: Callback, when the operation is finished, either successfully or not. OPTIONAL
     */
    fun testServer(done: ((e: java.lang.Exception?) -> Unit)? = null) {
        store.send("", conf.server, conf.timeout) {
            val error = it
                ?: IOException("Server replied with no error while it should have responded with HTTP 400 Bad Request!")

            if (error is IOException && error.message?.startsWith("HTTP Error 400:") == true) {
                debug("Successfully tested server.")

                if (done != null) done(null)

                return@send
            }

            debug(error)

            if (done != null) done(error)
        }
    }

    /**
     * Persist accumulated data to the filesystem.
     *
     * The app should call this on `applicationDidEnterBackground:`.
     */
    fun persist() {
        persist(async = false, force = true)
    }

    private var campaignId: String = ""
    fun setCampaignId(campaignId: String) {
        this.campaignId = campaignId
    }

    fun getCampaignId() = campaignId

    /**
     * Persist accumulated data to the filesystem.
     *
     * @param async: If true, returns immediately and does persistence asynchronously, only if it's already due.
     * @param force: Write regardless of threshold reached.
     */
    private fun persist(async: Boolean, force: Boolean = false) {
        persistenceCounter += 1

        if (force || persistenceCounter >= conf.persistEveryNTimes) {
            store.persist(async) {
                if (it != null) {
                    debug(it)
                } else {
                    persistenceCounter = 0

                    debug("Data persisted to storage.")
                }
            }
        }
    }

    /**
     * Persist data asynchronously and send all data to the CleanInsights Matomo Proxy server.
     *
     * If sending was successful, remove sent data from store and persist again.
     */
    fun persistAndSend() {
        persist(true)

        if (failedSubmissionCount > 0) {
            // Calculate a delay for the next retry:
            // Minimum is 2 times the configured network timeout after the first failure,
            // exponentially increasing with number of retries.
            // Maximum is every conf.maxRetryDelay interval.
            val exp = Date(lastFailedSubmission.time + ((conf.timeout * 2.0.pow(failedSubmissionCount.toDouble())).toLong()))
            val tru = Date(lastFailedSubmission.time + (conf.maxRetryDelay.toLong()))
            val retryAllowedAt = if (exp.before(tru)) exp else tru
            if (Date().before(retryAllowedAt)) return
        }

        val insights = Insights(conf, store)
        insights.events.addAll(store.events)
        insights.visits.addAll(store.visits)
        if (insights.isEmpty) return

        val done = { e: Exception? ->
            if (e != null) {
                lastFailedSubmission = Date()
                failedSubmissionCount++
                debug(e)
            } else {
                lastFailedSubmission = Date(0)
                failedSubmissionCount = 0
                debug("Successfully offloaded data.")
                insights.clean(store)
                persist(async = true, force = true)
            }
        }

        val body: String
        try {
            val arrayEvents = ArrayList<EventJson>()
            val arrayVisits = ArrayList<VisitJson>()
            insights.visits.forEach { arrayVisits.add(VisitJson(it.scenePath, it.campaignId, it.times, it.first.time, it.last.time)) }
            insights.events.forEach {
                arrayEvents.add(EventJson(it.category, it.action, it.name, it.value, it.campaignId, it.times, it.first.time, it.last.time))
            }
            val insightsJson = InsightsJson(insights.idsite, insights.lang, insights.ua, arrayVisits, arrayEvents)
            body = Gson().toJson(insightsJson)
        } catch (e: Exception) {
            done(e)
            return
        }

        val campaign = conf.campaigns[getCampaignId()] ?: return
        if (Date() >= campaign.getEndsDate()) store.send(body, conf.server, conf.timeout, done)
    }

    data class InsightsJson(val idsite: Int, val lang: String?, val ua: String?, val visits: ArrayList<VisitJson>, val events: ArrayList<EventJson>)
    data class VisitJson(val scenePath: List<String>, val campaign_id: String, val times: Int = 1, val period_start: Long, val period_end: Long)
    data class EventJson(
        val category: String,
        val action: String,
        val name: String?,
        val value: Double?,
        val campaign_id: String,
        val times: Int = 1,
        val period_start: Long,
        val period_end: Long
    )

    private fun getCampaignIfGood(campaignId: String, debugString: String): Campaign? {
        val campaign = conf.campaigns[campaignId]

        if (campaign == null) {
            debug("Measurement '%s' discarded, because campaign '%s' is missing in configuration.", debugString, campaignId)
            return null
        }

        val now = Date()

        if (now < campaign.getStartsDate()) {
            debug("Measurement '%s' discarded, because campaign '%s' didn't start, yet.", debugString, campaignId)
            return null
        }

        if (now > campaign.getEndsDate()) {
            debug("Measurement '%s' discarded, because campaign '%s' already ended.", debugString, campaignId)
            return null
        }

        return campaign
    }


    /**
     * Get a `DataPoint` subclass out of the `haystack`, as long as it fits the `campaign`.
     * Increases `times` according to the campaigns rules.
     *
     * Create a new `DataPoint` if nothing is returned here.
     *
     * @param haystack: The haystack full of `DataPoint` subclasses.
     * @param campaignId: The campaign ID it must match.
     * @param campaign: The campaign parameters to match against.
     * @param where: Additional condition for selection.
     * @return a `DataPoint` subclass out of the `haystack`, as long as it fits the `campaign`.
     */
    private fun <T : DataPoint> getAndMeasure(haystack: List<T>, campaignId: String, campaign: Campaign, where: ((T) -> Boolean)): T? {
        val period = campaign.currentMeasurementPeriod

        if (period == null) {
            debug("getAndMeasure campaign.currentMeasurementPeriod == null! This should not happen!")
            return null
        }

        try {
            val dataPoint = haystack.first {
                it.campaignId == campaignId
                        && it.first >= period.start
                        && it.first <= period.end
                        && it.last >= period.start
                        && it.last <= period.end
                        && `where`(it)
            }

            if (!campaign.onlyRecordOnce) dataPoint.times += 1

            return dataPoint
        } catch (e: NoSuchElementException) {
            return null
        }
    }

    private fun debug(format: String, vararg args: Any?) {
        debug(conf.debug, format, *args)
    }

    private fun debug(e: Exception) {
        debug(e.localizedMessage ?: e.toString())
    }
}