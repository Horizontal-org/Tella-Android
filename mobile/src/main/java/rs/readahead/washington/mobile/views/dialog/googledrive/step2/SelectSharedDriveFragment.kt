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

        // Create some sample data
        val items = listOf("Item 1", "Item 2", "Item 3", "Item 4")

        // Initialize the adapter and pass the data
        val adapter = StringListAdapter(items, object : StringListAdapter.ItemClickListener {
            override fun onItemClick(item: String) {
                // Handle item click
                Toast.makeText(requireContext(), "Clicked: $item", Toast.LENGTH_SHORT).show()
            }
        })

        // Set the adapter to the RecyclerView
        recyclerView.adapter = adapter

        // Optionally set layout manager (e.g., LinearLayoutManager)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

}

