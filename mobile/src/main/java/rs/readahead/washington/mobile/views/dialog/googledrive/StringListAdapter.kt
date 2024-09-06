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

    private var selectedPosition = -1  // Track the currently selected position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StringViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.simple_list_item, parent, false)
        return StringViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: StringViewHolder, position: Int) {
        holder.bind(items[position])

        // Set the checkbox checked state based on the selected position
        //holder.setChecked(position == selectedPosition)

        holder.itemView.setOnClickListener {
            // Check if the selected position has changed
            if (selectedPosition != position) {
                selectedPosition = position  // Update the selected position
                notifyDataSetChanged()  // Notify the adapter to refresh the view
                itemClickListener.onItemClick(items[position])
                holder.setChecked(true)
            }
        }
    }

    inner class StringViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView: TextView = view.findViewById(R.id.titleTextView)
        private val checkBox: CheckBox = view.findViewById(R.id.checkBox)

        fun bind(item: String) {
            textView.text = item
        }

        // Set the checked state of the checkbox
        fun setChecked(isChecked: Boolean) {
            checkBox.isChecked = isChecked
        }
    }

    interface ItemClickListener {
        fun onItemClick(item: String)
    }
}
