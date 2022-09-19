package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class TitleViewHolder (val view : View) : BaseViewHolder<String?>(view) {

    override fun bind(item: String?, vaultClickListener: VaultClickListener) {
        view.apply {
        }
    }
    companion object {
        fun from(parent: ViewGroup): TitleViewHolder {
            return TitleViewHolder(parent.inflate(R.layout.item_vault_title))
        }
    }
}