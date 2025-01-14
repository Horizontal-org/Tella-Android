package org.horizontal.tella.mobile.views.dialog.reports.step2

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentChooseUsernamePasswordBinding
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.dialog.OBJECT_KEY

class ChooseUserNamePasswordFragment :
    BaseBindingFragment<FragmentChooseUsernamePasswordBinding>(FragmentChooseUsernamePasswordBinding::inflate),
    View.OnClickListener {
    private var server: TellaReportServer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListeners()
    }

    private fun initListeners() {
        binding.yesBtn.setOnClickListener {
            binding?.yesBtn?.isChecked = true
            binding?.noBtn?.isChecked = false
        }
        binding?.noBtn?.setOnClickListener {
            binding?.yesBtn?.isChecked = false
            binding?.noBtn?.isChecked = true
        }
        binding?.backBtn?.setOnClickListener(this)
        binding?.nextBtn?.setOnClickListener(this)
    }

    private fun initView() {

        if (arguments == null) return

        arguments?.getString(OBJECT_KEY)?.let {
            server = Gson().fromJson(it, TellaReportServer::class.java)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.yes_btn -> {

            }

            R.id.no_btn -> {

            }

            R.id.back_btn -> {
                baseActivity.onBackPressed()
            }

            R.id.next_btn -> {
                validateAndLogin()
            }
        }
    }

    private fun validateAndLogin() {
        if (server == null) return
        if (binding?.yesBtn?.isChecked == true) {

        } else {

        }
    }
}

