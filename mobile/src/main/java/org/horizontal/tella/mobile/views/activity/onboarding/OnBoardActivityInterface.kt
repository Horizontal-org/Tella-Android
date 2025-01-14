package org.horizontal.tella.mobile.views.activity.onboarding

interface OnBoardActivityInterface {
   //fun startSlides()
    fun setCurrentIndicator(index : Int)
    fun hideProgress()
    fun showProgress()
    fun initProgress(itemCount: Int)
    fun showChooseServerTypeDialog ()
    fun enterCustomizationCode()
    fun initViewPager(itemCount: Int)
    fun enableSwipe(isSwipeable: Boolean,isTabLayoutVisible: Boolean)
    fun showButtons(isNextButtonVisible : Boolean,isBackButtonVisible: Boolean)
    fun hideViewpager()

}