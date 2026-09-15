package org.horizontal.tella.mobile.views.adapters.reports

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hzontal.tella_vault.VaultFile
import com.hzontal.utils.MediaFile.isAudioFileType
import com.hzontal.utils.MediaFile.isImageFileType
import com.hzontal.utils.MediaFile.isVideoFileType
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.interfaces.IReportAttachmentsHandler


open class ReportsFilesRecyclerViewAdapter(
    private val iAttachmentsMediaHandler: IReportAttachmentsHandler,
) :
    RecyclerView.Adapter<ReportsFilesRecyclerViewAdapter.GridAttachmentsViewHolder>() {
    private var listAttachment: ArrayList<VaultFile> = arrayListOf()

    init {
        val file = VaultFile()
        file.type = VaultFile.Type.UNKNOWN
        insertAttachment(file)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridAttachmentsViewHolder {
        return GridAttachmentsViewHolder(parent)
    }

    fun insertAttachment(newAttachment: VaultFile) {
        if (newAttachment.type != VaultFile.Type.UNKNOWN) {
            if (!listAttachment.contains(newAttachment)) {
                listAttachment.add(0, newAttachment)
                notifyItemInserted(0)
            }
        } else {
            listAttachment.add(0, newAttachment)
            notifyItemInserted(0)
        }
    }

    fun getFiles(): ArrayList<VaultFile> {
        val listFiles: ArrayList<VaultFile> = arrayListOf()
        for (file in listAttachment) {
            if (file.type != VaultFile.Type.UNKNOWN) listFiles.add(file)
        }
        return listFiles
    }

    private fun removeFile(position: Int) {
        listAttachment.removeAt(position)
        iAttachmentsMediaHandler.removeFiles()
        notifyItemRemoved(position)
    }

    override fun getItemCount(): Int {
        return listAttachment.size
    }

    override fun onBindViewHolder(holder: GridAttachmentsViewHolder, position: Int) {
        holder.bind(
            vaultFile = listAttachment[position]
        )
    }

    open inner class GridAttachmentsViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        constructor(parent: ViewGroup)
                : this(
            LayoutInflater.from(parent.context).inflate(R.layout.item_report_files, parent, false)
        )

        protected lateinit var icAttachmentImg: ImageView
        private lateinit var filePreviewImg: ImageView
        private lateinit var fileNameTextView: TextView
        private lateinit var removeBtn: View
        protected val context: Context by lazy { view.context }

        fun bind(vaultFile: VaultFile?) {
            view.apply {
                fileNameTextView = findViewById(R.id.fileNameTextView)
                filePreviewImg = findViewById(R.id.attachmentImg)
                removeBtn = findViewById(R.id.remove)
                icAttachmentImg = findViewById(R.id.icAttachmentImg)
            }

            if (vaultFile!!.type != VaultFile.Type.UNKNOWN) {
                resetAttachmentPreview()
                removeBtn.setOnClickListener {
                    removeFile(position = layoutPosition)
                }
                if (!vaultFile.mimeType.isNullOrEmpty() && isImageFileType(vaultFile.mimeType)) {
                    this.showImageInfo(vaultFile)
                } else if (!vaultFile.mimeType.isNullOrEmpty() && isAudioFileType(vaultFile.mimeType)) {
                    this.showAudioInfo()
                    fileNameTextView.text = vaultFile.name
                } else if (!vaultFile.mimeType.isNullOrEmpty() && isVideoFileType(vaultFile.mimeType)) {
                    this.showVideoInfo(vaultFile)
                } else {
                    fileNameTextView.text = vaultFile.name
                    this.showDocInfo()
                }
            } else {
                removeBtn.visibility = View.GONE
                showAddLink()
            }
        }

        private fun resetAttachmentPreview() {
            filePreviewImg.scaleType = ImageView.ScaleType.CENTER_CROP
            filePreviewImg.setBackgroundResource(R.color.light_purple)
            filePreviewImg.imageTintList = null
            filePreviewImg.setImageDrawable(null)
            filePreviewImg.setOnClickListener(null)
        }

        private fun showVideoInfo(vaultFile: VaultFile) {
            filePreviewImg.loadImage(vaultFile.thumb, R.drawable.ic_video)
            icAttachmentImg.setBackgroundResource(R.drawable.ic_play)
        }

        private fun showAudioInfo() {
            icAttachmentImg.setBackgroundResource(R.drawable.ic_audio_w_small)
        }

        private fun showDocInfo() {
            icAttachmentImg.setBackgroundResource(R.drawable.ic_reports)
        }

        private fun showImageInfo(vaultFile: VaultFile) {
            filePreviewImg.loadImage(vaultFile.thumb, R.drawable.ic_gallery)
        }

        fun ImageView.loadImage(thumb: ByteArray?, @DrawableRes fallback: Int) {
            Glide.with(this)
                .load(thumb)
                .fallback(fallback)
                .error(fallback)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(this)
        }

        private fun showAddLink() {
            filePreviewImg.scaleType = ImageView.ScaleType.CENTER_INSIDE
            filePreviewImg.setBackgroundResource(R.drawable.upload_box_btn)
            filePreviewImg.setImageResource(R.drawable.ic_report_attach_add)
            filePreviewImg.imageTintList = null
            filePreviewImg.setOnClickListener {
                iAttachmentsMediaHandler.addFiles()
            }
        }
    }
}
