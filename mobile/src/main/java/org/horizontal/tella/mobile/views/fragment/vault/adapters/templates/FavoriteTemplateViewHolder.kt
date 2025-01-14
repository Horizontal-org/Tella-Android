package org.horizontal.tella.mobile.views.fragment.vault.adapters.templates

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.uwazi.CollectTemplate
import org.horizontal.tella.mobile.views.fragment.vault.adapters.VaultClickListener
import org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class FavoriteTemplateViewHolder(val view: View) : BaseViewHolder<CollectTemplate>(view) {
       private lateinit var formsTitleTextView : TextView
    override fun bind(item: CollectTemplate, vaultClickListener: VaultClickListener) {
        formsTitleTextView = view.findViewById(R.id.templateTitleTv)
        item.apply {
            formsTitleTextView.text = entityRow.name
        }

        view.setOnClickListener {
            vaultClickListener.onFavoriteTemplateClickListener(item)
        }
    }
    companion object {
        fun from(parent: ViewGroup): FavoriteTemplateViewHolder {
            return FavoriteTemplateViewHolder(parent.inflate(R.layout.item_favorite_template))
        }
    }
}