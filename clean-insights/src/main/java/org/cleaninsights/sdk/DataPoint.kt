/**
 * DataPoint.kt
 * CleanInsightsSDK
 *
 * Created by Benjamin Erhart.
 * Copyright © 2020 Guardian Project. All rights reserved.
 */
package org.cleaninsights.sdk

import java.util.*

/**
 * @param campaignId: The campaign ID this data point is for.
 * @param times: Number of times this data point has arisen between `first` and `last`. OPTIONAL, defaults to 1.
 * @param first: The first time this data point has arisen. OPTIONAL, defaults to now.
 * @param last: The last time this data point has arisen. OPTIONAL, defaults to now.
 */
open class DataPoint(val campaignId: String, var times: Int = 1, val first: Date = Date(),
                     val last: Date = Date()) {

    override fun equals(other: Any?): Boolean {
        if (other is DataPoint) {
            return campaignId == other.campaignId && times == other.times && first == other.first
                    && last == other.last
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = campaignId.hashCode()
        result = 31 * result + times
        result = 31 * result + first.hashCode()
        result = 31 * result + last.hashCode()
        return result
    }

    override fun toString(): String {
        return String.format("[%s: campaignId=%s, times=%d, first=%s, last=%s]",
                this::class, campaignId, times, first, last)
    }
}
