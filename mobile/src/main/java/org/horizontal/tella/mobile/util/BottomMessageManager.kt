package org.horizontal.tella.mobile.util

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object BottomMessageManager {
    private val _events = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val events: SharedFlow<Int> = _events

    fun emit(@StringRes stringRes: Int) {
        _events.tryEmit(stringRes)
    }
}
