package rs.readahead.washington.mobile.views.fragment.vault.adapters.connections

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener

class ServerAdapter(
    val list: List<ServerDataItem>,
    private val vaultClickListener: VaultClickListener
) : RecyclerView.Adapter<ServerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        return ServerViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        holder.bind(list[position], vaultClickListener)
    }

    override fun getItemCount() = list.size

}