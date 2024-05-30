package rs.readahead.washington.mobile.views.settings

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import org.hzontal.tella.keys.config.IUnlockRegistryHolder
import org.hzontal.tella.keys.config.UnlockRegistry
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentHideTellaBinding
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.activity.SettingsActivity
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.base_ui.BaseFragment


class HideTella : BaseBindingFragment<FragmentHideTellaBinding>(FragmentHideTellaBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    fun initView() {
        val btnOneDesc = binding.subtitleBtnOne
        btnOneDesc.text =
            Html.fromHtml(getString(R.string.settings_servers_setup_change_name_icon_subtitle))

        val btnTwoDesc = binding.subtitleBtnTwo
        btnTwoDesc.text =
            Html.fromHtml(getString(R.string.settings_servers_setup_hide_behind_calculator_subtitle))

        val hideNotPossible = binding.hideBehindCalcNotPossible
        hideNotPossible.text =
            Html.fromHtml(getString(R.string.settings_servers_setup_hide_behind_calculator_not_possible))

        binding.sheetOneBtn.setOnClickListener {
            baseActivity.addFragment(CamouflageNameAndLogo(), R.id.my_nav_host_fragment)
        }

        val btnTwoLabel = binding.sheetTwoBtnLabel
        val btnTwo = binding.sheetTwoBtn

        if ((baseActivity.applicationContext as IUnlockRegistryHolder).unlockRegistry.getActiveMethod(
                baseActivity
            ) != UnlockRegistry.Method.TELLA_PIN
        ) {
            hideNotPossible.visibility = View.VISIBLE
            hideNotPossible.setOnClickListener {
                baseActivity.addFragment(SecuritySettings(), R.id.my_nav_host_fragment)
            }
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
        handleOnBackPressed()
    }

    private fun handleOnBackPressed() {
        binding.backBtn.setOnClickListener {
            (activity as? SettingsActivity)?.onBackPressed()
        }
        binding.toolbar.backClickListener = {
            (activity as? SettingsActivity)?.onBackPressed()
        }
        (activity as SettingsActivity).onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    (activity as? SettingsActivity)?.onBackPressed()
                }
            })
    }
}