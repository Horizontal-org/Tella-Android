package rs.readahead.washington.mobile.views.dialog.nextcloud.step1

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.owncloud.android.lib.common.network.CertificateCombinedException
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentEnterServerBinding
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.nextcloud.NextCloudLoginFlowViewModel
import rs.readahead.washington.mobile.views.dialog.nextcloud.SslUntrustedCertDialog
import java.net.MalformedURLException
import java.net.URL

@AndroidEntryPoint
class EnterNextCloudServerFragment : BaseBindingFragment<FragmentEnterServerBinding>(
    FragmentEnterServerBinding::inflate
) {

    private val untrustedCertDialogTag = "UNTRUSTED_CERT_DIALOG"
    private val viewModel: NextCloudLoginFlowViewModel by viewModels()
    private val serverNextCloud: NextCloudServer by lazy {
        NextCloudServer()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObservers()
    }

    fun initView() {
        with(binding) {
            backBtn.setOnClickListener {
                baseActivity.finish()
            }
            nextBtn.setOnClickListener {
                if (isValidUrl(url.text.toString())) {
                    serverNextCloud.url = url.text.toString()
                    viewModel.validateServerUrl(url.text.toString())
                } else {
                    DialogUtils.showBottomMessage(
                        baseActivity,
                        getString(R.string.Error_Connecting_To_Server_Msg),
                        false
                    )
                }
            }
        }
    }

    private fun isValidUrl(urlString: String): Boolean {
        // First, use Patterns to check if it matches a web URL pattern
        if (!Patterns.WEB_URL.matcher(urlString).matches()) {
            return false
        }

        // Then, try to create a URL object to check if it's a well-formed URL
        return try {
            URL(urlString)
            true
        } catch (e: MalformedURLException) {
            false
        }
    }

    private fun initObservers() {
        viewModel.isValidServer.observe(viewLifecycleOwner) { isValid ->
            if (isValid) {
                bundle.putString(OBJECT_KEY, Gson().toJson(serverNextCloud))
                navManager().navigateToEnterNextCloudLoginScreen()
            } else {
               DialogUtils.showBottomMessage(
                    baseActivity,
                    getString(R.string.Error_Connecting_To_Server_Msg),
                    false
                )
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { validationError ->
            validationError?.let {
                it.exception?.let { it1 -> showUntrustedCertDialog(it1) }
            }
        }
    }


    /**
     * Show dialog for untrusted certificate.
     */
    private fun showUntrustedCertDialog(exception:Throwable) {
        if (exception is CertificateCombinedException) {
            val dialog = SslUntrustedCertDialog.newInstanceForFullSslError(exception,serverNextCloud.url)
            val fm = baseActivity.supportFragmentManager
            val ft = fm.beginTransaction()
            ft.addToBackStack(null)
            dialog.show(ft, untrustedCertDialogTag)
        }
    }
}