package org.horizontal.tella.mobile.views.fragment.vault.adapters.forms

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.collect.CollectForm
import org.horizontal.tella.mobile.views.fragment.vault.adapters.VaultClickListener
import org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class FavoriteFormViewHolder(val view: View) : BaseViewHolder<CollectForm>(view) {
       private lateinit var formsTitleTextView : TextView
    override fun bind(item: CollectForm, vaultClickListener: VaultClickListener) {
        formsTitleTextView = view.findViewById(R.id.formTitleTv)
        item.apply {
            formsTitleTextView.text = form.name
        }

        view.setOnClickListener {
            vaultClickListener.onFavoriteItemClickListener(item)
        }
    }
    companion object {
        fun from(parent: ViewGroup): FavoriteFormViewHolder {
            return FavoriteFormViewHolder(parent.inflate(R.layout.item_favorite_form))
        }
    }
}