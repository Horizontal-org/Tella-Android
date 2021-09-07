package rs.readahead.washington.mobile.views.fragment.vault.attachements

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityCompat
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
import org.hzontal.shared_ui.breadcrumb.BreadcrumbsView
import org.hzontal.shared_ui.breadcrumb.DefaultBreadcrumbsCallback
import org.hzontal.shared_ui.breadcrumb.model.BreadcrumbItem
import org.hzontal.shared_ui.breadcrumb.model.Item
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.EventObserver
import rs.readahead.washington.mobile.bus.event.MediaFileDeletedEvent
import rs.readahead.washington.mobile.bus.event.VaultFileRenameEvent
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.util.C
import rs.readahead.washington.mobile.util.DialogsUtil
import rs.readahead.washington.mobile.views.activity.*
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.custom.SpacesItemDecoration
import rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments.AttachmentsRecycleViewAdapter
import rs.readahead.washington.mobile.views.fragment.vault.home.VAULT_FILTER
import rs.readahead.washington.mobile.views.fragment.vault.info.VAULT_FILE_INFO_TOOLBAR
import timber.log.Timber
import java.util.*

const val VAULT_FILE_ARG = "VaultFileArg"
const val WRITE_REQUEST_CODE = 1002

class AttachmentsFragment : BaseFragment(), View.OnClickListener,
    rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments.IGalleryVaultHandler,
    IAttachmentsPresenter.IView {
    private lateinit var attachmentsRecyclerView: RecyclerView
    private val attachmentsAdapter by lazy {
        AttachmentsRecycleViewAdapter(
            activity, this,
            MediaFileHandler(), gridLayoutManager
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
    private var vaultFile: VaultFile? = null
    private lateinit var breadcrumbView: BreadcrumbsView
    private var currentRootID: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vault_attachments, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (isListCheckOn) inflater.inflate(R.menu.home_menu_selected, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_more -> {
                if (attachmentsAdapter.selectedMediaFiles.size>0){
                    showFileActionsSheet(null, true)
                }
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
                if (hasStoragePermissions(activity)) {
                    exportVaultFiles()
                } else {
                    requestStoragePermissions(WRITE_REQUEST_CODE)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setToolbarLabel() {
        when (filterType) {
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
        breadcrumbView = view.findViewById(R.id.breadcrumbs_view)
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
        listCheck.setOnClickListener(this)
        gridCheck.setOnClickListener(this)
        checkBoxList.setOnClickListener(this)
        filterNameTv.setOnClickListener(this)
        toolbar.backClickListener = {
            if (attachmentsAdapter.selectedMediaFiles.size > 0) {
                attachmentsAdapter.clearSelected()
                updateAttachmentsToolbar(false)
            } else {
                nav().navigateUp()
            }
        }
        setUpToolbar()
        initData()
        setUpBreadCrumb()
    }

    private fun initData() {
        arguments?.getString(VAULT_FILTER)?.let {
            filterType = FilterType.valueOf(it)
        }
        initSorting()
        setToolbarLabel()
        attachmentsPresenter.getRootId()
        onFileDeletedEventListener()
        onFileRenameEventListener()
    }

    private fun initSorting() {
        sort = Sort()
        sort.type = Sort.Type.NAME
        sort.direction = Sort.Direction.ASC
    }

    private fun setUpBreadCrumb() {
        breadcrumbView.setCallback(object : DefaultBreadcrumbsCallback<BreadcrumbItem?>() {
            override fun onNavigateBack(item: BreadcrumbItem?, position: Int) {
                if (position == 0) {
                    breadcrumbView.visibility = View.GONE
                }
                currentRootID = item?.items?.get(item.selectedIndex)?.id
                attachmentsPresenter.getFiles(currentRootID, filterType, sort)
            }

            override fun onNavigateNewLocation(newItem: BreadcrumbItem?, changedPosition: Int) {
                showToast(changedPosition.toString())
                currentRootID = newItem?.items?.get(newItem.selectedIndex)?.id
                attachmentsPresenter.getFiles(currentRootID, filterType, sort)
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.gridCheck -> {
                gridCheck.toggleVisibility(false)
                listCheck.toggleVisibility(true)
                gridLayoutManager.spanCount = 4
                attachmentsAdapter.setLayoutManager(gridLayoutManager)
                attachmentsAdapter.notifyItemRangeChanged(0, attachmentsAdapter.itemCount)
            }
            R.id.listCheck -> {
                gridCheck.toggleVisibility(true)
                listCheck.toggleVisibility(false)
                gridLayoutManager.spanCount = 1
                attachmentsAdapter.setLayoutManager(gridLayoutManager)
                attachmentsAdapter.notifyItemRangeChanged(0, attachmentsAdapter.itemCount)
            }
            R.id.checkBoxList -> {
                isListCheckOn = !isListCheckOn
                attachmentsAdapter.enableSelectMode(isListCheckOn)
                if (!isListCheckOn) {
                    attachmentsAdapter.clearSelected()
                    updateAttachmentsToolbar(false)
                }
            }
            R.id.filterNameTv -> {
                handleSortSheet()
            }
            R.id.fab_button -> {
                VaultSheetUtils.showVaultManageFilesSheet(
                    activity.supportFragmentManager,
                    getString(R.string.vault_take_photo_video),
                    getString(R.string.vault_record_audio),
                    getString(R.string.vault_import_from_device),
                    getString(R.string.vault_import_delete_file),
                    getString(R.string.vault_create_new_folder),
                    getString(R.string.vault_manage_files),
                    action = object : VaultSheetUtils.IVaultManageFiles {
                        override fun goToCamera() {
                            activity.startActivity(Intent(activity, CameraActivity::class.java))
                        }

                        override fun goToRecorder() {
                            activity.startActivity(
                                Intent(
                                    activity,
                                    AudioRecordActivity2::class.java
                                )
                            )

                        }

                        override fun import() {
                            MediaFileHandler.startImportFiles(activity,true)
                        }

                        override fun importAndDelete() {

                        }

                        override fun createFolder() {
                            VaultSheetUtils.showVaultRenameSheet(
                                activity.supportFragmentManager,
                                getString(R.string.vault_rename_file),
                                getString(R.string.action_cancel),
                                getString(R.string.action_ok),
                                requireActivity(),
                                null
                            ) {
                                currentRootID?.let { root ->
                                    attachmentsPresenter.createFolder(
                                        it,
                                        root
                                    )
                                }

                            }

                        }

                    }
                )
            }
        }
    }

    private fun updateAttachmentsToolbar(isItemsChecked: Boolean) {
        activity.invalidateOptionsMenu()

        if (isItemsChecked) {
            toolbar.setToolbarNavigationIcon(R.drawable.ic_close_white_24dp)
            toolbar.setStartTextTitle(attachmentsAdapter.selectedMediaFiles.size.toString() + " items")
        } else {
            toolbar.setToolbarNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
            setToolbarLabel()
        }
    }

    override fun playMedia(vaultFile: VaultFile) {
        when (vaultFile.type) {
            VaultFile.Type.DIRECTORY -> {
                if (currentRootID != vaultFile.id){
                    currentRootID = vaultFile.id
                    attachmentsPresenter.getFiles(currentRootID, filterType, sort)
                    breadcrumbView.visibility = View.VISIBLE
                    breadcrumbView.addItem(createItem(vaultFile))
                }
            }
            VaultFile.Type.FILE -> {
                if (vaultFile.mimeType != null) {
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
                    }
                }
            }
            else -> {

            }
        }


    }

    override fun onSelectionNumChange(num: Int) {
    }

    override fun onMediaSelected(vaultFile: VaultFile) {
        updateAttachmentsToolbar(!attachmentsAdapter.selectedMediaFiles.isNullOrEmpty())
    }

    override fun onMediaDeselected(vaultFile: VaultFile) {
        updateAttachmentsToolbar(!attachmentsAdapter.selectedMediaFiles.isNullOrEmpty())
    }

    override fun onMoreClicked(vaultFile: VaultFile) {
        showFileActionsSheet(vaultFile, false)
    }
    private fun showFileActionsSheet(vaultFile: VaultFile?, isMultipleFiles: Boolean)
    {
        VaultSheetUtils.showVaultActionsSheet(activity.supportFragmentManager,
            getSheetName(vaultFile,isMultipleFiles),
            getString(R.string.action_upload),
            getString(R.string.action_share),
            getString(R.string.vault_move_to_another_folder),
            getString(R.string.vault_rename),
            getString(R.string.action_save),
            getString(R.string.vault_file_information),
            getString(R.string.action_delete),
            isDirectory = vaultFile?.type == VaultFile.Type.DIRECTORY,
            isMultipleFiles = isMultipleFiles,
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
                        requireActivity(),
                        vaultFile?.name
                    ) {
                        vaultFile?.let { it1 -> attachmentsPresenter.renameVaultFile(it1.id, it) }
                    }
                }

                override fun save() {
                    this@AttachmentsFragment.vaultFile = vaultFile
                    if (hasStoragePermissions(activity)) {
                        if (isMultipleFiles){
                           exportVaultFiles()
                        }else{
                            vaultFile?.let { exportVaultFile(it) }
                        }
                    } else {
                        requestStoragePermissions(WRITE_REQUEST_CODE)
                    }
                }

                override fun info() {
                    vaultFile.let {
                        val bundle = Bundle()
                        bundle.putSerializable(VAULT_FILE_ARG, it)
                        bundle.putBoolean(VAULT_FILE_INFO_TOOLBAR,true)
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
                                if (isMultipleFiles){
                                    attachmentsPresenter.deleteVaultFiles(attachmentsAdapter.selectedMediaFiles)
                                }else{
                                    attachmentsPresenter.deleteVaultFile(vaultFile)
                                }

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

    override fun onMediaImported(vaultFile: List<VaultFile?>) {
        attachmentsPresenter.addNewVaultFiles()
    }

    override fun onImportError(error: Throwable?) {
    }

    override fun onImportStarted() {

    }

    override fun onImportEnded() {
    }

    override fun onMediaFilesAdded() {
        attachmentsPresenter.getFiles(currentRootID, filterType, sort)
    }

    override fun onMediaFilesAddingError(error: Throwable?) {
    }

    override fun onMediaFilesDeleted(num: Int) {
        attachmentsPresenter.getFiles(currentRootID, filterType, sort)
    }

    override fun onMediaFilesDeletionError(throwable: Throwable?) {
    }

    override fun onMediaFileDeleted() {
        attachmentsPresenter.getFiles(currentRootID, filterType, sort)
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
        attachmentsPresenter.getFiles(currentRootID, filterType, sort)
    }

    override fun onRenameFileError(error: Throwable?) {
        DialogUtils.showBottomMessage(activity, error?.localizedMessage, true)
    }

    override fun onCreateFolderSuccess() {
        attachmentsPresenter.getFiles(currentRootID, filterType, sort)

    }

    override fun onCreateFolderError(error: Throwable?) {

    }

    override fun onGetRootIdSuccess(vaultFile: VaultFile?) {
        currentRootID = vaultFile?.id
        breadcrumbView.addItem(BreadcrumbItem.createSimpleItem(Item("Origin", vaultFile?.id)))
        attachmentsPresenter.getFiles(currentRootID, filterType, sort)
    }

    override fun onGetRootIdError(error: Throwable?) {

    }

    private fun exportVaultFiles() {
        val selected: List<VaultFile> = attachmentsAdapter.selectedMediaFiles
        attachmentsPresenter.exportMediaFiles(selected)
    }

    private fun exportVaultFile(vaultFile: VaultFile) {
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
        if (selected.isNullOrEmpty()) return
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
        if (requestCode == WRITE_REQUEST_CODE) {
            context?.let {
                if (hasStoragePermissions(it)) {
                    if (attachmentsAdapter.selectedMediaFiles.size == 0)
                        vaultFile?.let { it1 -> exportVaultFile(it1) }
                    else
                        exportVaultFiles()
                }

            }
        }
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

    private fun handleSortSheet() {
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
                    attachmentsPresenter.getFiles(currentRootID, filterType, sort)
                }

                override fun onSortDateDESC() {
                    filterNameTv.text = getString(R.string.vault_sort_date_desc)
                    sort.type = Sort.Type.DATE
                    sort.direction = Sort.Direction.DESC
                    attachmentsPresenter.getFiles(currentRootID, filterType, sort)
                }

                override fun onSortNameDESC() {
                    filterNameTv.text = getString(R.string.vault_sort_name_desc)
                    sort.type = Sort.Type.NAME
                    sort.direction = Sort.Direction.DESC
                    attachmentsPresenter.getFiles(currentRootID, filterType, sort)
                }

                override fun onSortNameASC() {
                    filterNameTv.text = getString(R.string.vault_sort_name_asc)
                    sort.type = Sort.Type.NAME
                    sort.direction = Sort.Direction.ASC
                    attachmentsPresenter.getFiles(currentRootID, filterType, sort)
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
            C.IMPORT_MULTIPLE_FILES -> {
                if (null != data) {
                    val listVaultFilesUris = arrayListOf<Uri?>()
                    if (data.clipData != null) {
                        for (i in 0 until data.clipData?.itemCount!!) {
                            val uri = data.clipData?.getItemAt(i)?.uri
                            listVaultFilesUris.add(uri)
                        }
                    } else {
                        data.data?.let { returnedUri ->
                            listVaultFilesUris.add(returnedUri)
                        }
                    }
                    attachmentsPresenter.importVaultFiles(listVaultFilesUris,currentRootID)
            }}
            WRITE_REQUEST_CODE -> {
                vaultFile?.let { exportVaultFile(vaultFile = it) }
            }
        }
    }

    fun hasStoragePermissions(context: Context): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        )
            return true
        return false
    }

    fun requestStoragePermissions(requestCode: Int) {
        requestPermissions(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), requestCode
        )
    }

    private fun createItem(file: VaultFile): BreadcrumbItem {
        val list: MutableList<Item> = ArrayList()
        list.add(Item(file.name, file.id))
        return BreadcrumbItem(list)
    }
    private fun  getSheetName(vaultFile: VaultFile?,isMultipleFiles : Boolean) : String? {
        return if (isMultipleFiles){
            attachmentsAdapter.selectedMediaFiles.size.toString() + " " + "items"
        } else {
            vaultFile?.name
        }
    }

}