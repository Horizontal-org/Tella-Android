package rs.readahead.washington.mobile.views.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntityRow

class UwaziTemplatesAdapter : ListAdapter<UwaziEntityRow, UwaziTemplatesAdapter.EntityViewHolder>(EntityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntityViewHolder {
        return EntityViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.templates_uwazi_row, parent, false))
    }

    override fun onBindViewHolder(holder: EntityViewHolder, position: Int) {
        holder.bind(entityRow = getItem(position))
    }

    inner class EntityViewHolder(val view : View) : RecyclerView.ViewHolder(view) {
        fun bind(entityRow: UwaziEntityRow) {
            view.apply{
                view.findViewById<TextView>(R.id.name).text = entityRow.name
                view.findViewById<TextView>(R.id.organization).text = entityRow.entityViewPage
            }
        }
    }

    private class EntityDiffCallback : DiffUtil.ItemCallback<UwaziEntityRow>() {
        override fun areItemsTheSame(oldItem: UwaziEntityRow, newItem: UwaziEntityRow): Boolean {
            return oldItem._id == newItem._id
        }

        override fun areContentsTheSame(oldItem: UwaziEntityRow, newItem: UwaziEntityRow): Boolean {
            return oldItem == newItem
        }
    }

}