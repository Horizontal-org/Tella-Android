package rs.readahead.washington.mobile.views.settings

interface OnFragmentSelected {
    fun setToolbarLabel(labelRes: Int)
    fun setToolbarHomeIcon(iconRes: Int)
    fun isCamouflage(): Boolean
    fun hideAppbar()
    fun showAppbar()
}