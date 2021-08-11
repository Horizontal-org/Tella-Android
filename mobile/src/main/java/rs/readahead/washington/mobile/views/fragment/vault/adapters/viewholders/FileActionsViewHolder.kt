package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import org.hzontal.shared_ui.buttons.HomeButton
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.VaultFile

class FileActionsViewHolder (val view : View) : BaseViewHolder<VaultFile>(view) {

    private lateinit var allFilesBtn : HomeButton
    private lateinit var imagesBtn : HomeButton
    private lateinit var audioBtn : HomeButton
    private lateinit var documentsBtn : HomeButton
    private lateinit var othersBtn : HomeButton
    private lateinit var videosBtn : HomeButton
    override fun bind(item: VaultFile, vaultClickListener: VaultClickListener) {
        view.apply {
            allFilesBtn = findViewById(R.id.allFilesBtn)
            imagesBtn = findViewById(R.id.imagesBtn)
            documentsBtn = findViewById(R.id.documentsBtn)
            audioBtn = findViewById(R.id.audioBtn)
            othersBtn = findViewById(R.id.othersBtn)
            videosBtn = findViewById(R.id.videosBtn)
        }
        allFilesBtn.setOnClickListener { vaultClickListener.allFilesClickListener()  }
        imagesBtn.setOnClickListener { vaultClickListener.imagesClickListener()  }
        documentsBtn.setOnClickListener { vaultClickListener.documentsClickListener()  }
        audioBtn.setOnClickListener { vaultClickListener.audioClickListener()  }
        othersBtn.setOnClickListener { vaultClickListener.othersClickListener()  }
        videosBtn.setOnClickListener { vaultClickListener.videoClickListener()  }
    }
    companion object {
        fun from(parent: ViewGroup): FileActionsViewHolder {
            return FileActionsViewHolder(parent.inflate(R.layout.item_vault_files))
        }
    }
}