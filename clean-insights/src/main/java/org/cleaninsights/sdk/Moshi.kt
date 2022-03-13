/**
 * Moshi.kt
 * CleanInsightsSDK
 *
 * Created by Benjamin Erhart.
 * Copyright © 2020 Guardian Project. All rights reserved.
 */
package org.cleaninsights.sdk

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.LinkedHashMap

internal class UrlAdapter {

    @ToJson
    fun toJson(url: URL): String {
        return url.toString()
    }

    @FromJson
    fun fromJson(json: String): URL {
        return URL(json)
    }
}

internal class DateAdapter {
    @ToJson
    fun toJson(date: Date): String {
        return date.time.toString()
    }

    @FromJson
    fun fromJson(json: String): Date {
        return Date(json.toLong())
    }
}
internal class FeatureConsentsJsonAdapter {

    @ToJson
    fun toJson(consents: LinkedHashMap<Feature, Consent>): Map<Feature, Consent> {
        return consents
    }

    @FromJson
    fun fromJson(consents: Map<Feature, Consent>): LinkedHashMap<Feature, Consent> {
        return LinkedHashMap(consents)
    }
}

internal class CampaignConsentsJsonAdapter {

    @ToJson
    fun toJson(consents: LinkedHashMap<String, Consent>): Map<String, Consent> {
        return consents
    }

    @FromJson
    fun fromJson(consents: Map<String, Consent>): LinkedHashMap<String, Consent> {
        return LinkedHashMap(consents)
    }
}

internal class VisitListJsonAdapter {

    @ToJson
    fun toJson(list: ArrayList<Visit>): List<Visit> {
        return list
    }

    @FromJson
    fun fromJson(list: List<Visit>): ArrayList<Visit> {
        return ArrayList(list)
    }
}

internal class EventListJsonAdapter {

    @ToJson
    fun toJson(list: ArrayList<Event>): List<Event> {
        return list
    }

    @FromJson
    fun fromJson(list: List<Event>): ArrayList<Event> {
        return ArrayList(list)
    }
}

@Suppress("unused")
internal open class DataPointData(
    val period_start: Long,
    val period_end: Long,
    val times: Int
)

@Suppress("unused")
internal class VisitData(
    val action_name: String,
    period_start: Long, period_end: Long, times: Int
) : DataPointData(period_start, period_end, times)

@Suppress("unused")
internal class EventData(
    val category: String,
    val action: String,
    val name: String?,
    val value: Double?,
    period_start: Long, period_end: Long, times: Int
) : DataPointData(period_start, period_end, times)

@Suppress("unused", "SpellCheckingInspection")
internal class InsightsData(
    val idsite: Int,
    val lang: String?,
    val ua: String?,
    val visits: List<VisitData>?,
    val events: List<EventData>?
)

internal class InsightsJsonAdapter {

    @ToJson
    fun toJson(i: Insights): InsightsData {
        return InsightsData(i.idsite, i.lang, i.ua,
            i.visits.map {
                VisitData(
                    it.scenePath.joinToString("/"),
                    it.first.time,
                    it.last.time,
                    it.times
                )
            }.ifEmpty { null },
            i.events.map {
                EventData(
                    it.category,
                    it.action,
                    it.name,
                    it.value,
                    it.first.time,
                    it.last.time,
                    it.times
                )
            }.ifEmpty { null })
    }
}
