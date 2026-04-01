package org.horizontal.tella.mobile.views.dialog.googledrive.step3

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentCreateFolderBinding
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.dialog.googledrive.SharedGoogleDriveViewModel
import org.horizontal.tella.mobile.views.dialog.IS_UPDATE_SERVER
import org.horizontal.tella.mobile.views.dialog.googledrive.setp0.OBJECT_KEY
import org.hzontal.shared_ui.utils.DialogUtils
import timber.log.Timber

@AndroidEntryPoint
class CreateFolderFragment : BaseBindingFragment<FragmentCreateFolderBinding>(
    FragmentCreateFolderBinding::inflate
), View.OnClickListener {

    private val sharedViewModel: SharedGoogleDriveViewModel by activityViewModels()
    private lateinit var googleDriveServer: GoogleDriveServer // for the update
    private lateinit var requestAuthorizationLauncher: ActivityResultLauncher<Intent>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAuthorizationLauncher()
        binding.nextBtn.setOnClickListener(this)
        binding.backBtn.setOnClickListener(this)

        arguments?.getString(OBJECT_KEY)?.let {
            googleDriveServer = Gson().fromJson(it, GoogleDriveServer::class.java)
            sharedViewModel.setEmail(googleDriveServer.username)
        }
        // Observe folder creation result
        sharedViewModel.folderCreated.observe(viewLifecycleOwner) { folderId ->
            Timber.d("Drive Folder ID: $folderId")
            googleDriveServer.folderId = folderId
            bundle.putString(OBJECT_KEY, Gson().toJson(googleDriveServer))
            bundle.putBoolean(
                IS_UPDATE_SERVER,
                arguments?.getBoolean(IS_UPDATE_SERVER, false) ?: false
            )
            navManager().navigateFromCreateFolderFragmentToGoogleDriveConnectedServerFragment()
        }

        // Observe errors (including create folder failures)
        sharedViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            Timber.d("Drive %s", errorMessage)
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        }
        sharedViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                DialogUtils.showBottomMessage(
                    baseActivity,
                    errorMessage,
                    true
                )
            }
        }

        // When Google Drive access was revoked (e.g. Tella removed from account), show re-auth and retry
        sharedViewModel.authorizationIntent.observe(viewLifecycleOwner) { intent ->
            intent?.let {
                requestAuthorizationLauncher.launch(it)
            }
        }
    }

    private fun setupAuthorizationLauncher() {
        requestAuthorizationLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    createFolder()
                } else {
                    DialogUtils.showBottomMessage(
                        baseActivity,
                        getString(R.string.google_drive_reconnect_sheet_title),
                        true
                    )
                }
            }
    }

    private fun createFolder() {
        val folderName = binding.createFolderEdit.text.toString()
        googleDriveServer.folderName = folderName
        if (folderName.isNotEmpty()) {
            sharedViewModel.createFolder(googleDriveServer)
        } else {
            DialogUtils.showBottomMessage(
                baseActivity,
                getString(R.string.Folder_Empty_Error),
                true
            )
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.next_btn -> {
                createFolder()
            }

            R.id.back_btn -> baseActivity.onBackPressed()
        }
    }
}
