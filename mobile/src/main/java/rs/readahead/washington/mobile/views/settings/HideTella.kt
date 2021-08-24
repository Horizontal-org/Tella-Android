package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.Navigation
import com.hzontal.tella_locking_ui.CALCULATOR_ALIAS
import org.hzontal.tella.keys.config.IUnlockRegistryHolder
import org.hzontal.tella.keys.config.UnlockRegistry
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.CamouflageAliasChangedEvent
import rs.readahead.washington.mobile.presentation.entity.CamouflageOption
import rs.readahead.washington.mobile.util.CamouflageManager
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import timber.log.Timber


class HideTella : BaseFragment() {

    private val cm = CamouflageManager.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_hide_tella, container, false)

        initView(view)

        return view
    }

    override fun initView(view: View) {
        (activity as OnFragmentSelected?)?.hideAppbar()

        val btnOneDesc = view.findViewById<TextView>(R.id.subtitle_btn_one)
        btnOneDesc.setText(Html.fromHtml(getString(R.string.settings_servers_setup_change_name_icon_subtitle)))

        val btnTwoDesc = view.findViewById<TextView>(R.id.subtitle_btn_two)
        btnTwoDesc.setText(Html.fromHtml(getString(R.string.settings_servers_setup_hide_behind_calculator_subtitle)))

        val hideNotPossible = view.findViewById<TextView>(R.id.hide_behind_calc_not_possible)
        hideNotPossible.setText(Html.fromHtml(getString(R.string.settings_servers_setup_hide_behind_calculator_not_possible)))

        view.findViewById<LinearLayout>(R.id.sheet_one_btn).setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_hide_tella_to_camouflage_name_logo)
        }

        val btnTwoLabel = view.findViewById<LinearLayout>(R.id.sheet_two_btn_label)
        val btnTwo = view.findViewById<LinearLayout>(R.id.sheet_two_btn)

        if ((activity.getApplicationContext() as IUnlockRegistryHolder).unlockRegistry.getActiveMethod(activity) != UnlockRegistry.Method.TELLA_PIN) {
            hideNotPossible.visibility = View.VISIBLE
            hideNotPossible.setOnClickListener {
                Navigation.findNavController(view).navigate(R.id.action_hide_tella_to_security_settings)
            }
            btnTwoLabel.setAlpha(0.38f)
            btnTwo.setClickable(false)
        } else {
            hideNotPossible.visibility = View.GONE
            btnTwoLabel.setAlpha(1f)
            btnTwo.setClickable(true)
            btnTwo.setOnClickListener {
                hideTellaBehindCalculator()
            }
        }

        view.findViewById<View>(R.id.back_btn).setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_hide_tella_to_security_settings)
        }
    }

    private fun hideTellaBehindCalculator(){
        if (cm.setLauncherActivityAlias(requireContext(), CALCULATOR_ALIAS)) {
            MyApplication.bus()
                .post(CamouflageAliasChangedEvent())
        }
    }
}