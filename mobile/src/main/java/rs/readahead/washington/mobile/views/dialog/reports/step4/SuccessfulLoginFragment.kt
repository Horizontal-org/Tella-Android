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
import rs.readahead.washington.mobile.views.dialog.TITLE_KEY

class SuccessfulLoginFragment : BaseBindingFragment<FragmentSuccessfulLoginBinding>(
    FragmentSuccessfulLoginBinding::inflate
) {
    private var isUpdate = false
    private var server: TellaReportServer? = null

    companion object {
        val TAG: String = SuccessfulLoginFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            server: TellaReportServer,
            isUpdate: Boolean
        ): SuccessfulLoginFragment {
            val frag = SuccessfulLoginFragment()
            val args = Bundle()
            args.putInt(TITLE_KEY, R.string.settings_docu_dialog_title_server_settings)
            args.putSerializable(ID_KEY, server.id)
            args.putString(OBJECT_KEY, Gson().toJson(server))
            args.putBoolean(IS_UPDATE_SERVER, isUpdate)
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
        arguments?.getBoolean(IS_UPDATE_SERVER)?.let {
            isUpdate = it
        }
    }

    private fun initListeners() {
        binding?.goToAdvancedSettingsBtn?.setOnClickListener {
            binding?.goToAdvancedSettingsBtn?.isChecked = true
            binding?.goToReportsBtn?.isChecked = false
        }

        binding?.goToReportsBtn?.setOnClickListener {
            binding?.goToAdvancedSettingsBtn?.isChecked = false
            binding?.goToReportsBtn?.isChecked = true
        }
    }

}