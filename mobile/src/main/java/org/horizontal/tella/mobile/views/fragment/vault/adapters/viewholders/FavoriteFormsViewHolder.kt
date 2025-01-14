package org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.collect.CollectForm
import org.horizontal.tella.mobile.views.fragment.vault.adapters.VaultClickListener
import org.horizontal.tella.mobile.views.fragment.vault.adapters.forms.FavoriteFormsAdapter
import org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class FavoriteFormsViewHolder  (val view : View) : BaseViewHolder<List<CollectForm>>(view) {
    private lateinit var favoriteFormsRecyclerView : RecyclerView

    override fun bind(item: List<CollectForm>, vaultClickListener: VaultClickListener) {
        favoriteFormsRecyclerView = view.findViewById(R.id.favoriteFormsRecyclerView)
        favoriteFormsRecyclerView.apply {
            adapter = FavoriteFormsAdapter(item, vaultClickListener)
        }
    }

    companion object {
        fun from(parent: ViewGroup): FavoriteFormsViewHolder {
            return FavoriteFormsViewHolder(parent.inflate(R.layout.item_vault_favorite_forms))
        }
    }

}