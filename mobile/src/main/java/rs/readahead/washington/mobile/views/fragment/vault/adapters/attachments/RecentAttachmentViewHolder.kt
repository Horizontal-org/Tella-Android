package rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class RecentAttachmentViewHolder (val view: View) : BaseViewHolder<VaultFile>(view) {
    private lateinit var previewImageView : AppCompatImageView
    override fun bind(item: VaultFile, vaultClickListener: VaultClickListener) {
        previewImageView = view.findViewById(R.id.attachmentImg)
    }

    companion object {
        fun from(parent: ViewGroup): RecentAttachmentViewHolder {
            return RecentAttachmentViewHolder(parent.inflate(R.layout.item_vault_attachmenets))
        }
    }

}