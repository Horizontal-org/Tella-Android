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
        binding.activityNameTextView.text = item.name

        when (item.type) {
            BackgroundActivityType.FILE -> {
                item.thumb?.let { icPreview(item.mimeType, it, binding.backgroundActivityImg) }
            }

            BackgroundActivityType.OTHER -> {
                binding.backgroundActivityImg.loadDocumentIcon()
            }
        }
    }

    private fun icPreview(mimeType: String, thumb: ByteArray, previewImageView: ImageView) {
        when {
            MediaFile.isImageFileType(mimeType) -> previewImageView.loadImage(thumb)
            MediaFile.isAudioFileType(mimeType) -> previewImageView.loadAudioIcon()
            MediaFile.isVideoFileType(mimeType) -> {
                previewImageView.loadVideoIcon()
                previewImageView.loadImage(thumb)
            }
            MediaFile.isTextFileType(mimeType) -> previewImageView.loadDocumentIcon()
            else -> previewImageView.loadDocumentIcon()
        }
    }

    private fun ImageView.loadImage(thumb: ByteArray) {
        Glide.with(this)
            .load(thumb)
            .into(this)
    }

    private fun ImageView.loadAudioIcon() {
        Glide.with(this)
            .load(R.drawable.ic_audio_w_small)
            .into(this)
    }

    private fun ImageView.loadVideoIcon() {
        Glide.with(this)
            .load(R.drawable.ic_play)
            .into(this)
    }

    private fun ImageView.loadDocumentIcon() {
        Glide.with(this)
            .load(R.drawable.ic_document_24px_filled)
            .into(this)
    }


}