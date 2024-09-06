package rs.readahead.washington.mobile.views.dialog.googledrive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.R

class StringListAdapter(
    private val items: List<String>,
    private val itemClickListener: ItemClickListener
) : RecyclerView.Adapter<StringListAdapter.StringViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StringViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.simple_list_item, parent, false)
        return StringViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: StringViewHolder, position: Int) {
        holder.bind(items[position])
        holder.itemView.isSelected = selectedPosition == position

        holder.itemView.setOnClickListener {
            holder.setChecked(true)
            selectedPosition = holder.adapterPosition
            notifyDataSetChanged()
            itemClickListener.onItemClick(items[position])
        }
    }

    inner class StringViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView: TextView = view.findViewById(R.id.titleTextView)
        private val checkBox: CheckBox = view.findViewById(R.id.checkBox)

        fun bind(item: String) {
            textView.text = item
        }
        fun setChecked(isChecked: Boolean) {
            checkBox.isChecked = isChecked
        }
    }

    interface ItemClickListener {
        fun onItemClick(item: String)
    }
}