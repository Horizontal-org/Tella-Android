package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.connections.ServerAdapter
import rs.readahead.washington.mobile.views.fragment.vault.adapters.connections.ServerDataItem
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class ServersViewHolder(val view: View) : BaseViewHolder<List<ServerDataItem>>(view) {

    private lateinit var serversRecyclerView: RecyclerView

    override fun bind(item: List<ServerDataItem>, vaultClickListener: VaultClickListener) {
        serversRecyclerView = view.findViewById(R.id.serversRecyclerView)
        serversRecyclerView.apply {
            adapter = ServerAdapter(item, vaultClickListener)
        }
    }


    companion object {
        fun from(parent: ViewGroup): ServersViewHolder {
            return ServersViewHolder(parent.inflate(R.layout.item_home_vault_servers_list))
        }
    }
}