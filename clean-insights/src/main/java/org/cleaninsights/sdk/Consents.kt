/**
 * Consents.kt
 * CleanInsightsSDK
 *
 * Created by Benjamin Erhart.
 * Copyright © 2020 Guardian Project. All rights reserved.
 */
package org.cleaninsights.sdk

import kotlin.collections.set

/**
 * This class keeps track of all granted or denied consents of a user.
 *
 * There are two different types of consents:
 * - Consents for common features like if we're allowed to evaluate the locale or a user agent.
 * - Consents per measurement campaign.
 *
 * The time of the consent is recorded along with it's state: If it was actually granted or denied.
 *
 * Consents for common features are given indefinitely, since they are only ever recorded along with
 * running campaigns.
 *
 * Consents for campaigns only last for a certain amount of days.
 */
@Suppress("MemberVisibilityCanBePrivate")
class Consents {

    var features = LinkedHashMap<Feature, Consent>()

    var campaigns = LinkedHashMap<String, Consent>()


    /**
     * User consents to evaluate a `Feature`.
     */
    fun grant(feature: Feature): FeatureConsent {
        // Don't overwrite original grant timestamp.
        if (features[feature] == null || !features[feature]!!.granted) {
            features[feature] = Consent(true)
        }

        return FeatureConsent(feature, features[feature]!!)
    }

    /**
     * User denies consent to evaluate a `Feature`.
     */
    fun deny(feature: Feature): FeatureConsent {
        // Don't overwrite original deny timestamp.
        if (features[feature] == null || features[feature]!!.granted) {
            features[feature] = Consent(false)
        }

        return FeatureConsent(feature, features[feature]!!)
    }

    /**
     * Returns the consent for a given feature, if any available.
     *
     * @param feature: The feature to get the consent for.
     * @return the `FeatureConsent` for the given feature or `null`, if consent unknown.
     */
    fun consent(feature: Feature): FeatureConsent? {
        val consent = features[feature] ?: return null

        return FeatureConsent(feature, consent)
    }

    /**
     * Checks the consent state for a feature.
     *
     * @param feature: The feature to check the consent state for.
     * @return the current state of consent.
     */
    fun state(feature: Feature): Consent.State {
        return consent(feature)?.state ?: return Consent.State.Unknown
    }

    /**
     * User consents to run a specific campaign.
     *
     * @param campaignId: The campaign ID.
     * @param campaign: The campaign.
     */
    fun grant(campaignId: String, campaign: Campaign): CampaignConsent {

        val period = campaign.nextTotalMeasurementPeriod

        if (period != null) {
            // Always overwrite, since this might be a refreshed consent for a new period.
            campaigns[campaignId] = Consent(true, period)
        }
        else {
            // Consent is technically granted, but has no effect, as start and end
            // will be set the same.
            campaigns[campaignId] = Consent(true)
        }

        return CampaignConsent(campaignId, campaigns[campaignId]!!)
    }

    /**
     * User denies consent to run a specific campaign.
     */
    fun deny(campaignId: String): CampaignConsent {
        // Don't overwrite original deny timestamp.
        if (campaigns[campaignId] == null || campaigns[campaignId]!!.granted) {
            campaigns[campaignId] = Consent(false)
        }

        return CampaignConsent(campaignId, campaigns[campaignId]!!)
    }

    /**
     * Checks the consent state for a campaign.
     *
     * @param campaignId: The campaign ID to check the consent state for.
     * @return the current state of consent.
     */
    fun consent(campaignId: String): CampaignConsent? {
        val consent = campaigns[campaignId] ?: return null

        return CampaignConsent(campaignId, consent)
    }

    /**
     * Checks the consent state of a campaign.
     *
     * @param campaignId: The campaign ID to check the consent state of.
     * @return the current state of consent.
     */
    fun state(campaignId: String): Consent.State {
        return consent(campaignId)?.state ?: return Consent.State.Unknown
    }

    override fun equals(other: Any?): Boolean {
        if (other is Consents) {
            return features == other.features && campaigns == other.campaigns
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = features.hashCode()
        result = 31 * result + campaigns.hashCode()
        return result
    }

    override fun toString(): String {
        return String.format("[%s: features=%s, campaigns=%s]",
                this::class, features, campaigns)
    }
}
