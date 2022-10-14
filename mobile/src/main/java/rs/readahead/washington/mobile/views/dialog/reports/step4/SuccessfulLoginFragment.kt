package rs.readahead.washington.mobile.views.dialog.reports.step4

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSuccessfulLoginBinding
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.ID_KEY
import rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.reports.step5.ServerAdvancedSettingsFragment

class SuccessfulLoginFragment : BaseBindingFragment<FragmentSuccessfulLoginBinding>(
    FragmentSuccessfulLoginBinding::inflate
) {
    private lateinit var server: TellaReportServer

    companion object {
        val TAG: String = SuccessfulLoginFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            server: TellaReportServer
        ): SuccessfulLoginFragment {
            val frag = SuccessfulLoginFragment()
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

    private fun initView() {
        if (arguments == null) return

        arguments?.getString(OBJECT_KEY)?.let {
            server = Gson().fromJson(it, TellaReportServer::class.java)
        }
    }

    private fun initListeners() {
        binding?.goToAdvancedSettingsBtn?.setOnClickListener {
            binding?.goToAdvancedSettingsBtn?.isChecked = true
            binding?.goToReportsBtn?.isChecked = false
            baseActivity.addFragment(ServerAdvancedSettingsFragment.newInstance(server), R.id.container)
        }

        binding?.goToReportsBtn?.setOnClickListener {
            binding?.goToAdvancedSettingsBtn?.isChecked = false
            binding?.goToReportsBtn?.isChecked = true
        }
    }

}