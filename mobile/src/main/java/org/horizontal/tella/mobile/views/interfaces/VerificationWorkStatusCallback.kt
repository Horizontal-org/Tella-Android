package org.horizontal.tella.mobile.views.interfaces

interface VerificationWorkStatusCallback {
    fun isBackgroundWorkInProgress(): Boolean
    fun showBackgroundWorkAlert()
    fun setBackgroundWorkStatus(inProgress: Boolean)
}