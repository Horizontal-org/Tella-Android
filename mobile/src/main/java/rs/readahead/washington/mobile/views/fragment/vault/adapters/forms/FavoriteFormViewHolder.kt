package rs.readahead.washington.mobile.views.fragment.vault.adapters.forms

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.entity.XFormEntity
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.FavoriteFormsViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class FavoriteFormViewHolder(val view: View) : BaseViewHolder<XFormEntity>(view) {
       private lateinit var formsTitleTextView : TextView
    override fun bind(item: XFormEntity, vararg args: Any) {
        formsTitleTextView = view.findViewById(R.id.formTitleTv)
        item.apply {
            formsTitleTextView.text = name
        }

        view.setOnClickListener {
            (args[0] as VaultClickListener).onFavoriteItemClickListener(item)
        }
    }
    companion object {
        fun from(parent: ViewGroup): FavoriteFormViewHolder {
            return FavoriteFormViewHolder(parent.inflate(R.layout.item_favorite_form))
        }
    }
}