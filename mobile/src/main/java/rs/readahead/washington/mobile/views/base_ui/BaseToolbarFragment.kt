package rs.readahead.washington.mobile.views.base_ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import rs.readahead.washington.mobile.views.settings.OnFragmentSelected

abstract class BaseToolbarFragment : BaseFragment() , OnFragmentSelected{

    abstract fun setUpToolbar()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onBackPressed()
    }


    private fun onBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(
            activity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    nav().navigateUp()
                }
            }
        )
    }
}