package rs.readahead.washington.mobile.views.fragment.vault.adapters.forms

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.data.entity.XFormEntity
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.FavoriteFormsViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder

class FavoriteFormsAdapter(val list: List<CollectForm>, private val vaultClickListener: VaultClickListener)  :  RecyclerView.Adapter<FavoriteFormViewHolder>(){

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int) = FavoriteFormViewHolder.from(p0)

    override fun onBindViewHolder(holder: FavoriteFormViewHolder, p1: Int) {
       holder.bind(list[p1],vaultClickListener)
    }

    override fun getItemCount() = list.size
}