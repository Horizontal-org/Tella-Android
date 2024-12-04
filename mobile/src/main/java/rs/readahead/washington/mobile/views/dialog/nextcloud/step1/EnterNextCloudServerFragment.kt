package rs.readahead.washington.mobile.views.dialog.nextcloud.step1

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.owncloud.android.lib.common.network.CertificateCombinedException
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentEnterServerBinding
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.nextcloud.NextCloudLoginFlowViewModel
import rs.readahead.washington.mobile.views.dialog.nextcloud.sslalert.SslUntrustedCertDialog
import java.net.MalformedURLException
import java.net.URL

@AndroidEntryPoint
class EnterNextCloudServerFragment : BaseBindingFragment<FragmentEnterServerBinding>(
    FragmentEnterServerBinding::inflate
) {

    private val untrustedCertDialogTag = "UNTRUSTED_CERT_DIALOG"
    private val viewModel: NextCloudLoginFlowViewModel by viewModels()
    private val serverNextCloud: NextCloudServer by lazy { NextCloudServer() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        with(binding) {
            backBtn.setOnClickListener { baseActivity.finish() }
            nextBtn.setOnClickListener {
                val urlText = url.text.toString()
                if (isValidUrl(urlText)) {
                    serverNextCloud.url = urlText
                    viewModel.validateServerUrl(urlText)
                } else {
                    showErrorMessage(getString(R.string.Error_Connecting_To_Server_Msg))
                }
            }
        }
    }

    private fun isValidUrl(urlString: String): Boolean {
        return Patterns.WEB_URL.matcher(urlString).matches() && try {
            URL(urlString)
            true
        } catch (e: MalformedURLException) {
            false
        }
    }

    private fun setupObservers() {
        viewModel.isValidServer.observe(viewLifecycleOwner) { isValid ->
            if (isValid) {
                navigateToNextScreen()
            } else {
                showErrorMessage(getString(R.string.Error_Connecting_To_Server_Msg))
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { validationError ->
            validationError?.exception?.let { handleCertificateError(it) }
        }
    }

    private fun navigateToNextScreen() {
        bundle.putString(OBJECT_KEY, Gson().toJson(serverNextCloud))
        navManager().navigateToEnterNextCloudLoginScreen()
    }

    private fun showErrorMessage(message: String) {
        DialogUtils.showBottomMessage(baseActivity, message, false)
    }

    private fun handleCertificateError(exception: Throwable) {
        if (exception is CertificateCombinedException) {
            showUntrustedCertDialog(exception)
        }
    }

    private fun showUntrustedCertDialog(exception: CertificateCombinedException) {
        SslUntrustedCertDialog.newInstanceForFullSslError(exception, serverNextCloud.url).apply {
            show(
                baseActivity.supportFragmentManager.beginTransaction().addToBackStack(null),
                untrustedCertDialogTag
            )
        }
    }
}
