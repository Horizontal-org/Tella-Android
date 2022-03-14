/**
 * Event.kt
 * CleanInsightsSDK
 *
 * Created by Benjamin Erhart.
 * Copyright © 2020 Guardian Project. All rights reserved.
 */
package org.cleaninsights.sdk

import java.time.Instant
import java.util.*

/**
 * @param category: The event category. Must not be empty. (eg. Videos, Music, Games...)
 * @param action: The event action. Must not be empty. (eg. Play, Pause, Duration, Add Playlist, Downloaded, Clicked...)
 * @param name: The event name. OPTIONAL.
 * @param value: The event value. OPTIONAL.
 * @param campaignId: The campaign ID this data point is for.
 * @param times: Number of times this data point has arisen between `first` and `last`. OPTIONAL, defaults to 1.
 * @param first: The first time this data point has arisen. OPTIONAL, defaults to now.
 * @param last: The last time this data point has arisen. OPTIONAL, defaults to now.
 */
class Event(
    val category: String, val action: String, val name: String?, var value: Double?,
    campaignId: String, times: Int = 1, first: Date = Date(), last: Date = Date()) :
        DataPoint(campaignId, times, first, last) {

    constructor(category: String, action: String, name: String?, value: Double?, campaignId: String, period: Period)
            : this(category, action, name, value, campaignId, 1, period.start, period.end)

    override fun equals(other: Any?): Boolean {
        if (other is Event) {
            return category == other.category && action == other.action && name == other.name
                    && value == other.value && super.equals(other)
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = category.hashCode()
        result = 31 * result + action.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (value?.hashCode() ?: 0)
        result = 31 * result + super.hashCode()
        return result
    }

    override fun toString(): String {
        return String.format("[%s: category=%s, action=%s, name=%s, value=%f, campaignId=%s, times=%d, first=%s, last=%s]",
                this::class, category, action, name, value, campaignId, times, first, last)
    }
}