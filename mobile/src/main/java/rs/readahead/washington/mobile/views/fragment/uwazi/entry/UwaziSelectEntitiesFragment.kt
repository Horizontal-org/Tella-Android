package rs.readahead.washington.mobile.views.fragment.uwazi.entry

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.UwaziSelectEntitiesFragmentBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingDialogFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.searchable_multi_select.SearchableAdapter
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.searchable_multi_select.SearchableItem


class UwaziSelectEntitiesFragment :
    BaseBindingDialogFragment<UwaziSelectEntitiesFragmentBinding>(UwaziSelectEntitiesFragmentBinding::inflate) {
    private var items: MutableList<SearchableItem> = ArrayList()
    private lateinit var resultList: ArrayList<SearchableItem>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            STYLE_NORMAL,
            R.style.FullScreenDialogStyle
        );
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeData()
        initViews()
    }

    private fun initializeData() {
        items.addAll(
            listOf(
                SearchableItem("City Hall protest", "0"),
                SearchableItem("Highway accident", "1"),
                SearchableItem("BERSIH rally", "2"),
                SearchableItem("Miscellaneous incidents", "3"),
                SearchableItem("General Election - 2018", "4"),
                SearchableItem("Fake LGBT protest", "5"),
                SearchableItem("Red shirt rally", "6"),
                SearchableItem("Border crossing", "7"),
                SearchableItem("Seaport protest", "8")
            )
        )
    }

    private fun initViews() {
        val txtSearch: EditText =
            binding.searchView.findViewById(androidx.appcompat.R.id.search_src_text)
        txtSearch.setHintTextColor(Color.WHITE)
        txtSearch.setTextColor(Color.WHITE)

        val mLayoutManager = LinearLayoutManager(context)
        binding.recyclerView.apply {
            itemAnimator = null
            layoutManager = mLayoutManager
            adapter = setupAdapter()
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String) = false

            override fun onQueryTextChange(newText: String): Boolean {
                (binding.recyclerView.adapter as? SearchableAdapter)?.filter?.filter(newText)
                return false
            }
        })

        binding.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            binding.searchView.isSelected = hasFocus
            binding.searchView.isIconified = !hasFocus
        }

        binding.toolbar.apply {
            setRightIcon(R.drawable.ic_check_white)
            setRightIconVisibility(false)
            onRightClickListener = {
                (items.filter { it.isSelected }
                    .toMutableList() as ArrayList<SearchableItem>).also { resultList = it }
                nav().popBackStack()
            }
            backClickListener = { nav().popBackStack() }
        }
    }

    private fun setupAdapter(): SearchableAdapter {
        return SearchableAdapter(
            context = requireContext(),
            mValues = items,
            filteredList = items,
            clickListener = object : SearchableAdapter.ItemClickListener {
                override fun onItemClicked(item: SearchableItem, position: Int, b: Boolean) {
                    items.firstOrNull { it.code == item.code }?.isSelected = b
                    binding.toolbar.setRightIconVisibility(items.any { it.isSelected })
                }
            }, singleSelection = false
        )
    }

}
