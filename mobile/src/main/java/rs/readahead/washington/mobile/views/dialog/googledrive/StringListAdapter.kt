import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.googledrive.Folder

class StringListAdapter(
    private val items: List<Folder>,
    private val itemClickListener: ItemClickListener
) : RecyclerView.Adapter<StringListAdapter.StringViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StringViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.simple_list_item, parent, false)
        return StringViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: StringViewHolder, position: Int) {
        holder.bind(items[position].name, position == selectedPosition)

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousPosition) // Refresh the previously selected item
            notifyItemChanged(selectedPosition) // Refresh the newly selected item

            itemClickListener.onItemClick(items[holder.adapterPosition])
        }
    }

    inner class StringViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView: TextView = view.findViewById(R.id.titleTextView)
        private val imageChecked: ImageView = view.findViewById(R.id.image)

        fun bind(item: String, isSelected: Boolean) {
            textView.text = item
            imageChecked.isVisible = isSelected // Show the image only if the item is selected
        }
    }

    interface ItemClickListener {
        fun onItemClick(item: Folder)
    }
}