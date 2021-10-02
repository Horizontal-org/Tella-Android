package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hzontal.tella_locking_ui.CALCULATOR_ALIAS
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.CamouflageAliasChangedEvent
import rs.readahead.washington.mobile.util.CamouflageManager
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class HideBehindCalculator: BaseFragment()  {

    private val cm = CamouflageManager.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.onboard_calculator_fragment, container, false)

        initView(view)

        return view
    }

    override fun initView(view: View) {
        (activity as OnFragmentSelected?)?.hideAppbar()

        view.findViewById<View>(R.id.back_btn).setOnClickListener {
            activity.onBackPressed()
        }

        view.findViewById<View>(R.id.calculatorBtn).setOnClickListener {
            hideTellaBehindCalculator()
        }
    }

    private fun hideTellaBehindCalculator(){
        if (cm.setLauncherActivityAlias(requireContext(), CALCULATOR_ALIAS)) {
            MyApplication.bus()
                .post(CamouflageAliasChangedEvent())
        }
    }
}