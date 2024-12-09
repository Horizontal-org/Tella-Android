package rs.readahead.washington.mobile.views.fragment.main_connexions.base

import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.domain.entity.dropbox.DropBoxServer

object SharedLiveData {
    val updateViewPagerPosition = SingleLiveEvent<Int>()
    val updateOutboxTitle = SingleLiveEvent<Int>()
    val updateSubmittedTitle = SingleLiveEvent<Int>()
    val updateDraftTitle = SingleLiveEvent<Int>()
    val refreshTokenServer = SingleLiveEvent<DropBoxServer>()
}