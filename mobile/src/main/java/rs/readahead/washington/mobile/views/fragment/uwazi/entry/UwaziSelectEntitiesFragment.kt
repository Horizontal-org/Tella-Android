package rs.readahead.washington.mobile.views.fragment.uwazi.entry

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.UwaziSelectEntitiesFragmentBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingDialogFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.searchable_multi_select.SearchableAdapter
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.searchable_multi_select.SearchableItem


class UwaziSelectEntitiesFragment :
    BaseBindingDialogFragment<UwaziSelectEntitiesFragmentBinding>(UwaziSelectEntitiesFragmentBinding::inflate) {
    private var items: MutableList<SearchableItem> = ArrayList()
    private lateinit var resultList: ArrayList<SearchableItem>
    private val uwaziParser: UwaziParser by lazy { UwaziParser(context) }

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
        if (arguments?.getString(UWAZI_TEMPLATE) != null) {
            arguments?.getString(UWAZI_TEMPLATE)?.let {
                uwaziParser.parseTemplateForRelationShipEntities(it)
            }
        }

        if (arguments?.getString(UWAZI_ENTRY_PROMPT_ID) != null) {
            val id = arguments?.getString(UWAZI_ENTRY_PROMPT_ID)
        val property = uwaziParser.getTemplate()?.entityRow?.properties?.find { property ->  property._id == id}
            var i = 0
            property?.entities?.forEach { value ->
                    items.add(SearchableItem(value.label,i.toString()))
                    i++
                }
            }
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
                val bundle = Bundle()
                val jsonResultList = Gson().toJson(resultList.map { it.text })
                bundle.putString("resultListJson", jsonResultList)
                setFragmentResult(
                    "RELATIONSHIP",
                       bundle
                    )
                nav().popBackStack()
            }
            backClickListener = { nav().popBackStack() }
        }
    }

    private fun setupAdapter(): SearchableAdapter {
        return SearchableAdapter(
            items = items,
            filteredList = items,
            itemClickListener = object : SearchableAdapter.ItemClickListener {
                override fun onItemClicked(item: SearchableItem, position: Int, b: Boolean) {
                    items.firstOrNull { it.code == item.code }?.isSelected = b
                    binding.toolbar.setRightIconVisibility(items.any { it.isSelected })
                }
            }, singleSelection = false
        )
    }

}
