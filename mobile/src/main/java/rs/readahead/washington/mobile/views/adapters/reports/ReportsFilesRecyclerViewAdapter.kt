package rs.readahead.washington.mobile.views.adapters.reports

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hzontal.tella_vault.VaultFile
import com.hzontal.utils.MediaFile.isAudioFileType
import com.hzontal.utils.MediaFile.isImageFileType
import com.hzontal.utils.MediaFile.isVideoFileType
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.media.VaultFileUrlLoader
import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel
import rs.readahead.washington.mobile.views.interfaces.IAttachmentsMediaHandler


open class ReportsFilesRecyclerViewAdapter(
    private val iAttachmentsMediaHandler: IAttachmentsMediaHandler,
    context: Context,
    mediaFileHandler: MediaFileHandler
) :
    RecyclerView.Adapter<ReportsFilesRecyclerViewAdapter.GridAttachmentsViewHolder>() {
    private var listAttachment: ArrayList<VaultFile> = arrayListOf()
    private val glideLoader = VaultFileUrlLoader(context, mediaFileHandler)
    var onRemoveAttachment: (Int, VaultFile?) -> Unit = { _: Int, _: VaultFile? -> }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridAttachmentsViewHolder {
        return GridAttachmentsViewHolder(parent)
    }

    fun insertAttachment(newAttachment: VaultFile) {
        listAttachment.add(0, newAttachment)
        notifyItemInserted(0)
    }

    fun setFiles(listAttachment: ArrayList<VaultFile>) {
        this.listAttachment = listAttachment
        notifyDataSetChanged()
    }

    fun getFiles(): ArrayList<VaultFile> {
        return listAttachment
    }

    private fun removeFile(position: Int, name: VaultFile?) {
        listAttachment.removeAt(position)
        onRemoveAttachment.invoke(position, name)
        notifyItemRemoved(position)
    }


    override fun getItemCount(): Int {
        return listAttachment.size
    }

    override fun onBindViewHolder(holder: GridAttachmentsViewHolder, position: Int) {
        holder.bind(vaultFile = listAttachment[position],iAttachmentsMediaHandler = iAttachmentsMediaHandler)
    }


    inner class GridAttachmentsViewHolder (view: View)  : BaseAttachmentViewHolder(view) {

        constructor(parent: ViewGroup)
                : this(LayoutInflater.from(parent.context).inflate(R.layout.item_vault_attachment_grid, parent, false))
        private lateinit var fileNameTextView : TextView
        private lateinit var moreBtn : View

        fun bind(vaultFile: VaultFile?, iAttachmentsMediaHandler: IAttachmentsMediaHandler){
            view.apply {
                fileNameTextView = findViewById(R.id.fileNameTextView)
                filePreviewImg = findViewById(R.id.attachmentImg)
                moreBtn = findViewById(R.id.more)
                icAttachmentImg = findViewById(R.id.icAttachmentImg)
                checkBox = findViewById(R.id.checkbox_type_single)
            }
            vaultFile?.apply {
                fileNameTextView.text = name
            }

            moreBtn.setOnClickListener { vaultFile?.let { it1 -> iAttachmentsMediaHandler.playMedia(it1) } }

            if (isImageFileType(vaultFile!!.mimeType)) {
                this.showImageInfo(vaultFile)
                Glide.with(context)
                    .using(glideLoader)
                    .load(VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(filePreviewImg)
            } else if (isAudioFileType(vaultFile.mimeType)) {
                this.showAudioInfo()
                val drawable: Drawable? = VectorDrawableCompat.create(
                    context.getResources(),
                    R.drawable.ic_mic_gray, null
                )
                filePreviewImg.setImageDrawable(drawable)
            } else if (isVideoFileType(vaultFile.mimeType)) {
                this.showVideoInfo(vaultFile)
                Glide.with(context)
                    .using(glideLoader)
                    .load(VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(filePreviewImg)
            }
        }

    }
    abstract inner class BaseAttachmentViewHolder (val view: View)  : RecyclerView.ViewHolder(view){
        protected lateinit var icAttachmentImg : ImageView
        protected lateinit var filePreviewImg : ImageView
        protected val context: Context by lazy {view.context}
        protected lateinit var checkBox : AppCompatImageView

        fun showVideoInfo(vaultFile: VaultFile){
            Glide.with(context)
                .using(glideLoader)
                .load(VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(filePreviewImg)
            icAttachmentImg.setBackgroundResource(R.drawable.ic_play)

        }
        fun showAudioInfo() {
            icAttachmentImg.setBackgroundResource(R.drawable.ic_audio_w_small)
        }
        fun showImageInfo(vaultFile: VaultFile){
            Glide.with(filePreviewImg.context)
                .using(glideLoader)
                .load(VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(filePreviewImg)
        }
    }
}
