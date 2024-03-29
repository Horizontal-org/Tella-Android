package rs.readahead.washington.mobile.views.fragment.uwazi.entry

import android.os.Bundle
import android.view.View
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            STYLE_NORMAL,
            R.style.FullScreenDialogStyle
        );
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        items.add(SearchableItem("City Hall protest", "0"))
        items.add(SearchableItem("Highway accident", "1"))
        items.add(SearchableItem("BERSIH rally", "2"))
        items.add(SearchableItem("Miscellaneous incidents", "3"))
        items.add(SearchableItem("General Election - 2018", "4"))
        items.add(SearchableItem("Fake LGBT protest", "5"))
        items.add(SearchableItem("Red shirt rally", "6"))
        items.add(SearchableItem("Border crossing", "7"))
        items.add(SearchableItem("Seaport protest", "8"))

        binding.toolbar.backClickListener = {
            nav().popBackStack()
        }

        val mLayoutManager = LinearLayoutManager(context)
        val adapter =
            SearchableAdapter(
                requireContext(),
                items,
                items,
                object : SearchableAdapter.ItemClickListener {
                    override fun onItemClicked(
                        item: SearchableItem,
                        position: Int,
                        b: Boolean
                    ) {
                        for (i in items.indices) {
                            if (items[i].code == item.code) {
                                items[i].isSelected = b
                                break
                            }
                        }
                    }

                }, false
            )
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.layoutManager = mLayoutManager
        binding.recyclerView.adapter = adapter

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                // do something on text submit
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // do something when text changes
                adapter.filter.filter(newText)
                return false
            }
        })

    }

}
