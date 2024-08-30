package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment


class ChooseServerTypeFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.choose_server_type_layout, container, false)
        (activity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_servers_add_server_dialog_title)

        return view
    }

    override fun initView(view: View) {
        TODO("Not yet implemented")
    }


}