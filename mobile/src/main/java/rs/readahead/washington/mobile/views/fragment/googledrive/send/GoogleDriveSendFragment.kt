package rs.readahead.washington.mobile.views.fragment.googledrive.send

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.googledrive.GoogleDriveViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsSendFragment
import rs.readahead.washington.mobile.views.fragment.reports.ReportsViewModel
import java.io.File

@AndroidEntryPoint
class GoogleDriveSendFragment : BaseReportsSendFragment() {

    override val viewModel by viewModels<GoogleDriveViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.uploadResult.observe(viewLifecycleOwner) { result ->
            // Handle the result here, e.g., show a Toast or update UI
            Toast.makeText(context, "Upload Result: $result", Toast.LENGTH_SHORT).show()
        }

        // Trigger file upload (localFile, folderId, title, and description should be provided)
        val localFile = createTempImageFile() // Create the local file
        val title = "My Image Title"
        val description = "This is a description of the image"

        viewModel.uploadFile(localFile, title, description)

    }

    private fun createTempImageFile(): java.io.File {
        // Create a temporary file in the app's cache directory
        val tempFile = File.createTempFile("temp_image", ".jpg")

        // Optionally, you can write some data to the file for testing
        tempFile.writeText("This is a temporary image file for testing.")

        return tempFile
    }

    override fun navigateBack() {
        if (isFromOutbox) {
            nav().popBackStack()
        } else {
            nav().popBackStack(R.id.newReportScreen, true)
        }
    }


}