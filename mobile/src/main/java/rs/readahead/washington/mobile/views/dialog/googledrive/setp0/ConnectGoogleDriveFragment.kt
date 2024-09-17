package rs.readahead.washington.mobile.views.dialog.googledrive.setp0

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.credentials.GetCredentialRequest
import androidx.fragment.app.viewModels
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentConnectGoogleDriveBinding
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.googledrive.SharedGoogleDriveViewModel

const val EMAIL_KEY = "email_key"
const val OBJECT_KEY = "ok"

@AndroidEntryPoint
class ConnectGoogleDriveFragment :
    BaseBindingFragment<FragmentConnectGoogleDriveBinding>(FragmentConnectGoogleDriveBinding::inflate) {
    private val sharedViewModel: SharedGoogleDriveViewModel by viewModels()
    private lateinit var googleDriveServer: GoogleDriveServer // for the update
   // private val server by lazy { GoogleDriveServer() }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val request = createCredentialRequest()
        baseActivity.maybeChangeTemporaryTimeout {
            sharedViewModel.signInWithGoogle(request, requireContext())
        }
        observeViewModel()
    }

    // Extracted method to create Google Sign-In option
    private fun getGoogleSignInOption(): GetSignInWithGoogleOption {
        return GetSignInWithGoogleOption.Builder(googleDriveServer.googleClientId).build()
    }

    // Extracted method to create credential request
    private fun createCredentialRequest(): GetCredentialRequest {
        val signInWithGoogleOption = getGoogleSignInOption()
        return GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()
    }

    // Extracted method to observe LiveData from ViewModel
    private fun observeViewModel() {
        sharedViewModel.email.observe(viewLifecycleOwner) { email ->
            email?.let {
                googleDriveServer.username = email
                bundle.putSerializable(OBJECT_KEY, googleDriveServer)
                navigateToSelectGoogleDriveFragment()
            }
        }
        sharedViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Extracted method to handle navigation
    private fun navigateToSelectGoogleDriveFragment() {
        bundle.putSerializable(OBJECT_KEY, googleDriveServer)
        navManager().navigateFromGoogleDriveConnectFragmentToSelectGoogleDriveFragment()
    }

    private fun copyFields(server: GoogleDriveServer): GoogleDriveServer {
        googleDriveServer.folderName = server.folderName
        googleDriveServer.folderId = server.folderId
        googleDriveServer.username = server.username
        return server
    }
}


