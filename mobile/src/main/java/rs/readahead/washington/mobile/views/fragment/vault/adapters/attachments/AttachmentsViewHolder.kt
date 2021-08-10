package rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.VaultFile

class AttachmentsViewHolder(val view: View)  : RecyclerView.ViewHolder(view){
    private lateinit var dateTextView : TextView;
    private lateinit var nameFileTextVew : TextView
    private lateinit var filePreviewImg : ImageView
    private lateinit var moreBtn : View
    fun bind(vaultFile: VaultFile,clickListener: (VaultFile) -> Unit){
        view.apply {
            dateTextView = findViewById(R.id.fileDateTextView)
            nameFileTextVew = findViewById(R.id.fileNameTextView)
            filePreviewImg = findViewById(R.id.attachmentImg)
            moreBtn = findViewById(R.id.more)
        }
        vaultFile.apply {
            dateTextView.text = created.toString()
            nameFileTextVew.text = name
        }

        moreBtn.setOnClickListener {
            clickListener.invoke(vaultFile)
        }

    }

    companion object {
        fun from(parent: ViewGroup): AttachmentsViewHolder {
            return AttachmentsViewHolder(parent.inflate(R.layout.item_vault_attachment_hor))
        }
    }

}