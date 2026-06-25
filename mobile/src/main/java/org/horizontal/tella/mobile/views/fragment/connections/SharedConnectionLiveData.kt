package org.horizontal.tella.mobile.views.fragment.connections

import org.horizontal.tella.mobile.bus.SingleLiveEvent

object SharedConnectionLiveData {
    val updateViewPagerPosition = SingleLiveEvent<ConnectionTab>()
    val updateOutboxTitle = SingleLiveEvent<Int>()
    val updateSubmittedTitle = SingleLiveEvent<Int>()
    val updateDraftTitle = SingleLiveEvent<Int>()
}
