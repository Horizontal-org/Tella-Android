package rs.readahead.washington.mobile.views.dialog.googledrive.step1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSelectGoogleDriveBinding
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import timber.log.Timber

class SelectGoogleDriveFragment :
    BaseBindingFragment<FragmentSelectGoogleDriveBinding>(FragmentSelectGoogleDriveBinding::inflate),
    View.OnClickListener {
    private var server: TellaReportServer? = null
    private lateinit var email : String
    private lateinit var requestAuthorizationLauncher: ActivityResultLauncher<Intent>
    private lateinit var driveService: Drive
    private var sharedDrives: List<String>? = null // Data to pass to the next fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        // Retrieve the email from the Intent
         email = arguments?.getString("email_key").toString()
        val googleAccountCredential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE)
        ).apply {
            selectedAccountName = email
        }

        // Initialize the Drive service
        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            googleAccountCredential
        ).setApplicationName("Tella").build()
        fetchSharedDrives(driveService)

        requestAuthorizationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                // Retry accessing Google Drive after authorization
                if (::driveService.isInitialized) {
                    fetchSharedDrives(driveService)
                } else {
                    Timber.e("Drive service is not initialized")
                }
            } else {
                // Handle the case where the user denies the authorization
                Timber.e("Authorization denied by user.")
            }
        }
    }

    private fun fetchSharedDrives(driveService: Drive) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
               // val request = driveService.Files().list()
                // Query to list folders shared with the user
                val query = "mimeType = 'application/vnd.google-apps.folder' and sharedWithMe = true"
                val request = driveService.files().list().setQ(query).setFields("files(id, name)")
                val result = request.execute()
                sharedDrives = result.files.map { it.name } // Adjust this based on your data model

//                if (sharedDrives != null && sharedDrives!!.isNotEmpty()) {
//                    binding.sharedDriveBtn.isEnabled = true
//                } else {
//                    binding.sharedDriveBtn.isEnabled = false
//                    Timber.d("No shared drives found.")
//                }
            } catch (e: UserRecoverableAuthIOException) {
                requestAuthorizationLauncher.launch(e.intent)
                Timber.e(e, "Failed to fetch shared drives.")
            }
        }
    }

//        if (sharedDrives != null && sharedDrives.isNotEmpty()) {
//
//        } else {
//            Timber.d("No shared drives found.")
//        }

    private fun initView() {
        binding.learnMoreTextView.setOnClickListener(this)
        binding.backBtn.setOnClickListener (this)
        binding.createFolderBtn.setOnClickListener(this)
        binding.sharedDriveBtn.setOnClickListener(this)
        binding.nextBtn.setOnClickListener (this)

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.back_btn -> {
                baseActivity.onBackPressed()
            }

            R.id.learn_more_textView -> {
                val args = Bundle().apply {
                    putString("email_key", email)
                }
                findNavController().navigate(
                    R.id.action_selectGoogleDriveFragment_to_createFolderFragment,
                    args
                )
            }

            R.id.next_btn -> {
                sharedDrives?.let {
                    // Create a Bundle to pass data
                    val args = Bundle().apply {
                        putStringArrayList("shared_drives_key", ArrayList(it))
                    }

                    // Navigate with the Bundle
                    findNavController().navigate(
                        R.id.action_selectGoogleDriveFragment_to_selectSharedDriveFragment,
                        args
                    )
                } ?: run {
                    Timber.d("No shared drives data to pass.")
                }
            }
            R.id.create_folder_btn -> {
                sharedDrives?.let {
                    // Create a Bundle to pass data
                    val args = Bundle().apply {
                        putStringArrayList("shared_drives_key", ArrayList(it))
                    }

                    // Navigate with the Bundle
                    findNavController().navigate(
                        R.id.action_selectGoogleDriveFragment_to_selectSharedDriveFragment,
                        args
                    )
                } ?: run {
                    Timber.d("No shared drives data to pass.")
                }
            }
        }
    }

//    private fun validateAndLogin() {
//        if (server == null) return
//        if (binding?.createFolderBtn?.isChecked == true) {
//
//        } else {
//
//        }
   // }
}

