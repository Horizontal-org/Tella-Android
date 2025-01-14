package org.horizontal.tella.mobile.views.fragment.vault.adapters.forms

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.horizontal.tella.mobile.domain.entity.collect.CollectForm
import org.horizontal.tella.mobile.views.fragment.vault.adapters.VaultClickListener

class FavoriteFormsAdapter(val list: List<CollectForm>, private val vaultClickListener: VaultClickListener)  :  RecyclerView.Adapter<FavoriteFormViewHolder>(){

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int) = FavoriteFormViewHolder.from(p0)

    override fun onBindViewHolder(holder: FavoriteFormViewHolder, p1: Int) {
       holder.bind(list[p1],vaultClickListener)
    }

    override fun getItemCount() = list.size
}