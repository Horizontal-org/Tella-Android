package org.horizontal.tella.mobile.domain.entity

import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile

/**
 * The subset of a submittable instance that the shared connection UI (tabs, draft/outbox/submitted
 * lists, send screen) needs, regardless of whether the instance is a report or a Uwazi entity.
 */
interface IEntityInstance {
    var id: Long
    var title: String
    var updated: Long
    var status: EntityStatus
    var widgetMediaFiles: List<FormMediaFile>
    val serverId: Long
}
