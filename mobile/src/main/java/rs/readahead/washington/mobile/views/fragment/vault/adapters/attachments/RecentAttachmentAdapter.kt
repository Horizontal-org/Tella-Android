package rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener

class RecentAttachmentAdapter (val list: List<VaultFile?>, private val vaultClickListener: VaultClickListener)  :  RecyclerView.Adapter<RecentAttachmentViewHolder>(){

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int) = RecentAttachmentViewHolder.from(p0)

    override fun onBindViewHolder(holder: RecentAttachmentViewHolder, p1: Int) {
       holder.bind(list[p1],vaultClickListener)
    }

    override fun getItemCount() = list.size


}
