package org.horizontal.tella.mobile.views.dialog.uwazi.step4

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import org.hzontal.shared_ui.bottomsheet.KeyboardUtil
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentTwoFactorAuthenticationBinding
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.util.KeyboardLiveData
import org.horizontal.tella.mobile.views.base_ui.BaseFragment
import org.horizontal.tella.mobile.views.dialog.ID_KEY
import org.horizontal.tella.mobile.views.dialog.IS_UPDATE_SERVER
import org.horizontal.tella.mobile.views.dialog.OBJECT_KEY
import org.horizontal.tella.mobile.views.dialog.TITLE_KEY
import org.horizontal.tella.mobile.views.dialog.uwazi.UwaziConnectFlowViewModel
import org.horizontal.tella.mobile.views.dialog.uwazi.step3.LoginFragment
import org.horizontal.tella.mobile.views.dialog.uwazi.step5.LanguageFragment

class TwoFactorAuthenticationFragment : BaseFragment() {
    private var validated = true
    private lateinit var binding: FragmentTwoFactorAuthenticationBinding
    private val viewModel: UwaziConnectFlowViewModel by viewModels()
    private lateinit var serverUwazi: UWaziUploadServer
    private var isUpdate = false

    companion object {
        val TAG = LoginFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            server: UWaziUploadServer,
            isUpdate: Boolean
        ): TwoFactorAuthenticationFragment {
            val frag = TwoFactorAuthenticationFragment()
            val args = Bundle()

            args.putInt(TITLE_KEY, R.string.settings_docu_dialog_title_server_settings)
            args.putSerializable(ID_KEY, server.id)
            args.putString(OBJECT_KEY, Gson().toJson(server))
            args.putBoolean(IS_UPDATE_SERVER, isUpdate)

            frag.arguments = args
            return frag
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTwoFactorAuthenticationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        initObservers()
    }

    private fun initListeners() {
        binding.loginButton.setOnClickListener {
            if (!MyApplication.isConnectedToInternet(baseActivity)) {
                DialogUtils.showBottomMessage(
                    baseActivity,
                    getString(R.string.settings_docu_error_no_internet),
                    true
                )
            } else {
                validate()
                if (validated) {
                    viewModel.checkServer(copyFields(UWaziUploadServer(0)))
                }
            }
        }
        binding.backBtn.setOnClickListener {
            baseActivity.onBackPressed()
        }
    }

    private fun initObservers() {
        with(viewModel) {

            authenticationError.observe(viewLifecycleOwner) { error ->
                if (error) {
                    binding.passwordLayout.error = getString(R.string.Inavlid_Token_Msg_Error)
                }
            }

            authenticationSuccess.observe(viewLifecycleOwner) { isSuccess ->
                if (isSuccess) {
                    KeyboardUtil.hideKeyboard(baseActivity,binding.root)
                    baseActivity.addFragment(
                        LanguageFragment.newInstance(serverUwazi, isUpdate),
                        R.id.container
                    )
                }
            }

            progress.observe(viewLifecycleOwner) {
                binding.progressBar.isVisible = it
            }
        }
    }

    private fun copyFields(server: UWaziUploadServer): UWaziUploadServer {
        server.url = serverUwazi.url
        server.username = serverUwazi.username
        server.password = serverUwazi.password
        server.token = binding.password.text.toString()
        serverUwazi = server
        return server
    }

    private fun validate() {
        validated = true
        validateRequired(binding.password, binding.passwordLayout)
    }

    private fun validateRequired(field: EditText?, layout: TextInputLayout?) {
        layout!!.error = null
        if (TextUtils.isEmpty(field!!.text.toString())) {
            layout.error = getString(R.string.settings_text_empty_field)
            validated = false
        }
    }

    override fun initView(view: View) {
        arguments?.getString(OBJECT_KEY)?.let {
            serverUwazi = Gson().fromJson(it, UWaziUploadServer::class.java)
        }
        arguments?.getBoolean(IS_UPDATE_SERVER)?.let {
            isUpdate = it
        }

        KeyboardLiveData(binding.root).observe(this) {
            binding.backBtn.isVisible = !it.first
        }

    }

}