package org.horizontal.tella.mobile.views.dialog.googledrive.setp0

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.databinding.FragmentConnectGoogleDriveBinding
import org.horizontal.tella.mobile.domain.entity.googledrive.Config
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.dialog.googledrive.SharedGoogleDriveViewModel
import javax.inject.Inject

const val OBJECT_KEY = "ok"

/**
 * Stub implementation of ConnectGoogleDriveFragment for F-Droid builds.
 * 
 * This class exists to satisfy compile-time dependencies but shows an error
 * message since Google Drive is not available in F-Droid builds.
 */
@AndroidEntryPoint
class ConnectGoogleDriveFragment :
    BaseBindingFragment<FragmentConnectGoogleDriveBinding>(FragmentConnectGoogleDriveBinding::inflate) {
    private val sharedViewModel: SharedGoogleDriveViewModel by viewModels()
    @Inject lateinit var config: Config

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Toast.makeText(
            requireContext(),
            "Google Drive is not available in F-Droid builds",
            Toast.LENGTH_LONG
        ).show()
        // Navigate back or close
        baseActivity.onBackPressed()
    }

    private fun copyFields(server: GoogleDriveServer): GoogleDriveServer {
        // Stub implementation
        return server
    }
}




