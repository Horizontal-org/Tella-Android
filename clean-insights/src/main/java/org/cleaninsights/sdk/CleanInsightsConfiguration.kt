/**
 * Configuration.kt
 * CleanInsightsSDK
 *
 * Created by Benjamin Erhart.
 * Copyright © 2020 Guardian Project. All rights reserved.
 */
package org.cleaninsights.sdk

import java.net.URL

/**
 * @param server
 *      The server URL should look like `https://myhost.example.com/ci/cleaninsights.php`.
 *
 * @param siteId
 *      The Matomo site ID to record this data for.
 *
 * @param campaigns
 *      Campaign configuration.
 *
 * @param timeout
 *      Connection timeout. OPTIONAL, defaults to 5 seconds.
 *
 * @param maxRetryDelay
 *      The SDK uses a truncated exponential backoff strategy on server failures. So the delay until
 *      it retries will rise exponentially, until it reaches `maxRetryDelay` seconds.
 *      OPTIONAL, defaults to 3600 seconds. (1 hour)
 *
 * @param maxAgeOfOldData
 *      The number in days of how long the SDK will try to keep sending old measurements.
 *      If the measurements become older than that, they will be purged.
 *      OPTIONAL, defaults to 100 days.
 *
 * @param persistEveryNTimes
 *      Regulates, how often data persistence is done. OPTIONAL. Defaults to 10.
 *      If set to 1, every time something is tracked, *ALL* data is stored to disk. The more you track,
 *      the higher you should set this to avoid heavy load due to disk I/O.
 *
 * @param serverSideAnonymousUsage
 *      When set to true, assumes consent for all campaigns and none for features.
 *      Only use this, when you're running on the server and don't measure anything users
 *      might need to give consent to!
 *
 * @param debug
 *      When set, CleanInsights SDK will print some debug output to STDOUT. OPTIONAL. Defaults to false.
 */
data class CleanInsightsConfiguration(
    val server: URL,
    val siteId: Int,
    val campaigns: Map<String, Campaign>,
    val timeout: Double = 5.0,
    val maxRetryDelay: Double = 3600.0,
    val maxAgeOfOldData: Int = 100,
    val persistEveryNTimes: Int = 10,
    val serverSideAnonymousUsage: Boolean = false,
    val debug: Boolean = false)
{

    /**
     * Checks configuration for some well-known problems, emits a debug message and returns false, if one found.
     *
     * @param debug
     *      Callback to handle the debug message.
     * @return `true`, if config seems ok, `false` if known problems exist.
     */
    fun check(debug: (String) -> Unit): Boolean {
        if (!server.toString().startsWith("http")) {
            debug("Configuration problem: 'server' is not defined properly. It needs to be a full URL like this: 'https://example.org/cleaninsights.php'!")

            return false
        }

        if (siteId < 1) {
            debug("Configuration problem: 'siteId' is not defined properly. It needs to be a positive integer value!")

            return false
        }

        if (campaigns.keys.isEmpty()) {
            debug("Configuration problem: No campaign defined!")

            return false
        }

        return true
    }
}