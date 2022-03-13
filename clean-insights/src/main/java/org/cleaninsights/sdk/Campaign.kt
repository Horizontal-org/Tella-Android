/**
 * Campaign.kt
 * CleanInsightsSDK
 *
 * Created by Benjamin Erhart.
 * Copyright © 2020 Guardian Project. All rights reserved.
 */
package org.cleaninsights.sdk

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*


const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
const val DAY_IN_SECONDS = 86400

class Period(val start: Date, val end: Date)

/**
 * @param start: The start of the campaign. (inclusive)
 * @param end: The end of the campaign. (inclusive)
 * @param aggregationPeriodLength: The length of the aggregation period in number of days.
 *      At the end of a period, the aggregated data will be sent to the analytics server.
 * @param numberOfPeriods: The number of periods you want to measure in a row. Therefore the total
 *      length in days you measure one user is `aggregationPeriodLength * numberOfPeriods` beginning
 *      with the first day of the next period after the user consented.
 * @param onlyRecordOnce: Will result in recording only the first time a visit or event happened per
 *      period. Useful for yes/no questions.
 * @param eventAggregationRule: The rule how to aggregate the value of an event (if any given) with
 *      subsequent calls.
 * @param strengthenAnonymity: When set to true, measurements only ever start at the next full period.
 *      This ensures, that anonymity guaranties aren't accidentally reduced because the
 *      first period is very short.
 */
@Suppress("MemberVisibilityCanBePrivate")
data class Campaign(
    val start: String, val end: String, val aggregationPeriodLength: Long,
    val numberOfPeriods: Int = 1, val onlyRecordOnce: Boolean = false,
    val eventAggregationRule: EventAggregationRule = EventAggregationRule.Sum,
    val strengthenAnonymity: Boolean = false
) {

    val currentMeasurementPeriod: Period?
        get() {
            if (numberOfPeriods < 1) return null

            var now = Date()

            var periodEnd = getStartsDate()

            do {
                periodEnd = Date(periodEnd.time + (aggregationPeriodLength* DAY_IN_SECONDS))
            } while (periodEnd <= now)

            var periodStart = Date(periodEnd.time - (aggregationPeriodLength* DAY_IN_SECONDS))

            if (periodStart < getStartsDate()) periodStart = getStartsDate()

            if (getEndsDate() < periodEnd) periodEnd = getEndsDate()

            now = Date()

            if (periodStart > now || periodEnd < now) {
                return null
            }

            return Period(periodStart, periodEnd)
        }

    val nextTotalMeasurementPeriod: Period?
        get() {
            val current = currentMeasurementPeriod ?: return null

            val periodStart = if (strengthenAnonymity) current.end else current.start

            var periodEnd = periodStart

            var counter = 0

            while (counter < numberOfPeriods) {
                periodEnd = Date(periodEnd.time + (aggregationPeriodLength* DAY_IN_SECONDS))

                if (periodEnd > getEndsDate()) {
                    periodEnd = Date(periodEnd.time - (aggregationPeriodLength * DAY_IN_SECONDS))
                    break
                }

                counter++
            }

            if (periodStart == periodEnd) {
                return null
            }

            return Period(periodStart, periodEnd)
        }

    /**
     * Apply the `eventAggregationRule` to the given event with the given value.
     *
     * @param value: The value to apply.
     * @param event: The event to apply the value to.
     */
    fun apply(value: Double?, event: Event) {
        if (value == null || onlyRecordOnce) return

        val oldVal = event.value ?: 0.0

        when (eventAggregationRule) {
            EventAggregationRule.Sum ->
                event.value = oldVal + value

            EventAggregationRule.Avg ->
                event.value = (oldVal * (event.times - 1) + value) / event.times
        }
    }

    fun getStartsDate(): Date {
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return sdf.parse(start)!!
    }

    fun getEndsDate(): Date {
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return sdf.parse(end)!!
    }
}