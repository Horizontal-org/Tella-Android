/**
 * Consent.kt
 * CleanInsightsSDK
 *
 * Created by Benjamin Erhart.
 * Copyright © 2020 Guardian Project. All rights reserved.
 */
@file:Suppress("unused")

package org.cleaninsights.sdk

import com.squareup.moshi.Json
import java.util.*

enum class Feature {
    @Json(name = "lang")
    Lang,

    @Json(name = "ua")
    Ua
}

open class Consent(val granted: Boolean, val start: Date = Date(), val end: Date = Date()) {

    enum class State {

        /**
        A campaign with that ID doesn't exist or already expired.
         */
        Unconfigured,

        /**
        There's no record of consent. User was probably never asked.
         */
        Unknown,

        /**
        User denied consent. Don't ask again!
         */
        Denied,

        /**
        Consent was given, but consent period has not yet started.
         */
        NotStarted,

        /**
        Consent was given, but consent period is over. You might ask again for a new period.
         */
        Expired,

        /**
        Consent was given and is currently valid.
         */
        Granted
    }

    open val state: State
        get() = if (granted) State.Granted else State.Denied

    constructor(granted: Boolean, period: Period) : this(granted, period.start, period.end)

    override fun equals(other: Any?): Boolean {
        if (other is Consent) {
            return granted == other.granted && start == other.start && end == other.end
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = granted.hashCode()
        result = 31 * result + start.hashCode()
        result = 31 * result + end.hashCode()
        return result
    }

    override fun toString(): String {
        return String.format(
            "[%s: granted=%b, start=%s, end=%s]",
            this::class, granted, start, end
        )
    }
}

class FeatureConsent(val feature: Feature, consent: Consent) :
    Consent(consent.granted, consent.start, consent.end)

class CampaignConsent(val campaignId: String, consent: Consent) :
    Consent(consent.granted, consent.start, consent.end) {

    override val state: State
        get() {
            if (!granted) return State.Denied

            val now = Date()

            if (now < start) return State.NotStarted

            if (now > end) return State.Expired

            return State.Granted
        }
}
