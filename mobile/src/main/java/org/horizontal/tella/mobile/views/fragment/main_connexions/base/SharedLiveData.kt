package org.horizontal.tella.mobile.views.fragment.main_connexions.base

import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer

object SharedLiveData {
    val updateViewPagerPosition = SingleLiveEvent<Int>()
    val updateOutboxTitle = SingleLiveEvent<Int>()
    val updateSubmittedTitle = SingleLiveEvent<Int>()
    val refreshTokenServer = SingleLiveEvent<DropBoxServer>()
}