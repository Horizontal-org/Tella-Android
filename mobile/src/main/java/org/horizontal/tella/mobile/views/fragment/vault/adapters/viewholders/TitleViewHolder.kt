package org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.fragment.vault.adapters.VaultClickListener
import org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders.base.inflate

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