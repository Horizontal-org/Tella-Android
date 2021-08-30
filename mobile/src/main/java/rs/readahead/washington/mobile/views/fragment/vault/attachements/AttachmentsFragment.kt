package rs.readahead.washington.mobile.views.fragment.vault.attachements

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hzontal.tella_locking_ui.common.extensions.toggleVisibility
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Sort
import com.hzontal.utils.MediaFile.isAudioFileType
import com.hzontal.utils.MediaFile.isImageFileType
import com.hzontal.utils.MediaFile.isVideoFileType
import org.hzontal.shared_ui.appbar.ToolbarComponent
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionConfirmed
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showConfirmSheet
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.OnPermissionDenied
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.EventObserver
import rs.readahead.washington.mobile.bus.event.MediaFileDeletedEvent
import rs.readahead.washington.mobile.bus.event.VaultFileRenameEvent
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.util.C
import rs.readahead.washington.mobile.util.DialogsUtil
import rs.readahead.washington.mobile.util.FileUtil
import rs.readahead.washington.mobile.views.activity.AudioPlayActivity
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.activity.PhotoViewerActivity
import rs.readahead.washington.mobile.views.activity.VideoViewerActivity
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.base_ui.BaseToolbarFragment
import rs.readahead.washington.mobile.views.custom.SpacesItemDecoration
import rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments.AttachmentsRecycleViewAdapter
import rs.readahead.washington.mobile.views.fragment.vault.home.VAULT_FILTER
import rs.readahead.washington.mobile.views.settings.OnFragmentSelected
import timber.log.Timber

const val VAULT_FILE_ARG = "VaultFileArg"

class AttachmentsFragment : BaseFragment(), View.OnClickListener,
    rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments.IGalleryVaultHandler,
    IAttachmentsPresenter.IView {
    private lateinit var attachmentsRecyclerView: RecyclerView
    private val attachmentsAdapter by lazy {
        AttachmentsRecycleViewAdapter(
            activity, this,
            MediaFileHandler(),gridLayoutManager
        )
    }
    private val attachmentsPresenter by lazy { AttachmentsPresenter(this) }
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var detailsFab: FloatingActionButton
    private lateinit var toolbar: ToolbarComponent
    private lateinit var listCheck: ImageView
    private lateinit var gridCheck: ImageView
    private lateinit var filterNameTv: TextView
    private lateinit var emptyViewMsgContainer: LinearLayout
    private lateinit var checkBoxList: AppCompatImageView
    private var isListCheckOn = false
    private var progressDialog: ProgressDialog? = null
    private val disposables by lazy { MyApplication.bus().createCompositeDisposable() }
    private var filterType = FilterType.ALL
    private lateinit var sort: Sort

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
                startShareActivity(false)
                return true
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

    private fun setToolbarLabel() {
        when(filterType){
            FilterType.PHOTO -> toolbar.setStartTextTitle("Images")
            FilterType.VIDEO -> toolbar.setStartTextTitle("Videos")
            FilterType.AUDIO -> toolbar.setStartTextTitle("Audios")
            FilterType.DOCUMENTS -> toolbar.setStartTextTitle("Documents")
            FilterType.OTHERS -> toolbar.setStartTextTitle("Others")
            FilterType.ALL -> toolbar.setStartTextTitle("All files")
        }
    }
     private fun setUpToolbar() {
        val activity = context as MainActivity
        activity.setSupportActionBar(toolbar)
    }

    override fun initView(view: View) {
        attachmentsRecyclerView = view.findViewById(R.id.attachmentsRecyclerView)
        attachmentsRecyclerView.addItemDecoration(SpacesItemDecoration(5))
        listCheck = view.findViewById(R.id.listCheck)
        gridCheck = view.findViewById(R.id.gridCheck)
        emptyViewMsgContainer = view.findViewById(R.id.emptyViewMsgContainer)
        filterNameTv = view.findViewById(R.id.filterNameTv)
        toolbar = view.findViewById(R.id.toolbar)
        gridLayoutManager = GridLayoutManager(activity, 1)
        attachmentsRecyclerView.apply {
            adapter = attachmentsAdapter
            layoutManager = gridLayoutManager
        }
        detailsFab = view.findViewById(R.id.fab_button)
        checkBoxList = view.findViewById(R.id.checkBoxList)

        detailsFab.setOnClickListener(this)
        toolbar.backClickListener = {
            if (isListCheckOn) {
                isListCheckOn != isListCheckOn
                updateAttachmentsToolbar()
            } else {
                nav().navigateUp()
            }
        }
        listCheck.setOnClickListener(this)
        gridCheck.setOnClickListener(this)
        checkBoxList.setOnClickListener(this)
        filterNameTv.setOnClickListener(this)
        setUpToolbar()
        initData()
    }

    private fun initData() {
         arguments?.getString(VAULT_FILTER)?.let {
             filterType  =  FilterType.valueOf(it)
        }
        initSorting()
        setToolbarLabel()
        attachmentsPresenter.getFiles(filterType, sort)
        onFileDeletedEventListener()
        onFileRenameEventListener()
    }
    private fun initSorting(){
        sort = Sort()
        sort.type = Sort.Type.NAME
        sort.direction = Sort.Direction.ASC
    }

    private fun onFabDetailsClick() {

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.gridCheck -> {
                gridCheck.toggleVisibility(false)
                listCheck.toggleVisibility(true)
                gridLayoutManager.spanCount = 4
                attachmentsAdapter.setLayoutManager(gridLayoutManager);
                attachmentsAdapter.notifyItemRangeChanged(0, attachmentsAdapter.itemCount)
            }
            R.id.listCheck -> {
                gridCheck.toggleVisibility(true)
                listCheck.toggleVisibility(false)
                gridLayoutManager.spanCount = 1
                attachmentsAdapter.setLayoutManager(gridLayoutManager);
                attachmentsAdapter.notifyItemRangeChanged(0, attachmentsAdapter.itemCount)
            }
            R.id.checkBoxList -> {
                isListCheckOn = !isListCheckOn
                attachmentsAdapter.enableSelectMode(isListCheckOn)
                updateAttachmentsToolbar()
            }
            R.id.filterNameTv ->{
                handleSortSheet()
            }
            R.id.fab_button -> {
                MediaFileHandler.startSelectMediaActivity(
                    activity,
                    "image/*",
                    arrayOf("image/*", "video/mp4"),
                    C.IMPORT_MEDIA
                )
            }
        }
    }

    private fun updateAttachmentsToolbar() {
        activity.invalidateOptionsMenu()

        if (isListCheckOn) {
            toolbar.setToolbarNavigationIcon(R.drawable.ic_close_white_24dp)
            toolbar.setStartTextTitle(attachmentsAdapter.selectedMediaFiles.size.toString() + " items")
        } else {
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
                if (vaultFile.type == VaultFile.Type.DIRECTORY) {

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
                    MediaFileHandler.startShareActivity(activity, vaultFile, false)
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
                        attachmentsPresenter.renameVaultFile(vaultFile.id, it)
                    }
                }

                override fun save() {
                }

                override fun info() {
                    vaultFile.let {
                        val bundle = Bundle()
                        bundle.putSerializable(VAULT_FILE_ARG, it)
                        nav().navigate(R.id.action_attachments_screen_to_info_screen, bundle)
                    }
                }

                override fun delete() {
                    showConfirmSheet(
                        activity.supportFragmentManager,
                        getString(R.string.vault_delete_file),
                        getString(R.string.vault_delete_file_msg),
                        getString(R.string.action_delete),
                        getString(R.string.action_cancel),
                        consumer = object : ActionConfirmed {
                            override fun accept(isConfirmed: Boolean) {
                                attachmentsPresenter.deleteVaultFile(vaultFile)
                            }
                        }
                    )

                }

            }

        )
    }

    override fun onGetFilesStart() {
    }

    override fun onGetFilesEnd() {
    }

    override fun onGetFilesSuccess(files: List<VaultFile?>) {
        if (files.isEmpty()) {
            attachmentsRecyclerView.visibility = View.GONE
            emptyViewMsgContainer.visibility = View.VISIBLE
        } else {
            attachmentsRecyclerView.visibility = View.VISIBLE
            emptyViewMsgContainer.visibility = View.GONE
        }
        attachmentsAdapter.setFiles(files)
    }

    override fun onGetFilesError(error: Throwable?) {
        Timber.d(error, javaClass.name)
    }

    override fun onMediaImported(vaultFile: VaultFile?) {
        attachmentsPresenter.addNewVaultFile(vaultFile)
    }

    override fun onImportError(error: Throwable?) {
    }

    override fun onImportStarted() {
    }

    override fun onImportEnded() {
    }

    override fun onMediaFilesAdded(vaultFile: VaultFile?) {
        attachmentsPresenter.getFiles(filterType,sort)
    }

    override fun onMediaFilesAddingError(error: Throwable?) {
    }

    override fun onMediaFilesDeleted(num: Int) {
        attachmentsPresenter.getFiles(filterType, null)
    }

    override fun onMediaFilesDeletionError(throwable: Throwable?) {
    }

    override fun onMediaFileDeleted() {
        attachmentsPresenter.getFiles(filterType, null)
    }

    override fun onMediaFileDeletionError(throwable: Throwable?) {
        DialogUtils.showBottomMessage(
            activity,
            getString(R.string.gallery_toast_fail_deleting_files),
            true
        )
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
        attachmentsPresenter.getFiles(filterType, null)
    }

    override fun onRenameFileError(error: Throwable?) {
        DialogUtils.showBottomMessage(activity, error?.localizedMessage, true)
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun onWriteExternalStoragePermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun onWriteExternalStorageNeverAskAgain() {
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun exportVaultFiles() {
        val selected: List<VaultFile> = attachmentsAdapter.selectedMediaFiles
        attachmentsPresenter.exportMediaFiles(selected)
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun exportVaultFile(vaultFile: VaultFile) {
        attachmentsPresenter.exportMediaFiles(arrayListOf(vaultFile))
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

    @SuppressLint("NeedOnRequestPermissionsResult")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun onFileDeletedEventListener() {
        disposables.wire(
            MediaFileDeletedEvent::class.java,
            object : EventObserver<MediaFileDeletedEvent?>() {
                override fun onNext(event: MediaFileDeletedEvent) {
                    onMediaFilesDeleted(1)
                }
            })
    }
    private fun onFileRenameEventListener() {
        disposables.wire(
            VaultFileRenameEvent::class.java,
            object : EventObserver<VaultFileRenameEvent?>() {
                override fun onNext(event: VaultFileRenameEvent) {
                    onRenameFileSuccess()
                }
            })
    }
    private fun handleSortSheet(){
        VaultSheetUtils.showVaultSortSheet(
            activity.supportFragmentManager,
            getString(R.string.gallery_subheading_sort_by),
            getString(R.string.vault_sort_name_asc),
            getString(R.string.vault_sort_name_desc),
            getString(R.string.vault_sort_date_asc),
            getString(R.string.vault_sort_date_desc),
            sort = object : VaultSheetUtils.IVaultSortActions {
                override fun onSortDateASC() {
                    filterNameTv.text = getString(R.string.vault_sort_date_asc)
                    sort.type = Sort.Type.DATE
                    sort.direction = Sort.Direction.ASC
                    attachmentsPresenter.getFiles(filterType, sort)
                }

                override fun onSortDateDESC() {
                    filterNameTv.text = getString(R.string.vault_sort_date_desc)
                    sort.type = Sort.Type.DATE
                    sort.direction = Sort.Direction.DESC
                    attachmentsPresenter.getFiles(filterType, sort)
                }

                override fun onSortNameDESC() {
                    filterNameTv.text = getString(R.string.vault_sort_name_desc)
                    sort.type = Sort.Type.NAME
                    sort.direction = Sort.Direction.DESC
                    attachmentsPresenter.getFiles(filterType, sort)
                }

                override fun onSortNameASC() {
                    filterNameTv.text = getString(R.string.vault_sort_name_asc)
                    sort.type = Sort.Type.NAME
                    sort.direction = Sort.Direction.ASC
                    attachmentsPresenter.getFiles(filterType, sort)
                }

            }

        )
    }

    private fun isLocationSettingsRequestCode(requestCode: Int): Boolean {
        return requestCode == C.START_CAMERA_CAPTURE ||
                requestCode == C.START_AUDIO_RECORD
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!isLocationSettingsRequestCode(requestCode) && resultCode != Activity.RESULT_OK) {
            return  // user canceled evidence acquiring
        }
        when (requestCode) {
            C.IMPORT_IMAGE -> {
                val image = data?.data
                if (image != null) {
                    attachmentsPresenter.importImage(image)
                }
            }
            C.IMPORT_VIDEO -> {
                val video = data?.data
                if (video != null) {
                    attachmentsPresenter.importVideo(video)
                }
            }
            C.IMPORT_MEDIA -> {
                val media = data?.data ?: return
                val type = FileUtil.getPrimaryMime(activity.contentResolver.getType(media))
                if ("image" == type) {
                    attachmentsPresenter.importImage(media)
                } else if ("video" == type) {
                    attachmentsPresenter.importVideo(media)
                }
            }
            C.CAMERA_CAPTURE, C.RECORDED_AUDIO -> {
            }
        }
    }


}