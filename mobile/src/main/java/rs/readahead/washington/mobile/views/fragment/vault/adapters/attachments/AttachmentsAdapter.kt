package rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hzontal.tella_vault.VaultFile
import com.hzontal.utils.MediaFile
import com.hzontal.utils.MediaFile.isAudioFileType
import com.hzontal.utils.MediaFile.isImageFileType
import com.hzontal.utils.MediaFile.isVideoFileType
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel

class AttachmentsAdapter constructor(val iGalleryMediaHandler: IGalleryMediaHandler) :
     RecyclerView.Adapter<AttachmentsAdapter.BaseAttachmentViewHolder>() {
    private var vaultList: List<VaultFile?> = arrayListOf()
    private val selected = LinkedHashSet<VaultFile>()
    private lateinit var layoutManager: GridLayoutManager
    enum class ViewType {
        SMALL,
        DETAILED
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : AttachmentsAdapter.BaseAttachmentViewHolder{
        return when (viewType) {
            ViewType.DETAILED.ordinal -> AttachmentsViewHolder(parent)
            else ->GridAttachmentsViewHolder(parent)
        }
    }

    override fun onBindViewHolder(holder: AttachmentsAdapter.BaseAttachmentViewHolder, position: Int) {
        val vaultFile = vaultList[position]

        if (isImageFileType(vaultFile.mimeType)) {
            holder.showImageInfo()
            Glide.with(holder.mediaView.getContext())
                .using(glideLoader)
                .load(VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(holder.mediaView)
        } else if (isAudioFileType(vaultFile.mimeType)) {
            holder.showAudioInfo(vaultFile)
            val drawable: Drawable? = VectorDrawableCompat.create(
                holder.itemView.context.resources,
                R.drawable.ic_mic_gray, null
            )
            holder.mediaView.setImageDrawable(drawable)
        } else if (isVideoFileType(vaultFile.mimeType)) {
            holder.showVideoInfo(vaultFile)
            Glide.with(holder.mediaView.getContext())
                .using(glideLoader)
                .load(VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(holder.mediaView)
        }
        when(holder){
            is AttachmentsViewHolder -> {holder.bind(vaultFile = vaultList[position],iGalleryMediaHandler = iGalleryMediaHandler)}
            is GridAttachmentsViewHolder -> {holder.bind(vaultFile = vaultList[position],iGalleryMediaHandler = iGalleryMediaHandler)}
        }
    }

    override fun getItemCount() = vaultList.size

    fun submitList(vaultList: List<VaultFile?>) {
        this.vaultList = vaultList
        notifyItemChanged(0)
    }

    override fun getItemViewType(position: Int): Int {
        return if (layoutManager.spanCount == 1) ViewType.DETAILED.ordinal
        else ViewType.SMALL.ordinal
    }

    fun setLayoutManager(gridLayoutManager : GridLayoutManager){
        layoutManager = gridLayoutManager
    }

    fun setSelectedMediaFiles(selectedMediaFiles: List<VaultFile>) {
        selected.addAll(selectedMediaFiles)
        notifyItemChanged(0)
    }

    fun deselectMediaFile(vaultFile: VaultFile) {
        if (!selected.contains(vaultFile)) {
            return
        }
        selected.remove(vaultFile)
       // notifyItemChanged(files.indexOf(vaultFile))
    }

   inner class AttachmentsViewHolder(view: View)  : BaseAttachmentViewHolder(view){

        constructor(parent: ViewGroup)
                : this(LayoutInflater.from(parent.context).inflate(R.layout.item_vault_attachment_hor, parent, false))

        private lateinit var dateTextView : TextView;
        private lateinit var nameFileTextVew : TextView
        private lateinit var filePreviewImg : ImageView
        private lateinit var moreBtn : View

        fun bind(vaultFile: VaultFile?,iGalleryMediaHandler: IGalleryMediaHandler){
            view.apply {
                dateTextView = findViewById(R.id.fileDateTextView)
                nameFileTextVew = findViewById(R.id.fileNameTextView)
                filePreviewImg = findViewById(R.id.attachmentImg)
                moreBtn = findViewById(R.id.more)
            }
            vaultFile?.apply {
                dateTextView.text = created.toString()
                nameFileTextVew.text = name
            }

            moreBtn.setOnClickListener {
                vaultFile?.let { it1 -> iGalleryMediaHandler.playMedia(it1) }
            }

        }

       override fun showVideoInfo(vaultFile: VaultFile) {
           TODO("Not yet implemented")
       }

       override fun showAudioInfo(vaultFile: VaultFile) {
           TODO("Not yet implemented")
       }

       override fun showImageInfo() {
           TODO("Not yet implemented")
       }


   }


    class GridAttachmentsViewHolder (view: View)  : BaseAttachmentViewHolder(view) {

        constructor(parent: ViewGroup)
                : this(LayoutInflater.from(parent.context).inflate(R.layout.item_vault_attachment_grid, parent, false))

        private lateinit var fileNameTextView : TextView
        private lateinit var filePreviewImg : ImageView
        private lateinit var moreBtn : View
        private lateinit var icAttachmentImg : ImageView

        fun bind(vaultFile: VaultFile?, iGalleryMediaHandler: IGalleryMediaHandler){
            view.apply {
                fileNameTextView = findViewById(R.id.fileNameTextView)
                filePreviewImg = findViewById(R.id.attachmentImg)
                moreBtn = findViewById(R.id.more)
                icAttachmentImg = findViewById(R.id.icAttachmentImg)
            }
            vaultFile?.apply {
                fileNameTextView.text = name
            }

            moreBtn.setOnClickListener { vaultFile?.let { it1 -> iGalleryMediaHandler.playMedia(it1) } }
        }

        override fun showVideoInfo(vaultFile: VaultFile) {
        }

        override fun showAudioInfo(vaultFile: VaultFile) {
        }

        override fun showImageInfo() {

        }

    }

    abstract class BaseAttachmentViewHolder (val view: View)  : RecyclerView.ViewHolder(view){
        abstract fun showVideoInfo(vaultFile: VaultFile)
        abstract fun showAudioInfo(vaultFile: VaultFile)
        abstract fun showImageInfo()
    }

}