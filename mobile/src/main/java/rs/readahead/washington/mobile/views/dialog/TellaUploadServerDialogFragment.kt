package rs.readahead.washington.mobile.views.dialog

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
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.google.android.material.textfield.TextInputLayout
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.DialogCollectServerBinding
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.mvp.contract.ICheckTUSServerContract
import rs.readahead.washington.mobile.mvp.presenter.CheckTUSServerPresenter
import timber.log.Timber

class TellaUploadServerDialogFragment : AppCompatDialogFragment(), ICheckTUSServerContract.IView {

    private lateinit var binding: DialogCollectServerBinding
    private var validated = true
    private var presenter: CheckTUSServerPresenter? = null
    private var securityProviderUpgradeAttempted = false
    private var serverId: Long = 0


    interface TellaUploadServerDialogHandler {
        fun onTellaUploadServerDialogCreate(server: TellaReportServer?)
        fun onTellaUploadServerDialogUpdate(server: TellaReportServer?)
        fun onDialogDismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let { arguments ->
            serverId = arguments.getLong(ID_KEY, 0)
        }
        binding = DialogCollectServerBinding.inflate(layoutInflater)
        val dialogView: View = inflater.inflate(R.layout.dialog_collect_server, container, false)
        presenter = CheckTUSServerPresenter(this)
        binding.toggleButton.setOnStateChangedListener { maybeShowAdvancedPanel() }
        maybeShowAdvancedPanel()
        binding.cancel.setOnClickListener { dismissDialog() }
        binding.back.setOnClickListener { dismissDialog() }
        binding.next.setOnClickListener {
            validate()
            if (validated) {
                checkServer(copyFields(TellaReportServer(serverId)), false)
            }
        }
        return dialogView
    }

    override fun onStart() {
        if (dialog == null) {
            return
        }
        dialog!!.window!!.setWindowAnimations(
            R.style.CollectDialogAnimation
        )
        super.onStart()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        // the content
        val root = RelativeLayout(activity)
        root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        if (activity == null) {
            return super.onCreateDialog(savedInstanceState)
        }
        val context = context ?: return super.onCreateDialog(savedInstanceState)

        // creating the fullscreen dialog
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(root)
        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.collect_server_dialog_layout_background,
                    null
                )
            )
            dialog.window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (presenter != null) {
            presenter!!.destroy()
            presenter = null
        }
    }

    override fun getContext(): Context? {
        return activity
    }

    override fun onServerCheckSuccess(server: TellaReportServer) {
        save(server)
    }

    override fun onServerCheckFailure(status: UploadProgressInfo.Status) {
        if (status == UploadProgressInfo.Status.UNAUTHORIZED) {
            if (binding.username.text.isNotEmpty() || binding.password.text.isNotEmpty()) {
                binding.toggleButton.setOpen()
                binding.usernameLayout.error =
                    getString(R.string.settings_docu_error_wrong_credentials)
            } else {
                binding.urlLayout.error =
                    getString(R.string.settings_docu_error_auth_required)
            }
        } else if (status == UploadProgressInfo.Status.UNKNOWN_HOST) {
            binding.urlLayout.error =
                getString(R.string.settings_docu_error_domain_doesnt_exit)
        } else {
            binding.urlLayout.error =
                getString(R.string.settings_docu_error_unknown_connection_error)
        }
        validated = false
    }

    override fun onServerCheckError(error: Throwable) {
        Toast.makeText(
            activity,
            getString(R.string.settings_docu_error_unknown_connection_error),
            Toast.LENGTH_LONG
        ).show()
        validated = false
    }

    override fun showServerCheckLoading() {
        binding.progressBar.visibility = View.VISIBLE
        setEnabledViews(false)
    }

    override fun hideServerCheckLoading() {
        setEnabledViews(true)
        binding.progressBar.visibility = View.GONE
    }

    override fun onNoConnectionAvailable() {
        Toast.makeText(
            activity,
            getString(R.string.settings_docu_error_no_internet),
            Toast.LENGTH_LONG
        ).show()
        binding.next.setText(R.string.settings_docu_dialog_action_try_again_connecting)
        validated = false
    }

    override fun setSaveAnyway(enabled: Boolean) {
        //dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(enabled ? View.VISIBLE : View.GONE);
        //dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(
        //        getString(enabled ? R.string.settings_dialog_action_save_server_no_internet : R.string.action_ok));
    }

    private fun validate() {
        validated = true
        validateRequired(binding.name, binding.nameLayout)
        validateUrl(binding.url, binding.urlLayout)
        validateRequired(binding.username, binding.usernameLayout)
        validateRequired(binding.password, binding.passwordLayout)

        // internetError.setVisibility(View.GONE);
    }

    private fun validateRequired(field: EditText?, layout: TextInputLayout?) {
        layout!!.error = null
        if (TextUtils.isEmpty(field!!.text.toString())) {
            layout.error = getString(R.string.settings_text_empty_field)
            validated = false
        }
    }

    private fun validateUrl(field: EditText?, layout: TextInputLayout?) {
        var url = field!!.text.toString()
        layout!!.error = null
        if (TextUtils.isEmpty(url)) {
            layout.error = getString(R.string.settings_text_empty_field)
            validated = false
        } else {
            url = url.trim { it <= ' ' }
            field.setText(url)
            if (!Patterns.WEB_URL.matcher(url).matches()) {
                layout.error = getString(R.string.settings_docu_error_not_valid_URL)
                validated = false
            }
        }
    }

    private fun checkServer(server: TellaReportServer, connectionRequired: Boolean) {
        // lets go with sync solution as this will not influence UX too much here
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1 &&
            !securityProviderUpgradeAttempted && context != null
        ) {
            try {
                ProviderInstaller.installIfNeeded(context)
            } catch (e: GooglePlayServicesRepairableException) {
                GoogleApiAvailability.getInstance()
                    .showErrorNotification(context, e.connectionStatusCode)
                securityProviderUpgradeAttempted = true
                return
            } catch (e: GooglePlayServicesNotAvailableException) {
                Timber.d(e)
            }
        }
        if (presenter != null) {
            presenter!!.checkServer(server, connectionRequired)
        }
    }

    private fun copyFields(server: TellaReportServer): TellaReportServer {
        server.name = binding.name.text.toString()
        server.url = binding.url.text.toString().trim { it <= ' ' }
        server.username = binding.username.text.toString().trim { it <= ' ' }
        server.password = binding.password.text.toString()
        return server
    }

    private fun save(server: TellaReportServer) {
        dismiss()
        val activity = activity as TellaUploadServerDialogHandler? ?: return
        if (server.id == 0L) {
            activity.onTellaUploadServerDialogCreate(server)
        } else {
            activity.onTellaUploadServerDialogUpdate(server)
        }
    }

    private fun setEnabledViews(enabled: Boolean) {
        binding.nameLayout.isEnabled = enabled
        binding.urlLayout.isEnabled = enabled
        binding.usernameLayout.isEnabled = enabled
        binding.passwordLayout.isEnabled = enabled
    }

    private fun onDialogDismiss() {
        val activity = activity as TellaUploadServerDialogHandler? ?: return
        activity.onDialogDismiss()
    }

    private fun dismissDialog() {
        dismiss()
        onDialogDismiss()
    }

    private fun maybeShowAdvancedPanel() {
        binding.advancedPanel.visibility =
            if (binding.toggleButton.isOpen) View.VISIBLE else View.GONE
    }

    companion object {
        @JvmField
        val TAG: String = TellaUploadServerDialogFragment::class.java.simpleName
        private const val TITLE_KEY = "tk"
        private const val ID_KEY = "ik"
        private const val OBJECT_KEY = "ok"

        @JvmStatic
        fun newInstance(server: TellaReportServer?): TellaUploadServerDialogFragment {
            val frag = TellaUploadServerDialogFragment()
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
}