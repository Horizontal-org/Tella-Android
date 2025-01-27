package org.horizontal.tella.mobile.views.dialog.uwazi

import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer

object SharedLiveData {
    val createServer = SingleLiveEvent<UWaziUploadServer>()
    val updateServer = SingleLiveEvent<UWaziUploadServer>()
}