package org.horizontal.tella.mobile.views.fragment.peertopeer

/**
 * Created by wafa on 23/6/2025.
 */
class Event<out T>(private val content: T) {
    private var hasBeenHandled = false

    /** Returns the content if not handled, else null */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) null
        else {
            hasBeenHandled = true
            content
        }
    }

    /** Always returns the content, even if already handled */
    fun peekContent(): T = content
}