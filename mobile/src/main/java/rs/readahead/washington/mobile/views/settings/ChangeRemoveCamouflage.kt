package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.Navigation
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.util.CamouflageManager
import rs.readahead.washington.mobile.views.base_ui.BaseFragment


class ChangeRemoveCamouflage : BaseFragment() {

    private val cm = CamouflageManager.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_change_remove_camouflage, container, false)

        initView(view)

        return view
    }

    override fun initView(view: View) {
        (activity as OnFragmentSelected?)?.showAppbar()
        (activity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_prot_select_camouflage)

        val fragmentTitle = view.findViewById<TextView>(R.id.title)
        fragmentTitle.text = getString(R.string.settings_servers_add_camouflage_subtitle, cm.getLauncherName(requireContext()))

        view.findViewById<View>(R.id.change_method).setOnClickListener {
            activity.addFragment(HideTella(), R.id.my_nav_host_fragment)
        }

        view.findViewById<View>(R.id.remove_camouflage).setOnClickListener {
            removeCamouflage()
        }
    }

    private fun  removeCamouflage() {
        cm.setDefaultLauncherActivityAlias(requireContext())
    }
}