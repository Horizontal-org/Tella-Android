package rs.readahead.washington.mobile.views.fragment.main_connexions.base

import rs.readahead.washington.mobile.bus.SingleLiveEvent

object SharedLiveData {
    val updateViewPagerPosition = SingleLiveEvent<Int>()
}