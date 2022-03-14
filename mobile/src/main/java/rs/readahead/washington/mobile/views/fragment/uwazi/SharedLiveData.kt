package rs.readahead.washington.mobile.views.fragment.uwazi

import rs.readahead.washington.mobile.bus.SingleLiveEvent

object SharedLiveData {
    val updateViewPagerPosition = SingleLiveEvent<Int>()
}