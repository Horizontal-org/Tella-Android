package org.horizontal.tella.mobile.views.fragment.uwazi

import org.horizontal.tella.mobile.bus.SingleLiveEvent

object SharedLiveData {
    val updateViewPagerPosition = SingleLiveEvent<Int>()
}