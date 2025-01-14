package org.horizontal.tella.mobile.views.dialog.googledrive.step3

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentCreateFolderBinding
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.dialog.googledrive.SharedGoogleDriveViewModel
import org.horizontal.tella.mobile.views.dialog.googledrive.setp0.OBJECT_KEY
import timber.log.Timber

@AndroidEntryPoint
class CreateFolderFragment : BaseBindingFragment<FragmentCreateFolderBinding>(
    FragmentCreateFolderBinding::inflate
), View.OnClickListener {

    private val sharedViewModel: SharedGoogleDriveViewModel by viewModels()
    private lateinit var googleDriveServer: GoogleDriveServer // for the update

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            navManager().navigateFromCreateFolderFragmentToGoogleDriveConnectedServerFragment()
        }

        // Observe errors
        sharedViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            Timber.d("Drive %s", errorMessage)
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createFolder() {
        val folderName = binding.createFolderEdit.text.toString()
        googleDriveServer.folderName = folderName
        if (folderName.isNotEmpty()) {
            sharedViewModel.createFolder(googleDriveServer)
        } else {
            Toast.makeText(requireContext(), "Folder name cannot be empty", Toast.LENGTH_SHORT)
                .show()
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
