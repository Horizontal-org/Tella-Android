package org.horizontal.tella.mobile.views.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.http.HttpStatus
import org.horizontal.tella.mobile.databinding.DialogCollectServerBinding
import org.horizontal.tella.mobile.domain.entity.IErrorBundle
import org.horizontal.tella.mobile.domain.entity.collect.CollectServer
import org.horizontal.tella.mobile.mvvm.odk.CheckOdkServerViewModel
import org.hzontal.shared_ui.buttons.PanelToggleButton
import timber.log.Timber
import java.net.UnknownHostException

@AndroidEntryPoint
class CollectServerDialogFragment : AppCompatDialogFragment() {

    private lateinit var binding: DialogCollectServerBinding

    private val viewModel: CheckOdkServerViewModel by viewModels()

    private var securityProviderUpgradeAttempted = false

    lateinit var nameLayout: TextInputLayout
    lateinit var name: EditText
    lateinit var urlLayout: TextInputLayout
    lateinit var url: EditText
    lateinit var usernameLayout: TextInputLayout
    lateinit var username: EditText
    lateinit var passwordLayout: TextInputLayout
    lateinit var password: EditText
    lateinit var progressBar: ProgressBar
    lateinit var serverInput: View
    lateinit var cancel: TextView
    lateinit var next: TextView
    lateinit var back: ImageView
    lateinit var advancedToggle: PanelToggleButton
    lateinit var advancedPanel: ViewGroup

    private var context: Context? = null

    private var validated = true

    interface CollectServerDialogHandler {
        fun onCollectServerDialogCreate(server: CollectServer)
        fun onCollectServerDialogUpdate(server: CollectServer)
        fun onDialogDismiss()
    }

    companion object {
        const val TAG = "CollectServerDialogFragment"
        private const val TITLE_KEY = "tk"
        private const val ID_KEY = "ik"
        private const val OBJECT_KEY = "ok"

        @JvmStatic
        fun newInstance(server: CollectServer? = null): CollectServerDialogFragment {
            val frag = CollectServerDialogFragment()
            val args = Bundle()
            if (server == null) {
                args.putInt(TITLE_KEY, R.string.settings_servers_add_server_dialog_title)
            } else {
                args.putInt(TITLE_KEY, R.string.settings_docu_dialog_title_server_settings)
                args.putSerializable(ID_KEY, server.id)
                args.putSerializable(OBJECT_KEY, server)
            }
            frag.arguments = args
            return frag
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val serverId = arguments?.getLong(ID_KEY, 0) ?: 0
        val server = arguments?.getSerializable(OBJECT_KEY) as? CollectServer

        binding = DialogCollectServerBinding.inflate(inflater, container, false)
        context = requireContext()

        initView()
        observeLiveData()
        if (server != null) {
            name.setText(server.name)
            url.setText(server.url)
            if (server.name.isNotEmpty() && server.password.isNotEmpty()) {
                advancedToggle.setOpen()
                username.setText(server.username)
                password.setText(server.password)
            }
        }

        advancedToggle.setOnStateChangedListener { open -> maybeShowAdvancedPanel() }
        maybeShowAdvancedPanel()

        cancel.setOnClickListener { dismissDialog() }
        back.setOnClickListener { dismissDialog() }
        next.setOnClickListener {
            validate()
            if (validated) {
                checkServer(copyFields(CollectServer(serverId)), false)
            }
        }

        return binding.root
    }

    private fun observeLiveData() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Handle loading state - show or hide progress bar
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            setEnabledViews(!isLoading)
        }

        viewModel.serverCheckSuccess.observe(viewLifecycleOwner) { server ->
            // Handle success - save the server or update the UI
            save(server)
        }

        viewModel.serverCheckFailure.observe(viewLifecycleOwner) { errorBundle ->
            // Handle failure - show the specific error based on the error code
            handleServerCheckFailure(errorBundle)
        }

        viewModel.serverCheckError.observe(viewLifecycleOwner) {
            // Handle error - show general error message
            handleServerCheckError()
        }

        viewModel.noConnectionAvailable.observe(viewLifecycleOwner) {
            Toast.makeText(
                activity,
                getString(R.string.settings_docu_error_no_internet),
                Toast.LENGTH_LONG
            ).show()
            next.setText(R.string.settings_docu_dialog_action_try_again_connecting)
            validated = false
        }
    }


    private fun handleServerCheckFailure(error: IErrorBundle) {
        when (error.code) {
            HttpStatus.UNAUTHORIZED_401 -> {
                if (usernameLayout.editText?.text?.isNotEmpty() == true || passwordLayout.editText?.text?.isNotEmpty() == true) {
                    advancedToggle.setOpen()
                    usernameLayout.error = getString(R.string.settings_docu_error_wrong_credentials)
                } else {
                    urlLayout.error = getString(R.string.settings_docu_error_auth_required)
                }
            }

            else -> {
                urlLayout.error = when (error.exception) {
                    is UnknownHostException -> getString(R.string.settings_docu_error_domain_doesnt_exit)
                    else -> getString(R.string.settings_docu_error_unknown_connection_error)
                }
            }
        }
        validated = false
    }

    private fun initView() {
        nameLayout = binding.nameLayout
        name = binding.name
        urlLayout = binding.urlLayout
        url = binding.url
        usernameLayout = binding.usernameLayout
        username = binding.username
        passwordLayout = binding.passwordLayout
        password = binding.password
        progressBar = binding.progressBar
        serverInput = binding.serverInput
        cancel = binding.cancel
        next = binding.next
        back = binding.back
        advancedToggle = binding.toggleButton
        advancedPanel = binding.advancedPanel
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setWindowAnimations(R.style.CollectDialogAnimation)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val root = RelativeLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val dialog = Dialog(requireActivity()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(root)
            window?.apply {
                setBackgroundDrawable(context.resources.getDrawable(R.drawable.collect_server_dialog_layout_background))
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
        }
        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.onCleared()
    }

    private fun checkServer(server: CollectServer, connectionRequired: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1 && !securityProviderUpgradeAttempted) {
            try {
                // Handle security provider installation if needed
            } catch (e: Exception) {
                Timber.d(e)
            }
        }
        viewModel.checkServer(server, connectionRequired)
    }

    private fun validate() {
        validated = true
        validateRequired(name, nameLayout)
        validateUrl(url, urlLayout)
    }

    private fun validateRequired(field: EditText, layout: TextInputLayout) {
        layout.error = null
        if (TextUtils.isEmpty(field.text.toString())) {
            layout.error = getString(R.string.settings_text_empty_field)
            validated = false
        }
    }

    private fun validateUrl(field: EditText, layout: TextInputLayout) {
        val url = field.text.toString().trim()
        layout.error = null
        if (url.isEmpty()) {
            layout.error = getString(R.string.settings_text_empty_field)
            validated = false
        } else if (!Patterns.WEB_URL.matcher(url).matches()) {
            layout.error = getString(R.string.settings_docu_error_not_valid_URL)
            validated = false
        }
    }

    private fun maybeShowAdvancedPanel() {
        advancedPanel.visibility = if (advancedToggle.isOpen) View.VISIBLE else View.GONE
    }


    private fun handleServerCheckError() {
        Toast.makeText(
            activity,
            getString(R.string.settings_docu_error_unknown_connection_error),
            Toast.LENGTH_LONG
        ).show()
        validated = false
    }

    private fun setEnabledViews(enabled: Boolean) {
        nameLayout.isEnabled = enabled
        urlLayout.isEnabled = enabled
        usernameLayout.isEnabled = enabled
        passwordLayout.isEnabled = enabled
    }

    private fun save(server: CollectServer) {
        dismiss()
        val activity = activity as? CollectServerDialogHandler ?: return
        if (server.id == 0L) {
            activity.onCollectServerDialogCreate(server)
        } else {
            activity.onCollectServerDialogUpdate(server)
        }
    }

    private fun dismissDialog() {
        dismiss()
        (activity as? CollectServerDialogHandler)?.onDialogDismiss()
    }

    @NonNull
    private fun copyFields(server: CollectServer): CollectServer {
        server.name = name.text.toString()
        server.url = url.text.toString().trim()
        server.username = username.text.toString().trim()
        server.password = password.text.toString()
        return server
    }

}
