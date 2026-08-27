package org.horizontal.tella.mobile.views.fragment.main_connexions.base

import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.Server
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance

/**
 * Adds the report-form specifics (a fixed title/description/attachments form) on top of the
 * shared draft/outbox/submitted behaviour. Uwazi builds its instances from a downloaded template
 * instead, so it extends [BaseEntityListViewModel] directly.
 */
abstract class BaseReportsViewModel : BaseEntityListViewModel<ReportInstance>() {

    //TODO CHECK FOR LATER AHLEM + WAFA
    // val reportProcess: SingleLiveEvent<Pair<UploadProgressInfo, ReportInstance>>
    // val instanceProgress: SingleLiveEvent<ReportInstance>

    abstract fun getFormInstance(
        title: String,
        description: String,
        files: List<FormMediaFile>?,
        server: Server,
        id: Long? = null,
        reportApiId: String = "",
        status: EntityStatus
    ): ReportInstance

    abstract fun getDraftFormInstance(
        title: String,
        description: String,
        files: List<FormMediaFile>?,
        server: Server,
        id: Long? = null
    ): ReportInstance
}
