package rs.readahead.washington.mobile.views.dialog.uwazi

import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer

object SharedLiveData {
    val createServer = SingleLiveEvent<UWaziUploadServer>()
    val updateServer = SingleLiveEvent<UWaziUploadServer>()
}