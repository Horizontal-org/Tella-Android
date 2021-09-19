package rs.readahead.washington.mobile.views.activity.onboarding

interface OnBoardActivityInterface {
   //fun startSlides()
    fun setCurrentIndicator(index : Int)
    fun hideProgress()
    fun showProgress()
    fun initProgress(itemCount: Int)
    fun showChooseServerTypeDialog ()
}