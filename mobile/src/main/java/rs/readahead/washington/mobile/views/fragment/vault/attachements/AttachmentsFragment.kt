package rs.readahead.washington.mobile.views.fragment.vault.attachements

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hzontal.tella_locking_ui.common.extensions.toggleVisibility
import com.hzontal.tella_vault.VaultFile
import com.hzontal.utils.MediaFile.isAudioFileType
import com.hzontal.utils.MediaFile.isImageFileType
import com.hzontal.utils.MediaFile.isVideoFileType
import org.hzontal.shared_ui.appbar.ToolbarComponent
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import permissions.dispatcher.NeedsPermission
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.util.DialogsUtil
import rs.readahead.washington.mobile.views.activity.AudioPlayActivity
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.activity.PhotoViewerActivity
import rs.readahead.washington.mobile.views.activity.VideoViewerActivity
import rs.readahead.washington.mobile.views.base_ui.BaseToolbarFragment
import rs.readahead.washington.mobile.views.custom.SpacesItemDecoration
import rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments.AttachmentsRecycleViewAdapter
import timber.log.Timber

const val VAULT_FILE_ARG = "VaultFileArg"
class AttachmentsFragment : BaseToolbarFragment(), View.OnClickListener, rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments.IGalleryVaultHandler, IAttachmentsPresenter.IView{
    private lateinit var attachmentsRecyclerView: RecyclerView
    private val attachmentsAdapter by lazy { AttachmentsRecycleViewAdapter(activity, this,
         MediaFileHandler(), R.layout.item_vault_attachment_hor)}
    private val attachmentsPresenter by lazy { AttachmentsPresenter(this) }
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var detailsFab: FloatingActionButton
    private lateinit var toolbar: ToolbarComponent
    private lateinit var listCheck: ImageView
    private lateinit var gridCheck: ImageView
    private lateinit var emptyViewMsgContainer : LinearLayout
    private lateinit var checkBoxList : AppCompatImageView
    private var isListCheckOn = false
    private var progressDialog : ProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vault_attachments, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (isListCheckOn) inflater.inflate(R.menu.home_menu_selected, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_more -> {
                true
            }
            R.id.action_share -> {
                true
            }

            R.id.action_check -> {
                true
            }

            R.id.action_upload -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun setToolbarLabel(labelRes: Int) {

    }

    override fun setToolbarHomeIcon(iconRes: Int) {
    }

    override fun setUpToolbar() {
        val activity = context as MainActivity
        activity.setSupportActionBar(toolbar)
    }

    override fun initView(view: View) {
        attachmentsRecyclerView = view.findViewById(R.id.attachmentsRecyclerView)
        attachmentsRecyclerView.addItemDecoration(SpacesItemDecoration(5))
        listCheck = view.findViewById(R.id.listCheck)
        gridCheck = view.findViewById(R.id.gridCheck)
        emptyViewMsgContainer = view.findViewById(R.id.emptyViewMsgContainer)
        toolbar = view.findViewById(R.id.toolbar)
        gridLayoutManager = GridLayoutManager(activity, 1)
        attachmentsRecyclerView.apply {
            adapter = attachmentsAdapter
            layoutManager = gridLayoutManager
        }
        detailsFab = view.findViewById(R.id.detailsFab)
        checkBoxList = view.findViewById(R.id.checkBoxList)

        detailsFab.setOnClickListener { onFabDetailsClick() }
        toolbar.backClickListener = {
            if (isListCheckOn){
                isListCheckOn != isListCheckOn
                updateAttachmentsToolbar() }
            else{ nav().navigateUp() }
        }
        listCheck.setOnClickListener(this)
        gridCheck.setOnClickListener(this)
        checkBoxList.setOnClickListener(this)
        initData()
    }

    private fun initData() {
        attachmentsPresenter.getFiles(null,null)
    }

    private fun onFabDetailsClick() {

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.gridCheck -> {
                gridCheck.toggleVisibility(false)
                listCheck.toggleVisibility(true)
                gridLayoutManager.spanCount = 4
                attachmentsAdapter.notifyItemRangeChanged(0, attachmentsAdapter.itemCount)
            }
            R.id.listCheck -> {
                gridCheck.toggleVisibility(true)
                listCheck.toggleVisibility(false)
                gridLayoutManager.spanCount = 1
                attachmentsAdapter.notifyItemRangeChanged(0, attachmentsAdapter.itemCount)
            }
            R.id.checkBoxList ->{
                isListCheckOn = !isListCheckOn
                attachmentsAdapter.enableSelectMode(isListCheckOn)
                updateAttachmentsToolbar()
            }
        }
    }
    private fun updateAttachmentsToolbar() {
        activity.invalidateOptionsMenu()

        if (isListCheckOn){
            toolbar.setToolbarNavigationIcon(R.drawable.ic_close_white_24dp)
            toolbar.setStartTextTitle(attachmentsAdapter.selectedMediaFiles.size.toString()+" items")
        }else{
            toolbar.setToolbarNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
            toolbar.setStartTextTitle("")
        }
    }

    override fun playMedia(vaultFile: VaultFile) {
        when {
            isImageFileType(vaultFile.mimeType) -> {
                val intent = Intent(activity, PhotoViewerActivity::class.java)
                intent.putExtra(PhotoViewerActivity.VIEW_PHOTO, vaultFile)
                startActivity(intent)
            }
            isAudioFileType(vaultFile.mimeType) -> {
                val intent = Intent(activity, AudioPlayActivity::class.java)
                intent.putExtra(AudioPlayActivity.PLAY_MEDIA_FILE_ID_KEY, vaultFile.id)
                startActivity(intent)
            }
            isVideoFileType(vaultFile.mimeType) -> {
                val intent = Intent(activity, VideoViewerActivity::class.java)
                intent.putExtra(VideoViewerActivity.VIEW_VIDEO, vaultFile)
                startActivity(intent)
            }
            else -> {
                if (vaultFile.type == VaultFile.Type.DIRECTORY){

                }
            }
        }
    }

    override fun onSelectionNumChange(num: Int) {
    }

    override fun onMediaSelected(vaultFile: VaultFile) {
        updateAttachmentsToolbar()
    }

    override fun onMediaDeselected(vaultFile: VaultFile) {
        updateAttachmentsToolbar()
    }

    override fun onMoreClicked(vaultFile: VaultFile) {
       VaultSheetUtils.showVaultActionsSheet(activity.supportFragmentManager,
           vaultFile.name,
           getString(R.string.action_upload),
           getString(R.string.action_share),
           getString(R.string.vault_move_to_another_folder),
           getString(R.string.vault_rename),
           getString(R.string.action_save),
           getString(R.string.vault_file_information),
           getString(R.string.action_delete),
           action = object : VaultSheetUtils.IVaultActions {
               override fun upload() {

               }
               override fun share() {
                   startShareActivity(includeMetadata = false)
               }

               override fun move() {
               }

               override fun rename() {
                   VaultSheetUtils.showVaultRenameSheet(
                       activity.supportFragmentManager,
                       getString(R.string.vault_rename_file),
                       getString(R.string.action_cancel),
                       getString(R.string.action_ok),
                       activity,
                       vaultFile.name
                   ) {
                       attachmentsPresenter.renameVaultFile(vaultFile.id, it) }
               }

               override fun save() {
               }

               override fun info() {
                   vaultFile.let {
                   val bundle = Bundle()
                   bundle.putSerializable(VAULT_FILE_ARG,it)
                    nav().navigate(R.id.action_attachments_screen_to_info_screen,bundle)
                   }
               }

               override fun delete() {
                   attachmentsPresenter.deleteVaultFile(vaultFile)
               }

           }

       )
    }

    override fun onGetFilesStart() {
    }

    override fun onGetFilesEnd() {
    }

    override fun onGetFilesSuccess(files: List<VaultFile?>) {
        if(files.isEmpty()){
            attachmentsRecyclerView.visibility = View.GONE
            emptyViewMsgContainer.visibility = View.VISIBLE
        }else{
            attachmentsRecyclerView.visibility = View.VISIBLE
            emptyViewMsgContainer.visibility = View.GONE
        }
            attachmentsAdapter.setFiles(files)
    }

    override fun onGetFilesError(error: Throwable?) {
        Timber.d(error, javaClass.name)
    }

    override fun onMediaImported(vaultFile: VaultFile?) {
    }

    override fun onImportError(error: Throwable?) {
    }

    override fun onImportStarted() {
    }

    override fun onImportEnded() {
    }

    override fun onMediaFilesAdded(vaultFile: VaultFile?) {
    }

    override fun onMediaFilesAddingError(error: Throwable?) {
    }

    override fun onMediaFilesDeleted(num: Int) {
    }

    override fun onMediaFilesDeletionError(throwable: Throwable?) {
    }

    override fun onMediaFileDeleted() {
        attachmentsPresenter.getFiles(null,null)
    }

    override fun onMediaFileDeletionError(throwable: Throwable?) {
        DialogUtils.showBottomMessage(activity,getString(R.string.gallery_toast_fail_deleting_files),true)
    }

    override fun onMediaExported(num: Int) {
    }

    override fun onExportError(error: Throwable?) {
    }

    override fun onExportStarted() {
        progressDialog = DialogsUtil.showProgressDialog(
            activity,
            getString(R.string.gallery_save_to_device_dialog_progress_expl)
        )
        detailsFab.hide()
    }

    override fun onExportEnded() {
        hideProgressDialog()
        detailsFab.show()
    }

    override fun onCountTUServersEnded(num: Long?) {
    }

    override fun onCountTUServersFailed(throwable: Throwable?) {

    }

    override fun onRenameFileStart() {
        activity.toggleLoading(true)
    }

    override fun onRenameFileEnd() {
        activity.toggleLoading(false)
    }

    override fun onRenameFileSuccess() {
        attachmentsPresenter.getFiles(null,null)
    }

    override fun onRenameFileError(error: Throwable?) {
       DialogUtils.showBottomMessage(activity,error?.localizedMessage,true)
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun exportvaultFiles() {
        val selected: List<VaultFile> = attachmentsAdapter.selectedMediaFiles
        attachmentsPresenter.exportMediaFiles(selected)
    }

    private fun hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog?.dismiss()
            progressDialog = null
        }
    }

    private fun startShareActivity(includeMetadata: Boolean) {
        val selected: List<VaultFile> = attachmentsAdapter.selectedMediaFiles
        if (selected.size > 1) {
            MediaFileHandler.startShareActivity(activity, selected, includeMetadata)
        } else {
            MediaFileHandler.startShareActivity(activity, selected[0], includeMetadata)
        }
    }

    private fun showVaultInfo(){

    }



}