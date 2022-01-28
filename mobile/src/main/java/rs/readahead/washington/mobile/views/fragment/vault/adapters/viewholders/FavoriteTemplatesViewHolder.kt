package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.templates.FavoriteTemplatesAdapter
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class FavoriteTemplatesViewHolder (val view : View) : BaseViewHolder<List<CollectTemplate>>(view) {
    private lateinit var favoriteTemplatesRecyclerView : RecyclerView

    override fun bind(item: List<CollectTemplate>, vaultClickListener: VaultClickListener) {
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