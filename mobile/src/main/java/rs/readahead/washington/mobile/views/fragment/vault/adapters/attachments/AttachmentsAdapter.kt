package rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.R

class AttachmentsAdapter constructor(iGalleryMediaHandler: IGalleryMediaHandler) :
     RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var vaultList: List<VaultFile> = arrayListOf()
    private val selected = LinkedHashSet<VaultFile>()
    private lateinit var layoutManager: GridLayoutManager
    enum class ViewType {
        SMALL,
        DETAILED
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.DETAILED.ordinal -> AttachmentsViewHolder(parent)
            else ->GridAttachmentsViewHolder(parent)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val vaultFile = vaultList[position]
       /* when(holder){
            is AttachmentsViewHolder -> {holder.bind(vaultFile = vaultList[position], clickListener = clickListener)}
            is GridAttachmentsViewHolder -> {holder.bind(vaultFile = vaultList[position], clickListener = clickListener)}
        }*/
    }

    override fun getItemCount() = vaultList.size

    fun submitList(vaultList: List<VaultFile>) {
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

   inner class AttachmentsViewHolder(val view: View)  : RecyclerView.ViewHolder(view){

        constructor(parent: ViewGroup)
                : this(LayoutInflater.from(parent.context).inflate(R.layout.item_vault_attachment_hor, parent, false))

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
    }


    class GridAttachmentsViewHolder (val view: View)  : RecyclerView.ViewHolder(view) {

        constructor(parent: ViewGroup)
                : this(LayoutInflater.from(parent.context).inflate(R.layout.item_vault_attachment_grid, parent, false))

        private lateinit var fileNameTextView : TextView
        private lateinit var filePreviewImg : ImageView
        private lateinit var moreBtn : View
        private lateinit var icAttachmentImg : ImageView

        fun bind(vaultFile: VaultFile, clickListener: (VaultFile) -> Unit){
            view.apply {
                fileNameTextView = findViewById(R.id.fileNameTextView)
                filePreviewImg = findViewById(R.id.attachmentImg)
                moreBtn = findViewById(R.id.more)
                icAttachmentImg = findViewById(R.id.icAttachmentImg)
            }
            vaultFile.apply {
                fileNameTextView.text = name
            }

            moreBtn.setOnClickListener { clickListener.invoke(vaultFile) }
        }

    }

}