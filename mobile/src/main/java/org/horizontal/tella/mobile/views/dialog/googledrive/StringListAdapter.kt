import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.googledrive.Folder

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
        private val container: LinearLayout = view.findViewById(R.id.item_container)

        fun bind(item: String, isSelected: Boolean) {
            textView.text = item
            imageChecked.isVisible = isSelected // Show the image only if the item is selected
            container.setBackgroundColor(
                if (isSelected)
                    ContextCompat.getColor(itemView.context, R.color.wa_white_8)
                else
                    ContextCompat.getColor(itemView.context, R.color.wa_white_16)
            )
        }
    }

    interface ItemClickListener {
        fun onItemClick(item: Folder)
    }
}