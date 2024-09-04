package rs.readahead.washington.mobile.views.dialog.googledrive.step1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSelectGoogleDriveBinding
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import timber.log.Timber

class FragmentSelectGoogleDriveFragment :
    BaseBindingFragment<FragmentSelectGoogleDriveBinding>(FragmentSelectGoogleDriveBinding::inflate),
    View.OnClickListener {
    private var server: TellaReportServer? = null
    private lateinit var requestAuthorizationLauncher: ActivityResultLauncher<Intent>
    private lateinit var driveService: Drive

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        //   initListeners()

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
                val request = driveService.Files().list()
                val result = request.execute()
                val sharedDrives = result.files

                if (sharedDrives != null && sharedDrives.isNotEmpty()) {
//                    val navHostFragment =
//                        supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//                    val navController = navHostFragment.navController
                    // navController.navigate(R.id.google_drive)
                } else {
                    Timber.d("No shared drives found.")
                }
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

        if (arguments == null) return

        arguments?.getString(OBJECT_KEY)?.let {
            server = Gson().fromJson(it, TellaReportServer::class.java)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.back_btn -> {
                baseActivity.onBackPressed()
            }

            R.id.next_btn -> {
                validateAndLogin()
            }
        }
    }

    private fun validateAndLogin() {
        if (server == null) return
        if (binding?.createFolderBtn?.isChecked == true) {

        } else {

        }
    }
}

