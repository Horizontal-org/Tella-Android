/**
* Visit.kt
* CleanInsightsSDK
*
* Created by Benjamin Erhart.
* Copyright © 2020 Guardian Project. All rights reserved.
*/
package org.cleaninsights.sdk

import java.util.*

/**
 * @param scenePath: A hierarchical path to the scene visited.
 * @param campaignId: The campaign ID this data point is for.
 * @param times: Number of times this data point has arisen between `first` and `last`. OPTIONAL, defaults to 1.
 * @param first: The first time this data point has arisen. OPTIONAL, defaults to now.
 * @param last: The last time this data point has arisen. OPTIONAL, defaults to now.
 */
class Visit(val scenePath: List<String>, campaignId: String, times: Int = 1, first: Date = Date(), last: Date =  Date()) :
        DataPoint(campaignId, times, first, last) {

    constructor(scenePath: List<String>, campaignId: String, period: Period?)
            : this(scenePath, campaignId, 1, period?.start ?: Date(), period?.end ?: Date())

    override fun equals(other: Any?): Boolean {
        if (other is Visit) {
            return scenePath == other.scenePath && super.equals(other)
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = scenePath.hashCode()
        result = 31 * result + super.hashCode()
        return result
    }

    override fun toString(): String {
        return String.format("[%s: scenePath=%s, campaignId=%s, times=%d, first=%s, last=%s]",
                this::class, scenePath, campaignId, times, first, last)
    }
}