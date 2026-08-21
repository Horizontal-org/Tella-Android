package org.horizontal.tella.mobile.views.dialog.nextcloud.step2

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.KeyboardUtil
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentLoginScreenBinding
import org.horizontal.tella.mobile.domain.entity.nextcloud.NextCloudServer
import org.horizontal.tella.mobile.util.KeyboardLiveData
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.dialog.OBJECT_KEY
import org.horizontal.tella.mobile.views.dialog.nextcloud.INextCloudAuthFlow
import org.horizontal.tella.mobile.views.dialog.nextcloud.NextCloudLoginFlowViewModel

@AndroidEntryPoint
class LoginNextCloudFragment : BaseBindingFragment<FragmentLoginScreenBinding>(
    FragmentLoginScreenBinding::inflate
) {
    private val viewModel by activityViewModels<NextCloudLoginFlowViewModel>()
    private lateinit var serverNextCloud: NextCloudServer
    private var isValidated = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeView()
        setupListeners()
        observeViewModel()
    }

    private fun initializeView() {
        arguments?.getString(OBJECT_KEY)?.let {
            serverNextCloud = Gson().fromJson(it, NextCloudServer::class.java)
        }
        KeyboardUtil(binding.root)
    }

    private fun setupListeners() {
        binding.loginButton.setOnClickListener { handleLoginButtonClick() }
        binding.backBtn.setOnClickListener { baseActivity.onBackPressed()}

        KeyboardLiveData(binding.root).observe(viewLifecycleOwner) {
            binding.backBtn.isVisible = !it.first
        }
    }

    private fun handleLoginButtonClick() {
        if (!MyApplication.isConnectedToInternet(baseActivity)) {
            showBottomMessage(getString(R.string.settings_docu_error_no_internet))
        } else {
            validateFields()
            if (isValidated) {
                initiateLogin(binding.username.text.toString(), binding.password.text.toString())
            }
        }
    }

    private fun showBottomMessage(message: String) {
        DialogUtils.showBottomMessage(baseActivity, message, true)
    }

    private fun validateFields() {
        isValidated = true
        validateField(binding.username, binding.usernameLayout)
        validateField(binding.password, binding.passwordLayout)
    }

    private fun validateField(field: EditText, layout: TextInputLayout) {
        layout.error = null
        if (TextUtils.isEmpty(field.text.toString())) {
            layout.error = getString(R.string.settings_text_empty_field)
            isValidated = false
        }
    }

    @SuppressLint("TimberArgCount")
    private fun observeViewModel() {
        viewModel.errorUserNamePassword.observe(viewLifecycleOwner) {
            displayCredentialError()
        }

        viewModel.progress.observe(viewLifecycleOwner) { isVisible ->
            binding.progressBar.isVisible = isVisible
        }

        viewModel.successLoginToServer.observe(viewLifecycleOwner) { credentials ->
            updateServerDetails(credentials)
            navManager().navigateToNextCloudCreateFolderScreen()
        }
    }

    private fun displayCredentialError() {
        binding.passwordLayout.error = getString(R.string.settings_docu_error_wrong_credentials)
    }

    private fun updateServerDetails(server: NextCloudServer) {
        serverNextCloud.apply {
            username = server.username
            password = server.password
            userId = server.userId
            url = server.url
        }
        bundle.putString(OBJECT_KEY, Gson().toJson(serverNextCloud))
    }

    private fun initiateLogin(userName: String, password: String) {
        viewModel.progress.postValue(true)
        (activity as? INextCloudAuthFlow)?.onStartRefreshLogin(
            serverNextCloud.url,
            userName,
            password
        )
    }
}
