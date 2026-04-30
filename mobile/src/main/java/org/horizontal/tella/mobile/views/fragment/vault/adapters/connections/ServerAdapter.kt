package org.horizontal.tella.mobile.views.fragment.vault.adapters.connections

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.ServerType
import org.horizontal.tella.mobile.views.fragment.vault.adapters.VaultClickListener

class ServerAdapter(
    private val list: List<ServerDataItem>,
    private val vaultClickListener: VaultClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val TYPE_SERVER = 0
        const val TYPE_ADD = 1
    }

    override fun getItemViewType(position: Int): Int =
        if (list[position].type == ServerType.ADD_BUTTON) TYPE_ADD else TYPE_SERVER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_ADD) {
            val v = inflater.inflate(R.layout.item_add_connection_card, parent, false)
            AddButtonViewHolder(v)
        } else {
            ServerViewHolder.from(parent) // your existing server card holder
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]
        if (holder is AddButtonViewHolder) {
            holder.itemView.setOnClickListener { vaultClickListener.onServerItemClickListener(item) }
        } else if (holder is ServerViewHolder) {
            holder.bind(item, vaultClickListener)
        }
    }

    override fun getItemCount() = list.size

    private class AddButtonViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
