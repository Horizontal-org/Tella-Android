package rs.readahead.washington.mobile.views.dialog.googledrive.step3

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hzontal.shared_ui.bottomsheet.KeyboardUtil
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentCreateFolderBinding
import rs.readahead.washington.mobile.databinding.FragmentEnterServerBinding
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.ConnectFlowUtils.validateUrl
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.reports.ReportsConnectFlowViewModel
import rs.readahead.washington.mobile.views.dialog.reports.step3.OBJECT_SLUG
import timber.log.Timber
import java.lang.Exception

@AndroidEntryPoint
class CreateFolderFragment : BaseBindingFragment<FragmentCreateFolderBinding>(
    FragmentCreateFolderBinding::inflate
), View.OnClickListener {
    private lateinit var driveService: Drive

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val email = arguments?.getString("email_key").toString()
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
        binding.nextBtn.setOnClickListener(this)
    }

    private fun createFolder() {
        val folderMetadata = File()
        folderMetadata.name = binding.createFolderEdit.toString()
        folderMetadata.mimeType = "application/vnd.google-apps.folder"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val folder = driveService.files().create(folderMetadata)
                    .setFields("id")
                    .execute()
                findNavController().navigate(
                    R.id.action_createFolderFragment_to_googleDriveConnectedServerFragment
                )

                Log.d("Drive", "Folder ID: ${folder.id}")
            } catch (e: Exception) {
            }
        }

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.next_btn -> {
                createFolder()
            }
        }
    }
}