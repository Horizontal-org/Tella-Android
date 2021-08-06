package rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments

import android.view.View
import android.view.ViewGroup
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.VaultFile

class RecentAttachmentViewHolder (val view: View) : BaseViewHolder<VaultFile>(view) {

    override fun bind(item: VaultFile, vararg args: Any) {

    }

    companion object {
        fun from(parent: ViewGroup): RecentAttachmentViewHolder {
            return RecentAttachmentViewHolder(parent.inflate(R.layout.item_vault_recent_files))
        }
    }

}