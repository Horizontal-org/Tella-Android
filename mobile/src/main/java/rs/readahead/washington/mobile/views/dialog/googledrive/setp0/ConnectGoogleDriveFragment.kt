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
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.googledrive.SharedGoogleDriveViewModel

const val EMAIL_KEY = "email_key"

@AndroidEntryPoint
class ConnectGoogleDriveFragment :
    BaseBindingFragment<FragmentConnectGoogleDriveBinding>(FragmentConnectGoogleDriveBinding::inflate) {
    private val sharedViewModel: SharedGoogleDriveViewModel by viewModels()

    companion object {
        const val GOOGLE_CLIENT_ID =
            "166289458819-e5nt7d2lahv55ld0j527o07kovqdbip2.apps.googleusercontent.com"
    }

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
        return GetSignInWithGoogleOption.Builder(GOOGLE_CLIENT_ID).build()
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
                navigateToSelectGoogleDriveFragment(it)
            }
        }
        sharedViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Extracted method to handle navigation
    private fun navigateToSelectGoogleDriveFragment(email: String) {
        val args = Bundle().apply {
            putString(EMAIL_KEY, email)
        }
        nav().navigate(
            R.id.action_googleDriveConnectFragment_to_selectGoogleDriveFragment,
            args
        )
    }
}


