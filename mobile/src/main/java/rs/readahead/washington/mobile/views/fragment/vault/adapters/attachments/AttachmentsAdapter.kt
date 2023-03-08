package rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hzontal.tella_vault.VaultFile
import com.hzontal.utils.MediaFile.isAudioFileType
import com.hzontal.utils.MediaFile.isImageFileType
import com.hzontal.utils.MediaFile.isVideoFileType
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.util.DateUtil
import rs.readahead.washington.mobile.views.interfaces.IGalleryVaultHandler
import java.util.*

class AttachmentsAdapter constructor(private val iGalleryMediaHandler: IGalleryVaultHandler,private val context: Context) :
     RecyclerView.Adapter<AttachmentsAdapter.BaseAttachmentViewHolder>() {
    private var vaultList : MutableList<VaultFile?>
    private val selected = LinkedHashSet<VaultFile>()
    private var selectable : Boolean
    private lateinit var layoutManager: GridLayoutManager
    //private var  glideLoader:VaultFileUrlLoader
    private val mediaFileHandler by lazy { MediaFileHandler() }

    init {
      //  glideLoader = VaultFileUrlLoader(context.applicationContext, mediaFileHandler)
        vaultList = ArrayList()
        selectable = false
    }

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
        val vaultFile = vaultList[position]!!
        when(holder){
            is AttachmentsViewHolder -> {holder.bind(vaultFile = vaultList[position],iGalleryMediaHandler = iGalleryMediaHandler)}
            is GridAttachmentsViewHolder -> {holder.bind(vaultFile = vaultList[position],iGalleryMediaHandler = iGalleryMediaHandler)}
        }


        when {
            isImageFileType(vaultFile.mimeType) -> {
                holder.showImageInfo(vaultFile)

            }
            isAudioFileType(vaultFile.mimeType) -> {
                holder.showAudioInfo(vaultFile)

            }
            isVideoFileType(vaultFile.mimeType) -> {
                holder.showVideoInfo(vaultFile)

            }
        }
        holder.maybeEnableCheckBox(selectable)
    }

    override fun getItemCount() = vaultList.size

        fun submitList(vaultList: List<VaultFile?>) {
        this.vaultList = vaultList.toMutableList()
        notifyItemChanged(0)
    }

    override fun getItemViewType(position: Int): Int {
        return if (layoutManager.spanCount == 1) ViewType.DETAILED.ordinal
        else ViewType.SMALL.ordinal
    }

    fun setLayoutManager(gridLayoutManager : GridLayoutManager){
        layoutManager = gridLayoutManager
    }

    fun setAttachments(attachments: List<VaultFile>) {
        vaultList = attachments.toMutableList()
        notifyDataSetChanged()
    }

    fun prependAttachment(vaultFile: VaultFile) {
        if (vaultList.contains(vaultFile)) {
            return
        }
        vaultList.add(0, vaultFile)
        notifyItemInserted(0)
    }

    fun appendAttachment(vaultFile: VaultFile) {
        if (vaultList.contains(vaultFile)) {
            return
        }
        vaultList.add(vaultFile)
        notifyItemInserted(vaultList.size - 1)
    }

    fun removeAttachment(vaultFile: VaultFile) {
        val position: Int = vaultList.indexOf(vaultFile)
        if (position == -1) {
            return
        }
        vaultList.remove(vaultFile)
        notifyItemRemoved(position)
    }
    @SuppressLint("NotifyDataSetChanged")
    fun enableSelectMode(selectable: Boolean){
        this.selectable = selectable
        notifyDataSetChanged()
    }


    inner class AttachmentsViewHolder(view: View)  : BaseAttachmentViewHolder(view){

        constructor(parent: ViewGroup)
                : this(LayoutInflater.from(parent.context).inflate(R.layout.item_vault_attachment_hor, parent, false))

        private lateinit var dateTextView : TextView;
        private lateinit var nameFileTextVew : TextView
        private lateinit var moreBtn : View

        fun bind(vaultFile: VaultFile?,iGalleryMediaHandler: IGalleryVaultHandler){
            view.apply {
                dateTextView = findViewById(R.id.fileDateTextView)
                nameFileTextVew = findViewById(R.id.fileNameTextView)
                filePreviewImg = findViewById(R.id.attachmentImg)
                moreBtn = findViewById(R.id.more)
                icAttachmentImg = findViewById(R.id.icAttachmentImg)
                checkBox = findViewById(R.id.checkbox_type_single)
            }
            vaultFile?.apply {
                nameFileTextVew.text = name
                dateTextView.text = DateUtil.getDate(created)
            }

            moreBtn.setOnClickListener {
                vaultFile?.let { it1 -> iGalleryMediaHandler.playMedia(it1) }
            }

        }

   }
    inner class GridAttachmentsViewHolder (view: View)  : BaseAttachmentViewHolder(view) {

        constructor(parent: ViewGroup)
                : this(LayoutInflater.from(parent.context).inflate(R.layout.item_vault_attachment_grid, parent, false))
        private lateinit var fileNameTextView : TextView
        private lateinit var moreBtn : View

        fun bind(vaultFile: VaultFile?, iGalleryMediaHandler: IGalleryVaultHandler){
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

            moreBtn.setOnClickListener { vaultFile?.let { it1 -> iGalleryMediaHandler.playMedia(it1) } }
        }

    }
    abstract inner class BaseAttachmentViewHolder (val view: View)  : RecyclerView.ViewHolder(view){
        protected lateinit var icAttachmentImg : ImageView
        protected lateinit var filePreviewImg : ImageView
        protected val context: Context by lazy {view.context}
        protected lateinit var checkBox : CheckBox

        fun showVideoInfo(vaultFile: VaultFile){
            Glide.with(context)
                .load(vaultFile.thumb)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(filePreviewImg)
            icAttachmentImg.setBackgroundResource(R.drawable.ic_play)

        }
        fun showAudioInfo(vaultFile: VaultFile){
           icAttachmentImg.setBackgroundResource(R.drawable.ic_audio_w_small)
        }
        fun showImageInfo(vaultFile: VaultFile){
            Glide.with(filePreviewImg.context)
                .load(vaultFile.thumb)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(filePreviewImg)
        }

        fun maybeEnableCheckBox(selectable: Boolean) {
            checkBox.visibility = if (selectable) View.VISIBLE else View.GONE
            checkBox.isChecked = selectable
        }
    }

}