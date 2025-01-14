package org.horizontal.tella.mobile.views.dialog.googledrive.setp0

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.credentials.GetCredentialRequest
import androidx.fragment.app.viewModels
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.databinding.FragmentConnectGoogleDriveBinding
import org.horizontal.tella.mobile.domain.entity.googledrive.Config
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.dialog.googledrive.SharedGoogleDriveViewModel
import timber.log.Timber
import javax.inject.Inject

const val OBJECT_KEY = "ok"

@AndroidEntryPoint
class ConnectGoogleDriveFragment :
    BaseBindingFragment<FragmentConnectGoogleDriveBinding>(FragmentConnectGoogleDriveBinding::inflate) {
    private val sharedViewModel: SharedGoogleDriveViewModel by viewModels()
    private lateinit var googleDriveServer: GoogleDriveServer // for the update
    @Inject lateinit var config: Config
    private val server by lazy { GoogleDriveServer(googleClientId = config.googleClientId) }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val request = createCredentialRequest()
        baseActivity.maybeChangeTemporaryTimeout {
            sharedViewModel.signInWithGoogle(request, requireContext())
        }
        Timber.d("config " + config.googleClientId)
        observeViewModel()
    }

    // Extracted method to create Google Sign-In option
    private fun getGoogleSignInOption(): GetSignInWithGoogleOption {
        Timber.d("server " + server.googleClientId)
        return GetSignInWithGoogleOption.Builder(server.googleClientId).build()
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
                server.username = email
                bundle.putString(OBJECT_KEY, Gson().toJson(server))
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
        navManager().navigateFromGoogleDriveConnectFragmentToSelectGoogleDriveFragment()
    }

    private fun copyFields(server: GoogleDriveServer): GoogleDriveServer {
        googleDriveServer.folderName = server.folderName
        googleDriveServer.folderId = server.folderId
        googleDriveServer.username = server.username
        return server
    }
}


