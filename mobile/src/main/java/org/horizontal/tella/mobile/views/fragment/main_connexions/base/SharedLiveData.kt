package org.horizontal.tella.mobile.views.fragment.main_connexions.base

import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer
import org.horizontal.tella.mobile.views.fragment.connections.SharedConnectionLiveData

object SharedLiveData {
    val updateOutboxTitle = SharedConnectionLiveData.updateOutboxTitle
    val updateSubmittedTitle = SharedConnectionLiveData.updateSubmittedTitle
    val updateDraftTitle = SharedConnectionLiveData.updateDraftTitle
    val refreshTokenServer = SingleLiveEvent<DropBoxServer>()
}
