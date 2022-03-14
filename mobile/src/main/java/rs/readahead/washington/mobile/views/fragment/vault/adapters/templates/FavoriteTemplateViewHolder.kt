package rs.readahead.washington.mobile.views.fragment.vault.adapters.templates

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate

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