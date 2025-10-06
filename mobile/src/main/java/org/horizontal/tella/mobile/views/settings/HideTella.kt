package org.horizontal.tella.mobile.views.settings

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.hzontal.tella.keys.config.IUnlockRegistryHolder
import org.hzontal.tella.keys.config.UnlockRegistry
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.base_ui.BaseFragment


class HideTella : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_hide_tella, container, false)

        initView(view)

        return view
    }

    override fun initView(view: View) {
        (baseActivity as OnFragmentSelected?)?.showAppbar()
        (baseActivity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_servers_hide_tella_title)

        val btnOneDesc = view.findViewById<TextView>(R.id.subtitle_btn_one)
        btnOneDesc.text = Html.fromHtml(getString(R.string.settings_servers_setup_change_name_icon_subtitle))

        val btnTwoDesc = view.findViewById<TextView>(R.id.subtitle_btn_two)
        btnTwoDesc.text = Html.fromHtml(getString(R.string.settings_servers_setup_hide_behind_calculator_subtitle))

        val hideNotPossible = view.findViewById<TextView>(R.id.hide_behind_calc_not_possible)
        hideNotPossible.text = Html.fromHtml(getString(R.string.settings_servers_setup_hide_behind_calculator_not_possible))

        view.findViewById<LinearLayout>(R.id.sheet_one_btn).setOnClickListener {
            baseActivity.addFragment(CamouflageNameAndLogo(), R.id.my_nav_host_fragment)
        }

        val btnTwoLabel = view.findViewById<LinearLayout>(R.id.sheet_two_btn_label)
        val btnTwo = view.findViewById<LinearLayout>(R.id.sheet_two_btn)

        if ((baseActivity.getApplicationContext() as IUnlockRegistryHolder).unlockRegistry.getActiveMethod(baseActivity) != UnlockRegistry.Method.TELLA_PIN) {
            hideNotPossible.visibility = View.VISIBLE
            hideNotPossible.setOnClickListener {
                baseActivity.addFragment(SecuritySettings(), R.id.my_nav_host_fragment)
            }
            btnTwoLabel.setAlpha(0.65f)
            btnTwo.isClickable = false
            btnTwo.setOnClickListener { }
        } else {
            hideNotPossible.visibility = View.GONE
            btnTwoLabel.setAlpha(1f)
            btnTwo.isClickable = true
            btnTwo.setOnClickListener {
                val intent = Intent(baseActivity, SettingsCalculatorActivity::class.java)
                baseActivity.startActivity(intent)
            }
        }

        view.findViewById<View>(R.id.back_btn).setOnClickListener {
            (baseActivity as OnFragmentSelected?)?.showAppbar()
            baseActivity.onBackPressed()
        }
    }
}