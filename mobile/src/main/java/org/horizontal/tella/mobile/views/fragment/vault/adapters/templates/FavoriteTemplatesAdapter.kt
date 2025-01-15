package org.horizontal.tella.mobile.views.fragment.vault.adapters.templates

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.horizontal.tella.mobile.domain.entity.uwazi.CollectTemplate
import org.horizontal.tella.mobile.views.fragment.vault.adapters.VaultClickListener

class FavoriteTemplatesAdapter(val list: List<CollectTemplate>, private val vaultClickListener: VaultClickListener)  :  RecyclerView.Adapter<FavoriteTemplateViewHolder>(){

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int) = FavoriteTemplateViewHolder.from(p0)

    override fun onBindViewHolder(holder: FavoriteTemplateViewHolder, p1: Int) {
       holder.bind(list[p1],vaultClickListener)
    }

    override fun getItemCount() = list.size
}