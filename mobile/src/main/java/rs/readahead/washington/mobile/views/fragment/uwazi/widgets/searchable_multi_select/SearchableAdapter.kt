package rs.readahead.washington.mobile.views.fragment.uwazi.widgets.searchable_multi_select

import android.content.Context
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

class SearchableAdapter(
    internal var context: Context,
    private val mValues: List<SearchableItem>,
    private var filteredList: List<SearchableItem>,
    clickListener: ItemClickListener,

    private var singleSelection: Boolean = false
) : Filterable, RecyclerView.Adapter<SearchableAdapter.ViewHolder>() {
    private var itemClickListener: ItemClickListener = clickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_multi_select_item, parent, false)
        return ViewHolder(view)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var mItem: SearchableItem? = null
        val titleTextView: TextView = view.findViewById(R.id.titleTextView)
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = filteredList[holder.adapterPosition]

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.titleTextView.text = holder.mItem!!.text
        holder.checkBox.isChecked = holder.mItem!!.isSelected
        if (singleSelection) {
            holder.checkBox.visibility = View.GONE
        } else {
            holder.checkBox.visibility = View.VISIBLE
        }
        var productPosition = 0
        for (i in mValues.indices) {
            if (mValues[i].code.equals(holder.mItem!!.code)) {
                productPosition = i
            }
        }

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) holder.itemView.setBackgroundResource(R.drawable.light_selected_background)
            else holder.itemView.setBackgroundResource(0)
            itemClickListener.onItemClicked(
                filteredList[holder.adapterPosition],
                productPosition,
                isChecked
            )
        }
        holder.itemView.setOnClickListener { _ ->
            holder.checkBox.isChecked = !holder.checkBox.isChecked
        }

    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): Filter.FilterResults {
                val charString = charSequence.toString()
                if (charString.isEmpty()) {
                    filteredList = mValues
                } else {
                    val tempList = ArrayList<SearchableItem>()
                    for (row in mValues) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for name or phone number match
                        if (row.text.lowercase(Locale.getDefault())
                                .contains(charString.lowercase(Locale.getDefault()))
                        ) {
                            tempList.add(row)
                        }
                    }

                    filteredList = tempList
                }

                val filterResults = Filter.FilterResults()
                filterResults.values = filteredList
                return filterResults
            }

            override fun publishResults(
                charSequence: CharSequence,
                filterResults: Filter.FilterResults
            ) {
                filteredList = filterResults.values as ArrayList<SearchableItem>

                // refresh the list with filtered data
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    interface ItemClickListener {
        fun onItemClicked(
            item: SearchableItem,
            position: Int,
            b: Boolean)
    }

}