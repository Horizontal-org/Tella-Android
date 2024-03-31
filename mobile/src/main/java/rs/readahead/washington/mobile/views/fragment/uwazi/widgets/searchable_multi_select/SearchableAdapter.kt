package rs.readahead.washington.mobile.views.fragment.uwazi.widgets.searchable_multi_select

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.R
import java.util.*
import kotlin.collections.ArrayList

class SearchableAdapter(
    private val items: List<SearchableItem>,
    private var filteredList: List<SearchableItem>,
    private val itemClickListener: ItemClickListener,
    private val singleSelection: Boolean = false
) : Filterable, RecyclerView.Adapter<SearchableAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_multi_select_item, parent, false)
        return ViewHolder(view)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleTextView: TextView = view.findViewById(R.id.titleTextView)
        private val checkBox: CheckBox = view.findViewById(R.id.checkBox)

        init {
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                val position = this.adapterPosition
                val item = filteredList[position]
                item.isSelected = isChecked
                itemClickListener.onItemClicked(item, position, isChecked)
            }
            itemView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
            }
        }

        fun bind(item: SearchableItem) {
            titleTextView.text = item.text
            checkBox.isChecked = item.isSelected
            checkBox.visibility = if (singleSelection) View.GONE else View.VISIBLE
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString().lowercase(Locale.getDefault())
                filteredList = if (charString.isEmpty()) {
                    items
                } else {
                    items.filter { it.text.lowercase(Locale.getDefault()).contains(charString) }
                }
                return FilterResults().apply { values = filteredList }
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                filteredList = filterResults.values as ArrayList<SearchableItem>
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int = filteredList.size

    interface ItemClickListener {
        fun onItemClicked(item: SearchableItem, position: Int, isChecked: Boolean)
    }
}
