package rs.readahead.washington.mobile.views.dialog.googledrive.step3

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentCreateFolderBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.googledrive.SharedGoogleDriveViewModel

@AndroidEntryPoint
class CreateFolderFragment : BaseBindingFragment<FragmentCreateFolderBinding>(
    FragmentCreateFolderBinding::inflate
), View.OnClickListener {

    private val viewModel: SharedGoogleDriveViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nextBtn.setOnClickListener(this)
        binding.backBtn.setOnClickListener(this)

        // Observe folder creation result
        viewModel.folderCreated.observe(viewLifecycleOwner) { folderId ->
            Log.d("Drive", "Folder ID: $folderId")
            findNavController().navigate(
                R.id.action_createFolderFragment_to_googleDriveConnectedServerFragment
            )
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            Log.e("Drive", errorMessage)
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createFolder() {
        val folderName = binding.createFolderEdit.text.toString()
        if (folderName.isNotEmpty()) {
            viewModel.createFolder(folderName)
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
