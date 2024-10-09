package rs.readahead.washington.mobile.views.dialog.googledrive.step2

import StringListAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSelectSharedDriveBinding
import rs.readahead.washington.mobile.domain.entity.googledrive.Folder
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.googledrive.SharedGoogleDriveViewModel
import rs.readahead.washington.mobile.views.dialog.googledrive.setp0.OBJECT_KEY
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.ReportsUtils

class SelectSharedDriveFragment :
    BaseBindingFragment<FragmentSelectSharedDriveBinding>(FragmentSelectSharedDriveBinding::inflate) {

    private val sharedViewModel: SharedGoogleDriveViewModel by activityViewModels()
    private lateinit var googleDriveServer: GoogleDriveServer // for the update

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        binding.toolbar.run { setStartTextTitle(context.getString(R.string.select_google_drive)) }
        binding.toolbar.backClickListener = { baseActivity.onBackPressed() }

        arguments?.getString(OBJECT_KEY)?.let {
            googleDriveServer = Gson().fromJson(it, GoogleDriveServer::class.java)
        }
        // Observe shared drives from ViewModel
        sharedViewModel.sharedDrives.observe(viewLifecycleOwner) { drives ->
            // Update RecyclerView with the new list of shared drives
            val adapter = StringListAdapter(drives, object : StringListAdapter.ItemClickListener {
                override fun onItemClick(folder: Folder) {
                    // Set folder details in the server object
                    googleDriveServer.folderId = folder.folderId
                    googleDriveServer.folderName = folder.name
                    googleDriveServer.name = getString(R.string.google_drive)

                    // Check folder permissions
                    sharedViewModel.checkFolderPermissions(
                        folder.folderId,
                        googleDriveServer.username
                    )
                }
            })
            binding.recyclerView.adapter = adapter
        }
        // Observe the permission result
        sharedViewModel.permissionResult.observe(viewLifecycleOwner) { hasAccess ->
            if (hasAccess) {
                // Proceed with uploading to the shared folder
                Handler(Looper.getMainLooper()).postDelayed({
                    bundle.putString(OBJECT_KEY, Gson().toJson(googleDriveServer))
                    navManager().navigateFromSelectSharedDriveFragmentToGoogleDriveConnectedServerFragment()
                }, 500)
            } else {
                // Show error message or alert that the user doesn't have permission
                activity?.let {
                    DialogUtils.showBottomMessage(
                        it,
                        getString(R.string.no_permission_to_folder_google_drive),
                        true
                    )
                }
            }
        }
        if (sharedViewModel.sharedDrives.value.isNullOrEmpty()) {
            sharedViewModel.fetchSharedDrives()
        }
    }

    private fun initRecyclerView() {
        // Initialize RecyclerView settings
        binding.recyclerView.setBackgroundColor(
            ContextCompat.getColor(baseActivity, R.color.wa_white_8)
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

}

