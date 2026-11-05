package org.horizontal.tella.mobile.views.fragment.ouisync

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.ItemOuisyncEntryBinding

data class OuisyncRow(val name: String, val path: String, val directory: Boolean)

class OuisyncListAdapter(
    private val onClick: (OuisyncRow) -> Unit,
    private val onMenu: ((OuisyncRow) -> Unit)? = null
) : RecyclerView.Adapter<OuisyncListAdapter.Holder>() {

    private val rows = mutableListOf<OuisyncRow>()

    fun submit(items: List<OuisyncRow>) {
        rows.clear()
        rows.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemOuisyncEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        holder.binding.name.text = row.name
        holder.binding.icon.setImageResource(iconFor(row))
        val showMenu = onMenu != null && !row.directory
        holder.binding.more.isVisible = showMenu
        holder.binding.more.setOnClickListener { onMenu?.invoke(row) }
        holder.binding.root.setOnClickListener { onClick(row) }
    }

    private fun iconFor(row: OuisyncRow): Int {
        return if (row.directory) R.drawable.ic_folder_24px
        else R.drawable.ic_document_24px_filled
    }

    class Holder(val binding: ItemOuisyncEntryBinding) : RecyclerView.ViewHolder(binding.root)
}
