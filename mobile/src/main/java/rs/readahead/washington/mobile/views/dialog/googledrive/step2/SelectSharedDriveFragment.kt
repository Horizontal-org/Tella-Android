package rs.readahead.washington.mobile.views.dialog.googledrive.step2

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import rs.readahead.washington.mobile.databinding.FragmentSelectSharedDriveBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.googledrive.StringListAdapter

class SelectSharedDriveFragment :
    BaseBindingFragment<FragmentSelectSharedDriveBinding>(FragmentSelectSharedDriveBinding::inflate){

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()

    }
    private fun initRecyclerView() {
        // Get the RecyclerView from the binding
        val recyclerView = binding.recyclerView
     // Retrieve the Bundle arguments
        val sharedDrives = arguments?.getStringArrayList("shared_drives_key")
        // Initialize the adapter and pass the data
        val adapter = sharedDrives?.let {
            StringListAdapter(it, object : StringListAdapter.ItemClickListener {
                override fun onItemClick(item: String) {
                    // Handle item click
                    Toast.makeText(requireContext(), "Clicked: $item", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // Set the adapter to the RecyclerView
        recyclerView.adapter = adapter

        // Optionally set layout manager (e.g., LinearLayoutManager)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

}

