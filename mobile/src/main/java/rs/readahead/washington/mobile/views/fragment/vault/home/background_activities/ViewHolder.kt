package rs.readahead.washington.mobile.views.fragment.vault.home.background_activities

import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hzontal.utils.MediaFile
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.ItemBackgroundActivityBinding
import rs.readahead.washington.mobile.domain.entity.background_activity.BackgroundActivityModel
import rs.readahead.washington.mobile.domain.entity.background_activity.BackgroundActivityType

class ViewHolder(private val binding: ItemBackgroundActivityBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: BackgroundActivityModel) {

        val context = binding.fileNameTextView.context
        val encryptingText = context.getString(R.string.encrypting)
        binding.fileNameTextView.text = "$encryptingText ${item.name}"
        when (item.type) {
            BackgroundActivityType.FILE -> {
                item.thumb?.let { icPreview(item.mimeType, it, binding.attachmentImg) }
            }

            BackgroundActivityType.OTHER -> {
                binding.icAttachmentImg.showDocumentInfo()
            }
        }
    }

    private fun icPreview(mimeType: String, thumb: ByteArray, previewImageView: ImageView) {
        when {
            MediaFile.isImageFileType(mimeType) -> binding.icAttachmentImg.loadImage(thumb)
            MediaFile.isAudioFileType(mimeType) -> binding.icAttachmentImg.showAudioInfo()
            MediaFile.isVideoFileType(mimeType) -> {
                binding.icAttachmentImg.showVideoInfo()
                binding.icAttachmentImg.loadImage(thumb)
            }
            MediaFile.isTextFileType(mimeType) -> binding.icAttachmentImg.showDocumentInfo()
            else -> binding.icAttachmentImg.showDocumentInfo()
        }
    }

    private fun ImageView.loadImage(thumb: ByteArray) {
        Glide.with(this)
            .load(thumb)
            .into(this)
    }

    private fun ImageView.showVideoInfo() {
        setBackgroundResource(R.drawable.ic_play)
    }

    private fun ImageView.showAudioInfo() {
        setBackgroundResource(R.drawable.ic_audio_w_small)
    }

    private fun ImageView.showDocumentInfo() {
        setBackgroundResource(R.drawable.ic_document_24px_filled)
    }

}