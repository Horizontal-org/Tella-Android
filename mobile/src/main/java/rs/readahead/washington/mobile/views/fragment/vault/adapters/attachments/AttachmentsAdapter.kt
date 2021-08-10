package rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments

import android.view.ViewGroup
import android.widget.Adapter
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.VaultFile

class AttachmentsAdapter constructor(private val clickListener: (VaultFile) -> Unit = {}) :   RecyclerView.Adapter<AttachmentsViewHolder>(){
     private var vaultList: List<VaultFile> = arrayListOf()
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int) = AttachmentsViewHolder.from(p0)

    override fun onBindViewHolder(holder: AttachmentsViewHolder, position: Int) {
        holder.bind(vaultFile = vaultList[position],clickListener = clickListener)
    }

    override fun getItemCount() = vaultList.size

    fun submitList(vaultList: List<VaultFile>){
        this.vaultList = vaultList
        notifyItemChanged(0)
    }

}