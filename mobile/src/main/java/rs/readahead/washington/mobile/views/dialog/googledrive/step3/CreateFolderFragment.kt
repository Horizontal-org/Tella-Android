package rs.readahead.washington.mobile.views.dialog.googledrive.step3

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentCreateFolderBinding
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.googledrive.SharedGoogleDriveViewModel
import rs.readahead.washington.mobile.views.dialog.googledrive.setp0.OBJECT_KEY

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

        if (arguments == null) return
        arguments?.getString(OBJECT_KEY)?.let {
            googleDriveServer = Gson().fromJson(it, GoogleDriveServer::class.java)
            sharedViewModel.setEmail(googleDriveServer.username)
        }
        // Observe folder creation result
        sharedViewModel.folderCreated.observe(viewLifecycleOwner) { folderId ->
            Log.d("Drive", "Folder ID: $folderId")
            googleDriveServer.folderId = folderId
            bundle.putSerializable(OBJECT_KEY, googleDriveServer)
            navManager().navigateFromCreateFolderFragmentToGoogleDriveConnectedServerFragment()
        }

        // Observe errors
        sharedViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            Log.e("Drive", errorMessage)
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
