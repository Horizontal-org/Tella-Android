package org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziTemplate
import org.horizontal.tella.mobile.views.fragment.vault.adapters.VaultClickListener
import org.horizontal.tella.mobile.views.fragment.vault.adapters.templates.FavoriteTemplatesAdapter
import org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class FavoriteTemplatesViewHolder (val view : View) : BaseViewHolder<List<UwaziTemplate>>(view) {
    private lateinit var favoriteTemplatesRecyclerView : RecyclerView

    override fun bind(item: List<UwaziTemplate>, vaultClickListener: VaultClickListener) {
        favoriteTemplatesRecyclerView = view.findViewById(R.id.favoriteFormsRecyclerView)

        view.findViewById<TextView>(R.id.favoritesText).text = view.resources.getString(R.string.Vault_FavoriteTemplates_Title)
        favoriteTemplatesRecyclerView.apply {
            adapter = FavoriteTemplatesAdapter(item, vaultClickListener)
        }
    }

    companion object {
        fun from(parent: ViewGroup): FavoriteTemplatesViewHolder {
            return FavoriteTemplatesViewHolder(parent.inflate(R.layout.item_vault_favorite_forms))
        }
    }

}