package rs.readahead.washington.mobile.views.activity.onboarding

interface OnBoardActivityInterface {
   //fun startSlides()
    fun setCurrentIndicator(index : Int)
    fun hideProgress()
    fun showProgress()
    fun initProgress(itemCount: Int)
    fun showChooseServerTypeDialog ()
    fun enterCustomizationCode()
    fun enableSwipe(isSwipeable: Boolean,isTabLayoutVisible: Boolean)
    fun showButtons(isNextButtonVisible : Boolean,isBackButtonVisible: Boolean)
}