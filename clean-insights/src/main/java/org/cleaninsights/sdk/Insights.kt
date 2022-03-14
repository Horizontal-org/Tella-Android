/**
 * Insights.kt
 * CleanInsightsSDK
 *
 * Created by Benjamin Erhart.
 * Copyright © 2020 Guardian Project. All rights reserved.
 */
package org.cleaninsights.sdk

import android.os.Build
import java.util.*
import kotlin.collections.ArrayList

/**
 * Create an `Insights` object according to configuration with all data from the store which is due
 * for offloading to the server.
 *
 * @param idsite: Matomo site ID.
 * @param lang: Preferred user languages as an HTTP Accept header.
 * @param ua: User Agent string.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class Insights(val idsite: Int, val lang: String?, val ua: String?) {

    companion object {
        val userAgent: String
            get() {
                var httpAgent = System.getProperty("http.agent")

                if (httpAgent == null || httpAgent.startsWith("Apache-HttpClient/UNAVAILABLE (java")) {
                    httpAgent = String.format(Locale.US,
                            "Dalvik/%s (Linux; U; Android %s; %s Build/%s)",
                            System.getProperty("java.vm.version") ?: "0.0.0",
                            Build.VERSION.RELEASE, Build.MODEL, Build.ID)
                }

                return httpAgent
            }

        /**
         * Removes `DataPoint`s, which are too old. These were never been sent, otherwise, they would
         * have been removed, already.
         *
         * Remove them now, if they're over the threshold, to not accumulate too many `DataPoints`
         * and therefore reduce privacy.
         */
        @JvmStatic
        private fun purge(conf: CleanInsightsConfiguration, store: Store) {
            val threshold = Date(Date().time - Date(conf.maxAgeOfOldData.toLong()).time)

            val filter = { dp: DataPoint ->
                dp.last.before(threshold)
            }

            store.visits.removeAll(filter)
            store.events.removeAll(filter)
        }
    }

    /**
     * Visit` data points.
     */
    val visits = ArrayList<Visit>()

    /**
    Event data points.
     */
    val events = ArrayList<Event>()

    /**
     * Create an `Insights` object according to configuration with all data from the store which is due
     * for offloading to the server.
     *
     * @param conf: The current configuration.
     * @param store: The current measurement and consents store.
     */
    constructor(conf: CleanInsightsConfiguration, store: Store) : this(
        conf.siteId,
        if (!conf.serverSideAnonymousUsage && store.consents.state(Feature.Lang) == Consent.State.Granted) Locale.getDefault().language else null,
        if (!conf.serverSideAnonymousUsage && store.consents.state(Feature.Ua) == Consent.State.Granted) userAgent else null
    ) {
        purge(conf, store)

        val now = Date()

        for (visit in store.visits) {
            if (!conf.campaigns.containsKey(visit.campaignId)) {
                continue
            }

            // Only send, after aggregation period is over. `last` should contain that date!
            if (now > visit.last) {
                visits.add(visit)
            }
        }

        for (event in store.events) {
            if (!conf.campaigns.containsKey(event.campaignId)) {
                continue
            }

            // Only send, after aggregation period is over. `last` should contain that date!
            if (now > event.last) {
                events.add(event)
            }
        }
    }


    val isEmpty: Boolean
        get() = visits.isEmpty() && events.isEmpty()

    /**
     * Removes all visits and events from the given `Store`, which are also available in here.
     *
     * This should be called, when all `Insights` were offloaded at the server successfully.
     *
     * @param store: The store where the `Visit`s and `Event`s in here came from.
     */
    fun clean(store: Store) {
        store.visits.removeAll(visits)

        store.events.removeAll(events)
    }
}
