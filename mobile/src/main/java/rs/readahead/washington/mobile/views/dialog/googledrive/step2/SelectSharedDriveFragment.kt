package rs.readahead.washington.mobile.views.dialog.googledrive.step2

import SharedGoogleDriveViewModel
import StringListAdapter
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import rs.readahead.washington.mobile.databinding.FragmentSelectSharedDriveBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment

class SelectSharedDriveFragment :
    BaseBindingFragment<FragmentSelectSharedDriveBinding>(FragmentSelectSharedDriveBinding::inflate){

    private val sharedViewModel: SharedGoogleDriveViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()

        // Observe shared drives from ViewModel
        sharedViewModel.sharedDrives.observe(viewLifecycleOwner) { drives ->
            // Update RecyclerView with the new list of shared drives
            val adapter = StringListAdapter(drives, object : StringListAdapter.ItemClickListener {
                override fun onItemClick(item: String) {
                    // Handle item click
                    Toast.makeText(requireContext(), "Clicked: $item", Toast.LENGTH_SHORT).show()
                }
            })
            binding.recyclerView.adapter = adapter
        }

        // Fetch shared drives if not already loaded
        if (sharedViewModel.sharedDrives.value.isNullOrEmpty()) {
            sharedViewModel.fetchSharedDrives()
        }
    }

    private fun initRecyclerView() {
        // Initialize RecyclerView settings
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

}

