package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments.RecentAttachmentAdapter
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class RecentFilesViewHolder  constructor(val view : View) : BaseViewHolder<List<VaultFile>>(view) {
    private lateinit var recentFilesRecyclerView : RecyclerView

    override fun bind(item: List<VaultFile>, vaultClickListener: VaultClickListener) {
        recentFilesRecyclerView = view.findViewById(R.id.recentFilesRecyclerView)
        recentFilesRecyclerView.apply {
            adapter = RecentAttachmentAdapter(item,vaultClickListener)
        }
    }

    companion object {
        fun from(parent: ViewGroup): RecentFilesViewHolder{
            return RecentFilesViewHolder(parent.inflate(R.layout.item_vault_recent_files))
        }
    }

}