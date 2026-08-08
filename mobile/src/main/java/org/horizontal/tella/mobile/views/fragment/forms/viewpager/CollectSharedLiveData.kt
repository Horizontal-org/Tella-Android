package org.horizontal.tella.mobile.views.fragment.forms.viewpager

import org.horizontal.tella.mobile.bus.SingleLiveEvent

/**
 * Collect's tab bus. It is deliberately separate from the connections'
 * [org.horizontal.tella.mobile.views.fragment.main_connexions.base.SharedLiveData]: Collect numbers
 * its tabs from its own blank-forms tab, so sharing one bus would let a Collect tab change move a
 * backgrounded connection's ViewPager to the wrong tab.
 */
object CollectSharedLiveData {
    val updateViewPagerPosition = SingleLiveEvent<Int>()
}
