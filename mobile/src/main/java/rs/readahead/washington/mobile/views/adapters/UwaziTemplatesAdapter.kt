package rs.readahead.washington.mobile.views.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.entity.uwazi.Template
import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntityRow
import rs.readahead.washington.mobile.databinding.BlankCollectFormRowBinding

class UwaziTemplatesAdapter : ListAdapter<UwaziEntityRow, UwaziTemplatesAdapter.EntityViewHolder>(EntityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntityViewHolder {
        return EntityViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.blank_collect_form_row,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: EntityViewHolder, position: Int) {
        holder.bind(entityRow = getItem(position))
    }

    inner class EntityViewHolder(private val binding : BlankCollectFormRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entityRow: UwaziEntityRow) {
            with(binding) {
                name.text = entityRow.name
                organization.text = entityRow.entityViewPage
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