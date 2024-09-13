package rs.readahead.washington.mobile.views.dialog.googledrive.step2

import StringListAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSelectSharedDriveBinding
import rs.readahead.washington.mobile.domain.entity.googledrive.Folder
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.googledrive.SharedGoogleDriveViewModel

class SelectSharedDriveFragment :
    BaseBindingFragment<FragmentSelectSharedDriveBinding>(FragmentSelectSharedDriveBinding::inflate) {

    private val sharedViewModel: SharedGoogleDriveViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        binding.toolbar.setStartTextTitle("Select Google drive")
        binding.toolbar.backClickListener = { baseActivity.onBackPressed() }

        // Observe shared drives from ViewModel
        sharedViewModel.sharedDrives.observe(viewLifecycleOwner) { drives ->
            // Update RecyclerView with the new list of shared drives
            val adapter = StringListAdapter(drives, object : StringListAdapter.ItemClickListener {
                override fun onItemClick(folder: Folder) {
                    // Handle item click
                    sharedViewModel.saveSelectedFolder(folder)
                }
            })
            binding.recyclerView.adapter = adapter
        }
        sharedViewModel.successMessage.observe(viewLifecycleOwner, Observer { message ->
            message?.let {
                // Introduce a delay before navigating
                Handler(Looper.getMainLooper()).postDelayed({
                    // Hide the ImageView or revert UI changes if needed
                    // Perform navigation after delay
                    findNavController().navigate(
                        R.id.action_selectSharedDriveFragment_to_googleDriveConnectedServerFragment
                    )
                }, 500) // Delay in milliseconds (e.g., 2000 ms = 2 seconds)
            }
        })
        if (sharedViewModel.sharedDrives.value.isNullOrEmpty()) {
            sharedViewModel.fetchSharedDrives()
        }
    }

    private fun initRecyclerView() {
        // Initialize RecyclerView settings
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

}

