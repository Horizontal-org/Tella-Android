package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.VaultFile

class FileActionsViewHolder (val view : View) : BaseViewHolder<VaultFile>(view) {

    override fun bind(item: VaultFile, vaultClickListener: VaultClickListener) {

    }
    companion object {
        fun from(parent: ViewGroup): FileActionsViewHolder {
            return FileActionsViewHolder(parent.inflate(R.layout.item_vault_files))
        }
    }
}