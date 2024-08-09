/**
 * EventAggregationRule.kt
 * CleanInsightsSDK
 *
 * Created by Benjamin Erhart.
 * Copyright © 2020 Guardian Project. All rights reserved.
 */
package org.cleaninsights.sdk


enum class EventAggregationRule {

    /**
     * Just add any new value given.
     */
    Sum,

    /**
     * Calculate the average of the given values.
     */
    Avg
}
