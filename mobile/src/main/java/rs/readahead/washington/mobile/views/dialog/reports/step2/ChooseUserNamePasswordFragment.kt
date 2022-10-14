package rs.readahead.washington.mobile.views.dialog.reports.step2

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentChooseUsernamePasswordBinding
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.ID_KEY
import rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.TITLE_KEY
import rs.readahead.washington.mobile.views.dialog.reports.step3.LoginReportsFragment
import rs.readahead.washington.mobile.views.dialog.uwazi.step2.LoginTypeFragment

class ChooseUserNamePasswordFragment :
    BaseBindingFragment<FragmentChooseUsernamePasswordBinding>(FragmentChooseUsernamePasswordBinding::inflate),
    View.OnClickListener {
    private var server: TellaReportServer? = null

    companion object {
        val TAG = LoginTypeFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            server: TellaReportServer): ChooseUserNamePasswordFragment {
            val frag = ChooseUserNamePasswordFragment()
            val args = Bundle()
            args.putString(OBJECT_KEY, Gson().toJson(server))
            frag.arguments = args
            return frag
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListeners()
    }

    private fun initListeners() {
        binding?.yesBtn?.setOnClickListener {
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

