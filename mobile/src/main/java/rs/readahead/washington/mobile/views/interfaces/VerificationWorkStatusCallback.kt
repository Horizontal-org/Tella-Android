package rs.readahead.washington.mobile.views.interfaces

interface VerificationWorkStatusCallback {
    fun isBackgroundWorkInProgress(): Boolean
    fun showBackgroundWorkAlert()
    fun setBackgroundWorkStatus(inProgress: Boolean)
}