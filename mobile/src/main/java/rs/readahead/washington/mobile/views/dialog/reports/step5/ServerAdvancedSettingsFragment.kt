package rs.readahead.washington.mobile.views.dialog.reports.step5

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentReportServerAdvancedSettingsBinding
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.ID_KEY
import rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.reports.step6.SuccessfulSetServerFragment

@AndroidEntryPoint
class ServerAdvancedSettingsFragment :
    BaseBindingFragment<FragmentReportServerAdvancedSettingsBinding>(
        FragmentReportServerAdvancedSettingsBinding::inflate
    ) {
    private var isUpdate = false
    private lateinit var server: TellaReportServer

    companion object {
        val TAG: String = ServerAdvancedSettingsFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            server: TellaReportServer,
            isUpdate: Boolean
        ): ServerAdvancedSettingsFragment {
            val frag = ServerAdvancedSettingsFragment()
            val args = Bundle()
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
        binding?.backBtn?.setOnClickListener {
            baseActivity.onBackPressed()
        }
        binding?.nextBtn?.setOnClickListener {
            baseActivity.addFragment(
                SuccessfulSetServerFragment.newInstance(server, isUpdate),
                R.id.container
            )
        }

        binding?.backgroundUploadSwitch?.mSwitch?.setOnCheckedChangeListener { _, isChecked: Boolean ->
            server.isActivatedBackgroundUpload = isChecked
        }

        binding?.shareVerificationSwitch?.mSwitch?.setOnCheckedChangeListener { _, isChecked: Boolean ->
            server.isActivatedMetadata = isChecked
        }

    }

}