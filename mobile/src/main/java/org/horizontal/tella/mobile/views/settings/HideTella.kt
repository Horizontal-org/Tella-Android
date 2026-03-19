package org.horizontal.tella.mobile.views.settings

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Html
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.base_ui.BaseFragment
import org.hzontal.tella.keys.config.IUnlockRegistryHolder
import org.hzontal.tella.keys.config.UnlockRegistry


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
        val linkText = getString(R.string.settings_hide_tella_change_lock_here)
        val fullText = getString(R.string.settings_hide_tella_change_lock_to_pin_hint, linkText)
        val spannable = SpannableString(fullText)
        val linkStart = fullText.indexOf(linkText)
        if (linkStart >= 0) {
            val linkEnd = linkStart + linkText.length
            val orange = ContextCompat.getColor(requireContext(), R.color.wa_orange)
            spannable.setSpan(ForegroundColorSpan(orange), linkStart, linkEnd, 0)
            spannable.setSpan(StyleSpan(Typeface.BOLD), linkStart, linkEnd, 0)
            spannable.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    baseActivity.addFragment(SecuritySettings(), R.id.my_nav_host_fragment)
                }
            }, linkStart, linkEnd, 0)
        }
        hideNotPossible.text = spannable
        hideNotPossible.movementMethod = LinkMovementMethod.getInstance()

        view.findViewById<LinearLayout>(R.id.sheet_one_btn).setOnClickListener {
            baseActivity.addFragment(CamouflageNameAndLogo(), R.id.my_nav_host_fragment)
        }

        val btnTwoLabel = view.findViewById<LinearLayout>(R.id.sheet_two_btn_label)
        val btnTwo = view.findViewById<LinearLayout>(R.id.sheet_two_btn)

        if ((baseActivity.applicationContext as IUnlockRegistryHolder).unlockRegistry.getActiveMethod(baseActivity) != UnlockRegistry.Method.TELLA_PIN) {
            hideNotPossible.visibility = View.VISIBLE
            btnTwoLabel.alpha = 0.65f
            btnTwo.isClickable = false
            btnTwo.setOnClickListener { }
        } else {
            hideNotPossible.visibility = View.GONE
            btnTwoLabel.alpha = 1f
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