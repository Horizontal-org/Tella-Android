package rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hzontal.tella_vault.VaultFile
import com.hzontal.utils.MediaFile
import com.hzontal.utils.MediaFile.isAudioFileType
import com.hzontal.utils.MediaFile.isImageFileType
import com.hzontal.utils.MediaFile.isTextFileType
import com.hzontal.utils.MediaFile.isVideoFileType
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.media.VaultFileUrlLoader
import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class RecentAttachmentViewHolder (val view: View) : BaseViewHolder<VaultFile?>(view) {
   // private val glideLoader = VaultFileUrlLoader(mContext.applicationContext, MediaFileHandler())
    private lateinit var previewImageView : AppCompatImageView
    private lateinit var icAttachmentImg : AppCompatImageView
    private lateinit var more : AppCompatImageView
    private lateinit var fileNameTextView : TextView
    override fun bind(item: VaultFile?, vaultClickListener: VaultClickListener) {
        previewImageView = view.findViewById(R.id.attachmentImg)
        icAttachmentImg = view.findViewById(R.id.icAttachmentImg)
        fileNameTextView = view.findViewById(R.id.fileNameTextView)
        more = view.findViewById(R.id.more)
        item?.let {   icPreview(it) }
        view.setOnClickListener {
            if (item != null) {
                vaultClickListener.onRecentFilesItemClickListener(item)
            }
        }
    }

    private fun icPreview(vaultFile : VaultFile){
        if (vaultFile.mimeType == null ) return
        when {
            isImageFileType(vaultFile.mimeType) -> {
                /*Glide.with(previewImageView.context)
                    .using(glideLoader)
                    .load(VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(previewImageView)*/
            }
            isAudioFileType(vaultFile.mimeType) -> {
                showAudioInfo(vaultFile)
            }
            isVideoFileType(vaultFile.mimeType) -> {
                showVideoInfo()
               /* Glide.with(mContext)
                    .using(glideLoader)
                    .load(VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(previewImageView)*/
            }
            isTextFileType(vaultFile.mimeType) -> {
                showDocumentInfo(vaultFile)
            }
        }
    }

    private fun showVideoInfo() {
        icAttachmentImg.setBackgroundResource(R.drawable.ic_play)
    }

    private fun showAudioInfo(vaultFile: VaultFile) {
        icAttachmentImg.setBackgroundResource(R.drawable.ic_audio_w_small)
        fileNameTextView.visibility = View.VISIBLE
        fileNameTextView.text = vaultFile.name
    }

    private fun showDocumentInfo(vaultFile: VaultFile?) {
        icAttachmentImg.setBackgroundResource(R.drawable.ic_document_24px_filled)
        fileNameTextView.visibility = View.VISIBLE
        fileNameTextView.text = vaultFile?.name
        more.visibility = View.VISIBLE
    }

    companion object {
        fun from(parent: ViewGroup): RecentAttachmentViewHolder {
            return RecentAttachmentViewHolder(parent.inflate(R.layout.item_vault_attachmenets))
        }
    }

}