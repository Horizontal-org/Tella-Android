/**
 * EventAggregationRule.kt
 * CleanInsightsSDK
 *
 * Created by Benjamin Erhart.
 * Copyright © 2020 Guardian Project. All rights reserved.
 */
package org.cleaninsights.sdk

import com.squareup.moshi.Json

enum class EventAggregationRule {

    /**
     * Just add any new value given.
     */
    @Json(name = "sum")
    Sum,

    /**
     * Calculate the average of the given values.
     */
    @Json(name = "avg")
    Avg
}
