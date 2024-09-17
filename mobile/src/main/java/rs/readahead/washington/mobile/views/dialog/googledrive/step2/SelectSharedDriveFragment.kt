package rs.readahead.washington.mobile.views.dialog.googledrive.step2

import StringListAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSelectSharedDriveBinding
import rs.readahead.washington.mobile.domain.entity.googledrive.Folder
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.googledrive.SharedGoogleDriveViewModel
import rs.readahead.washington.mobile.views.dialog.googledrive.setp0.OBJECT_KEY

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
                    // Handle item click
                    googleDriveServer.folderId = folder.folderId
                    googleDriveServer.folderName = folder.name
                    googleDriveServer.name = getString(R.string.google_drive)

                    Handler(Looper.getMainLooper()).postDelayed({
                        // Perform navigation after delay
                        bundle.putString(OBJECT_KEY, Gson().toJson(googleDriveServer))
                        navManager().navigateFromSelectSharedDriveFragmentToGoogleDriveConnectedServerFragment()
                    }, 500) // Delay in milliseconds (e.g., 2000 ms = 2 se
                }
            })
            binding.recyclerView.adapter = adapter
        }

        if (sharedViewModel.sharedDrives.value.isNullOrEmpty()) {
            sharedViewModel.fetchSharedDrives()
        }
    }

    private fun initRecyclerView() {
        // Initialize RecyclerView settings
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

}

