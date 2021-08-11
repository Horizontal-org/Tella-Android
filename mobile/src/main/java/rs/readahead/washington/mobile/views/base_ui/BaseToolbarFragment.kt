package rs.readahead.washington.mobile.views.base_ui

import rs.readahead.washington.mobile.views.settings.OnFragmentSelected

abstract class BaseToolbarFragment : BaseFragment() , OnFragmentSelected{

    abstract fun setUpToolbar()
}