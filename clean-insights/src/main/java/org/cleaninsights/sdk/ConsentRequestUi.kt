/**
 * ConsentRequestUi.kt
 * CleanInsightsSDK
 *
 * Created by Benjamin Erhart.
 * Copyright © 2020 Guardian Project. All rights reserved.
 */
package org.cleaninsights.sdk

typealias ConsentRequestUiComplete = (granted: Boolean) -> Unit

/**
 * Used for Java interop.
 */
interface ConsentRequestUiCompletionHandler {
    fun completed(granted: Boolean)
}

interface ConsentRequestUi {

    /**
     * Will be called if it is necessary to ask the user for consent to a measurement campaign.
     *
     * @param campaignId: The campaign identifier.
     * @param campaign: The campaign configuration.
     * @param complete: The callback which will store the consent or the denial of it.
     */
    fun show(campaignId: String, campaign: Campaign, complete: ConsentRequestUiComplete)

    /**
     * Will be called if it is necessary to ask the user for consent to record a common feature while measuring a campaign.
     *
     * @param feature: The feature to record. (e.g. user agent, locale)
     * @param complete: The callback which will store the consent or the denial of it.
     */
    fun show(feature: Feature, complete: ConsentRequestUiComplete)
}

interface JavaConsentRequestUi {

    /**
     * Will be called if it is necessary to ask the user for consent to a measurement campaign.
     *
     * @param campaignId: The campaign identifier.
     * @param campaign: The campaign configuration.
     * @param handler: The handler which will store the consent or the denial of it.
     */
    fun show(campaignId: String, campaign: Campaign, handler: ConsentRequestUiCompletionHandler)

    /**
     * Will be called if it is necessary to ask the user for consent to record a common feature while measuring a campaign.
     *
     * @param feature: The feature to record. (e.g. user agent, locale)
     * @param handler: The handler which will store the consent or the denial of it.
     */
    fun show(feature: Feature, handler: ConsentRequestUiCompletionHandler)
}