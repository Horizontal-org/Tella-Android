package rs.readahead.washington.mobile.views.dialog.nextcloud.step2

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentLoginScreenBinding
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.nextcloud.INextCloudAuthFlow
import rs.readahead.washington.mobile.views.dialog.nextcloud.NextCloudLoginFlowViewModel
import timber.log.Timber

@AndroidEntryPoint
class LoginNextCloudFragment : BaseBindingFragment<FragmentLoginScreenBinding>(
    FragmentLoginScreenBinding::inflate
) {
    private val viewModel by activityViewModels<NextCloudLoginFlowViewModel>()
    private lateinit var serverNextCloud: NextCloudServer
    private var validated = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListeners()
        initObservers()
    }

    fun initView() {
        arguments?.getString(OBJECT_KEY)?.let {
            serverNextCloud = Gson().fromJson(it, NextCloudServer::class.java)
        }
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
                    startRefresh(binding.username.text.toString(), binding.password.text.toString())

                }
            }
        }
        binding.backBtn.setOnClickListener { baseActivity.onBackPressed() }
    }


    private fun validate() {
        validated = true
        validateRequired(binding.username, binding.usernameLayout)
        validateRequired(binding.password, binding.passwordLayout)
    }

    private fun validateRequired(field: EditText?, layout: TextInputLayout?) {
        layout?.error = null
        if (TextUtils.isEmpty(field!!.text.toString())) {
            layout?.error = getString(R.string.settings_text_empty_field)
            validated = false
        }
    }

    @SuppressLint("TimberArgCount")
    private fun initObservers() {

        viewModel.errorUserNamePassword.observe(viewLifecycleOwner) {
            validateRequired(binding.password, binding.passwordLayout)
            binding.passwordLayout.error = getString(R.string.settings_docu_error_wrong_credentials)
        }

        viewModel.progress.observe(viewLifecycleOwner) { isVisible ->
            binding.progressBar.isVisible = isVisible
        }
    }

    private fun startRefresh(userName: String, password: String) {
        viewModel.progress.postValue(true)
        (activity as? INextCloudAuthFlow)?.onStartRefreshLogin(
            serverNextCloud.url,
            userName,
            password
        )
    }

}