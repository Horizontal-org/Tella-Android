package rs.readahead.washington.mobile.views.fragment.vault.attachements

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.hzontal.tella_locking_ui.common.extensions.toggleVisibility
import com.hzontal.tella_vault.Vault
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Sort
import com.hzontal.utils.MediaFile.isAudioFileType
import com.hzontal.utils.MediaFile.isImageFileType
import com.hzontal.utils.MediaFile.isVideoFileType
import kotlinx.coroutines.launch
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionConfirmed
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.RadioOptionConsumer
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showChooseImportSheet
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showConfirmSheet
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showProgressImportSheet
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showRadioListOptionsSheet
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils
import org.hzontal.shared_ui.breadcrumb.DefaultBreadcrumbsCallback
import org.hzontal.shared_ui.breadcrumb.model.BreadcrumbItem
import org.hzontal.shared_ui.breadcrumb.model.Item
import org.hzontal.shared_ui.pinview.ResourceUtils.getColor
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.EventObserver
import rs.readahead.washington.mobile.bus.event.CaptureEvent
import rs.readahead.washington.mobile.bus.event.MediaFileDeletedEvent
import rs.readahead.washington.mobile.bus.event.VaultFileRenameEvent
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.databinding.FragmentVaultAttachmentsBinding
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.util.*
import rs.readahead.washington.mobile.views.activity.*
import rs.readahead.washington.mobile.views.activity.AudioPlayActivity.PLAY_MEDIA_FILE_ID_KEY
import rs.readahead.washington.mobile.views.activity.CameraActivity.VAULT_CURRENT_ROOT_PARENT
import rs.readahead.washington.mobile.views.activity.PhotoViewerActivity.VIEW_PHOTO
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments.AttachmentsRecycleViewAdapter
import rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments.IGalleryVaultHandler
import rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers.*
import rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers.AttachmentsHelper.getCurrentType
import rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers.AttachmentsHelper.hasStoragePermissions
import rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers.AttachmentsHelper.setToolbarLabel
import rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers.AttachmentsHelper.shareVaultFile
import rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers.AttachmentsHelper.shareVaultFiles
import rs.readahead.washington.mobile.views.fragment.vault.home.VAULT_FILTER
import rs.readahead.washington.mobile.views.fragment.vault.info.VaultInfoFragment.Companion.VAULT_FILE_INFO_TOOLBAR


class AttachmentsFragment :
    BaseBindingFragment<FragmentVaultAttachmentsBinding>(FragmentVaultAttachmentsBinding::inflate),
    View.OnClickListener, IGalleryVaultHandler,OnNavBckListener {
    private var gridLayoutManager = GridLayoutManager(activity, 1)
    private val attachmentsAdapter by lazy {
        AttachmentsRecycleViewAdapter(activity, this, MediaFileHandler(), gridLayoutManager)
    }
    private val viewModel: AttachmentsViewModel by viewModels()
    private val moveModeUIUpdater by lazy { MoveModeUIUpdater(binding) }
    private var progressDialog: ProgressDialog? = null
    private val disposables by lazy { MyApplication.bus().createCompositeDisposable() }
    private var filterType = FilterType.ALL
    private lateinit var sort: Sort
    private var vaultFile: VaultFile? = null
    private var currentRootID: String? = null
    private var currentMove: String? = null
    private var isListCheckOn = false
    private var isMoveModeEnabled = false
    private var importAndDelete = false
    private var uriToDelete: Uri? = null
    private val bundle by lazy { Bundle() }
    private var withMetadata = false
    private var selectMode = SelectMode.DESELECT_ALL

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (isListCheckOn && !isMoveModeEnabled) {
            inflater.inflate(R.menu.home_menu_selected, menu)
            maybeShowUploadIcon(menu)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        initView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_more -> {
                if (attachmentsAdapter.selectedMediaFiles.size > 0) {
                    showFileActionsSheet(null, true)
                }
                true
            }
            R.id.action_share -> {
                baseActivity.maybeChangeTemporaryTimeout {
                    shareVaultFiles(attachmentsAdapter.selectedMediaFiles, baseActivity)
                }
                return true
            }
            R.id.action_upload -> {
                vaultFile = null
                performFileSearch(true, vaultFile)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setUpToolbar() {
        val activity = context as MainActivity
        activity.setSupportActionBar(binding.toolbar)
    }

    private fun initView() {

        if (SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.appbar.outlineProvider = null
        } else {
            binding.appbar.bringToFront()
        }
        gridLayoutManager = GridLayoutManager(activity, 1)
        binding.attachmentsRecyclerView.apply {
            adapter = attachmentsAdapter
            layoutManager = gridLayoutManager
        }
        // enableMoveTheme()
        initListeners()
        setUpToolbar()
        initData()
        setUpBreadCrumb()
        initObservers()
    }

    private fun initListeners() {
        binding.fabButton.setOnClickListener(this)
        binding.listCheck.setOnClickListener(this)
        binding.gridCheck.setOnClickListener(this)
        binding.checkBoxList.setOnClickListener(this)
        binding.moveHere.setOnClickListener(null)
        binding.fabMoveButton.setOnClickListener(this)
        binding.cancelMove.setOnClickListener(this)
        binding.filterGroup.setOnClickListener(this)
    }

    private fun initData() {
        arguments?.getString(VAULT_FILTER)?.let {
            filterType = FilterType.valueOf(it)
        }
        initSorting()
        setToolbarLabel(filterType, binding.toolbar, baseActivity)
        viewModel.getRootId()
        onFileDeletedEventListener()
        onFileRenameEventListener()
        onCaptureEventListener()
        initViewType()
    }

    private fun initSorting() {
        sort = Sort()
        sort.type = Sort.Type.NAME
        sort.direction = Sort.Direction.ASC
    }

    private fun initViewType() {
        if (filterType == FilterType.PHOTO_VIDEO) {
            setGridView()
        }
    }

    private fun setUpBreadCrumb() {
        binding.breadcrumbsView.setCallback(object : DefaultBreadcrumbsCallback<BreadcrumbItem?>() {
            override fun onNavigateBack(item: BreadcrumbItem?, position: Int) {
                handleBreadcrumbNavigation(item, position)
            }

            override fun onNavigateNewLocation(newItem: BreadcrumbItem?, changedPosition: Int) {
                showToast(changedPosition.toString())
                handleBreadcrumbNavigation(newItem, changedPosition)
            }
        })
    }

    private fun handleBreadcrumbNavigation(item: BreadcrumbItem?, position: Int) {
        if (position == 0) {
            binding.breadcrumbsView.visibility = View.GONE
        }

        currentRootID = item?.items?.get(item.selectedIndex)?.id
        onMediaFilesAdded()
        if (isMoveModeEnabled) {
            highlightMoveBackground()
        }
    }

    private fun setGridView() {
        binding.gridCheck.toggleVisibility(false)
        binding.listCheck.toggleVisibility(true)
        gridLayoutManager.spanCount = 4
        attachmentsAdapter.setLayoutManager(gridLayoutManager)
        attachmentsAdapter.notifyItemRangeChanged(0, attachmentsAdapter.itemCount)
        binding.attachmentsRecyclerView.setMargins(leftMarginDp = 13, rightMarginDp = 13)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.gridCheck -> setGridView()
            R.id.listCheck -> setListView()
            R.id.checkBoxList -> handleSelectMode()
            R.id.filterGroup -> handleSortSheet()
            R.id.fab_button -> handleFabButtonClick()
            R.id.fab_move_button -> handleFabMoveButtonClick()
            R.id.moveHere -> handleMoveHereClick()
            R.id.cancelMove -> handleCancelMoveClick()
        }
    }

    private fun setListView() {
        binding.gridCheck.toggleVisibility(true)
        binding.listCheck.toggleVisibility(false)
        gridLayoutManager.spanCount = 1
        attachmentsAdapter.setLayoutManager(gridLayoutManager)
        attachmentsAdapter.notifyItemRangeChanged(0, attachmentsAdapter.itemCount)
        binding.attachmentsRecyclerView.setMargins(leftMarginDp = 0, rightMarginDp = 0)
    }

    private fun handleFabButtonClick() {
        VaultSheetUtils.showVaultManageFilesSheet(
            baseActivity.supportFragmentManager,
            getString(R.string.Vault_TakePhotoVideo_SheetAction),
            getString(R.string.Vault_RecordAudio_SheetAction),
            getString(R.string.Vault_Import_SheetAction),
            getString(R.string.Vault_CreateFolder_SheetAction),
            getString(R.string.Vault_ManageFiles_SheetTitle),
            filterType != FilterType.OTHERS,
            filterType == FilterType.ALL,
            action = createVaultManageFilesAction()
        )
    }

    private fun handleFabMoveButtonClick() {
        VaultSheetUtils.showVaultBlueRenameSheet(
            baseActivity.supportFragmentManager,
            getString(R.string.Vault_CreateFolder_SheetAction),
            getString(R.string.action_cancel),
            getString(R.string.action_ok),
            baseActivity,
            null
        ) {
            currentRootID?.let { root ->
                viewModel.createFolder(it, root)
            }
        }
    }

    private fun handleMoveHereClick() {
        if (attachmentsAdapter.selectedMediaFiles.size > 0) {
            viewModel.moveFiles(
                currentRootID, attachmentsAdapter.selectedMediaFiles
            )
        }
    }

    private fun handleCancelMoveClick() {
        isMoveModeEnabled = false
        enableMoveTheme(enable = false)
        selectMode = SelectMode.SELECT_ALL
        handleSelectMode()
    }

    private fun createVaultManageFilesAction(): VaultSheetUtils.IVaultManageFiles {
        return object : VaultSheetUtils.IVaultManageFiles {
            override fun goToCamera() {
                val intent = Intent(activity, CameraActivity::class.java)
                intent.putExtra(VAULT_CURRENT_ROOT_PARENT, currentRootID)
                baseActivity.startActivity(intent)
            }

            override fun goToRecorder() {
                bundle.putString(VAULT_CURRENT_ROOT_PARENT, currentRootID)
                nav().navigate(R.id.action_attachments_screen_to_micScreen, bundle)
            }

            override fun chooseImportAndDelete() {
                showChooseImportSheet(
                    baseActivity.supportFragmentManager,
                    getString(R.string.Vault_ImportDelete_SheetAction),
                    getString(R.string.Vault_deleteFileImported_SheetDesc),
                    getString(R.string.Vault_Delete_Original),
                    getString(R.string.Vault_Keep_Original),
                    importConsumer = createImportActionConfirmed(),
                    importAndDeleteConsumer = createImportAndDeleteActionConfirmed()
                )
            }

            override fun createFolder() {
                VaultSheetUtils.showVaultRenameSheet(
                    baseActivity.supportFragmentManager,
                    getString(R.string.Vault_CreateFolder_SheetAction),
                    getString(R.string.action_cancel),
                    getString(R.string.action_ok),
                    requireActivity(),
                    null
                ) {
                    currentRootID?.let { root ->
                        viewModel.createFolder(it, root)
                    }
                }
            }
        }
    }

    private fun createImportActionConfirmed(): ActionConfirmed {
        return object : ActionConfirmed {
            override fun accept(isConfirmed: Boolean) {
                // First step in importing files
                importAndDelete = false
                baseActivity.maybeChangeTemporaryTimeout {
                    MediaFileHandler.startImportFiles(
                        activity, true, getCurrentType(filterType)
                    )
                }
            }
        }
    }

    private fun createImportAndDeleteActionConfirmed(): ActionConfirmed {
        return object : ActionConfirmed {
            override fun accept(isConfirmed: Boolean) {
                importAndDelete = true
                baseActivity.maybeChangeTemporaryTimeout {
                    MediaFileHandler.startImportFiles(
                        activity, true, getCurrentType(filterType)
                    )
                }
            }
        }
    }

    private fun handleSelectMode() {
        changeSelectMode()
        attachmentsAdapter.enableSelectMode(isListCheckOn)
        updateAttachmentsToolbar(isListCheckOn)
        baseActivity.invalidateOptionsMenu()

        when (selectMode) {
            SelectMode.DESELECT_ALL -> {
                attachmentsAdapter.clearSelected()
                enableMoveTheme(false)
                binding.checkBoxList.setCheckDrawable(R.drawable.ic_check, baseActivity)
            }
            SelectMode.ONE_SELECTION -> {
                binding.checkBoxList.setCheckDrawable(R.drawable.ic_check_box_off, baseActivity)
            }
            SelectMode.SELECT_ALL -> {
                binding.checkBoxList.setCheckDrawable(R.drawable.ic_check_box_on, baseActivity)
                attachmentsAdapter.selectAll()
            }
        }
    }

    private fun changeSelectMode() {
        when (selectMode) {
            SelectMode.DESELECT_ALL -> {
                isListCheckOn = true
                selectMode = SelectMode.ONE_SELECTION

            }
            SelectMode.ONE_SELECTION -> {
                isListCheckOn = true
                selectMode = SelectMode.SELECT_ALL

            }
            SelectMode.SELECT_ALL -> {
                isListCheckOn = false
                selectMode = SelectMode.DESELECT_ALL
            }
        }
    }

    private fun updateAttachmentsToolbar(isItemsChecked: Boolean) {
        baseActivity.invalidateOptionsMenu()

        if (isItemsChecked) {
            val itemsSize = attachmentsAdapter.selectedMediaFiles.size
            binding.toolbar.setToolbarNavigationIcon(R.drawable.ic_close_white_24dp)
            if (itemsSize == 0) {
                binding.toolbar.setStartTextTitle(getString(R.string.Vault_Select_Title))
            } else {
                binding.toolbar.setStartTextTitle(
                    attachmentsAdapter.selectedMediaFiles.size.toString() + " " + getString(
                        R.string.Vault_Items
                    )
                )
            }
        } else {
            binding.toolbar.setToolbarNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
            setToolbarLabel(filterType, binding.toolbar, baseActivity)
            attachmentsAdapter.clearSelected()
            enableMoveTheme(false)
        }
    }

    override fun playMedia(vaultFile: VaultFile) {

        when (vaultFile.type) {
            VaultFile.Type.DIRECTORY -> handleDirectory(vaultFile)
            VaultFile.Type.FILE -> handleFile(vaultFile)
            else -> {}
        }
    }

    private fun handleDirectory(vaultFile: VaultFile) {
        if (isMoveModeEnabled && attachmentsAdapter.selectedMediaFiles.contains(vaultFile)) return

        if (!isMoveModeEnabled) {
            attachmentsAdapter.clearSelected()
            isListCheckOn = false
            updateAttachmentsToolbar(false)
        }
        openDirectory(vaultFile)
    }

    private fun handleFile(vaultFile: VaultFile) {
        vaultFile.mimeType?.let {
            when {
                isImageFileType(it) -> openActivity<PhotoViewerActivity>(vaultFile)
                isAudioFileType(it) -> openActivity<AudioPlayActivity>(vaultFile.id)
                isVideoFileType(it) -> openActivity<VideoViewerActivity>(vaultFile)
                else -> showBottomSheet(vaultFile)
            }
        }
    }

    private inline fun <reified T : Activity> openActivity(fileId: String) {
        val intent = Intent(baseActivity, T::class.java).apply {
            putExtra(PLAY_MEDIA_FILE_ID_KEY, fileId)
        }
        startActivity(intent)
    }

    private inline fun <reified T : Activity> openActivity(vaultFile: VaultFile) {
        val intent = Intent(baseActivity, T::class.java).apply {
            putExtra(VIEW_PHOTO, vaultFile)
        }
        startActivity(intent)
    }

    private fun showBottomSheet(vaultFile: VaultFile) {
        BottomSheetUtils.showStandardSheet(
            baseActivity.supportFragmentManager,
            baseActivity.getString(R.string.Vault_Export_SheetAction) + " " + vaultFile.name + "?",
            baseActivity.getString(R.string.Vault_ViewerOther_SheetDesc),
            baseActivity.getString(R.string.Vault_Export_SheetAction),
            baseActivity.getString(R.string.action_cancel),
            onConfirmClick = {
                this.vaultFile = vaultFile
                performFileSearch(false, vaultFile)
            }
        )
    }


    private fun openDirectory(vaultFile: VaultFile) {
        if (currentRootID != vaultFile.id) {
            currentRootID = vaultFile.id
            highlightMoveBackground()
            onMediaFilesAdded()
            binding.breadcrumbsView.visibility = View.VISIBLE
            binding.breadcrumbsView.addItem(createItem(vaultFile))
        }
    }

    override fun onSelectionNumChange(num: Int) {
    }

    override fun onMediaSelected(vaultFile: VaultFile) {
        handleSelectionModeWhenMediSelected()
    }

    override fun onMediaDeselected(vaultFile: VaultFile) {
        handleSelectionModeWhenMediSelected()
    }

    private fun handleSelectionModeWhenMediSelected() {
        updateAttachmentsToolbar(true)
        if (attachmentsAdapter.selectedMediaFiles.isNullOrEmpty() && selectMode == SelectMode.SELECT_ALL) {
            selectMode = SelectMode.DESELECT_ALL
            handleSelectMode()
        } else if (attachmentsAdapter.selectedMediaFiles.size == attachmentsAdapter.itemCount && selectMode != SelectMode.SELECT_ALL) {
            selectMode = SelectMode.ONE_SELECTION
            handleSelectMode()
        } else if (attachmentsAdapter.selectedMediaFiles.size < attachmentsAdapter.itemCount && selectMode == SelectMode.SELECT_ALL) {
            selectMode = SelectMode.DESELECT_ALL
            handleSelectMode()
        }
    }

    override fun onMoreClicked(vaultFile: VaultFile) {
        showFileActionsSheet(vaultFile, false)
    }

    private fun showFileActionsSheet(vaultFile: VaultFile?, isMultipleFiles: Boolean) {
        VaultSheetUtils.showVaultActionsSheet(baseActivity.supportFragmentManager,
            getSheetName(vaultFile, isMultipleFiles),
            getString(R.string.Vault_Upload_SheetAction),
            getString(R.string.Vault_Share_SheetAction),
            getString(R.string.Vault_Move_SheetDesc),
            getString(R.string.Vault_Rename_SheetAction),
            getString(R.string.gallery_action_desc_save_to_device),
            getString(R.string.Vault_File_SheetAction),
            getString(R.string.Vault_Delete_SheetAction),
            isDirectory = vaultFile?.type == VaultFile.Type.DIRECTORY,
            isMultipleFiles = isMultipleFiles,
            isUploadVisible = false,
            isMoveVisible = filterType == FilterType.ALL,
            action = object : VaultSheetUtils.IVaultActions {
                override fun upload() {
                }

                override fun share() {
                    baseActivity.maybeChangeTemporaryTimeout {
                        if (attachmentsAdapter.selectedMediaFiles.size > 0) {
                            shareVaultFiles(attachmentsAdapter.selectedMediaFiles, baseActivity)
                        } else {
                            shareVaultFile(vaultFile, baseActivity)
                        }
                    }
                }

                override fun move() {
                    currentMove = currentRootID
                    if (vaultFile != null) {
                        isListCheckOn = true
                        attachmentsAdapter.enableSelectMode(isListCheckOn)
                        attachmentsAdapter.selectMediaFile(vaultFile)
                        updateAttachmentsToolbar(true)
                    }
                    isMoveModeEnabled = true
                    enableMoveTheme(enable = true)
                }

                override fun rename() {
                    VaultSheetUtils.showVaultRenameSheet(
                        baseActivity.supportFragmentManager,
                        getString(R.string.Vault_RenameFile_SheetTitle),
                        getString(R.string.action_cancel),
                        getString(R.string.action_ok),
                        requireActivity(),
                        vaultFile?.name
                    ) {
                        vaultFile?.let { it1 -> viewModel.renameVaultFile(it1.id, it) }
                    }
                }

                override fun save() {
                    showConfirmSheet(baseActivity.supportFragmentManager,
                        getString(R.string.gallery_save_to_device_dialog_title),
                        getString(R.string.gallery_save_to_device_dialog_expl),
                        getString(R.string.action_save),
                        getString(R.string.action_cancel),
                        consumer = object : ActionConfirmed {
                            override fun accept(isConfirmed: Boolean) {
                                this@AttachmentsFragment.vaultFile = vaultFile
                                exportVaultFilesWithMetadataCheck(vaultFile)
                            }
                        })
                }

                override fun info() {
                    vaultFile.let {
                        val bundle = Bundle()
                        bundle.putSerializable(VAULT_FILE_ARG, it)
                        bundle.putBoolean(VAULT_FILE_INFO_TOOLBAR, true)
                        nav().navigate(R.id.action_attachments_screen_to_info_screen, bundle)
                    }
                }

                override fun delete() {
                    showConfirmSheet(baseActivity.supportFragmentManager,
                        getString(R.string.Vault_DeleteFile_SheetTitle),
                        getString(R.string.Vault_deleteFile_SheetDesc),
                        getString(R.string.action_delete),
                        getString(R.string.action_cancel),
                        consumer = object : ActionConfirmed {
                            override fun accept(isConfirmed: Boolean) {
                                if (isConfirmed) {
                                    if (isMultipleFiles) {
                                        viewModel.deleteFilesAfterConfirmation(
                                            attachmentsAdapter.selectedMediaFiles
                                        )
                                    } else {
                                        val files = mutableListOf<VaultFile?>()
                                        files.add(vaultFile)
                                        viewModel.deleteFilesAfterConfirmation(files)
                                    }
                                }

                            }
                        })

                }
            })

    }

    private fun initObservers() {
        with(viewLifecycleOwner) {
            viewModel.filesData.observe(this, ::onGetFilesSuccess)
            viewModel.filesSize.observe(this, ::onMoveFilesSuccess)
            viewModel.moveFilesError.observe(this, ::onMoveFilesError)
            viewModel.deletedFiles.observe(this, ::onMediaFilesDeleted)
            viewModel.deletedFile.observe(this, ::onMediaFileDeleted)
            viewModel.deletedFileError.observe(this, ::onMediaFileDeletionError)
            viewModel.folderCreated.observe(this, ::onCreateFolderSuccess)
            viewModel.rootId.observe(this, ::onGetRootIdSuccess)
            viewModel.progressPercent.observe(this, ::onGetProgressPercent)
            viewModel.mediaImportedWithDelete.observe(this, ::onMediaImportedWithDelete)
            viewModel.mediaImported.observe(this, ::onMediaImported)
            viewModel.renameFileSuccess.observe(this, ::onRenameFileSuccess)
            viewModel.exportState.observe(this, ::onExportStarted)
            viewModel.mediaExported.observe(this, ::onMediaExported)
            viewModel.onConfirmDeleteFiles.observe(this, ::onConfirmDeleteFiles)
        }
    }

    private fun onGetFilesSuccess(files: List<VaultFile?>) {
        val recyclerView = binding.attachmentsRecyclerView
        val emptyViewContainer = binding.emptyViewMsgContainer

        if (files.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyViewContainer.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyViewContainer.visibility = View.GONE
        }

        attachmentsAdapter.setFiles(files)
        attachmentsAdapter.enableSelectMode(isListCheckOn)
    }

    private fun onMediaImported(vaultFile: VaultFile) {
        onMediaFilesAdded()
    }

    private fun onMediaImportedWithDelete(uri: Uri) {
        lifecycleScope.launch {
            uri.let {
                deleteFileFromExternalStorage(it)
                uriToDelete = it
            }
        }

        onMediaFilesAdded()
    }

    private fun onConfirmDeleteFiles(deleteState: Pair<List<VaultFile?>, Boolean>) {
        val vaultFiles = deleteState.first
        val showConfirmDelete = deleteState.second
        if (showConfirmDelete) {
            showConfirmSheet(baseActivity.supportFragmentManager,
                getString(R.string.Vault_Warning_Title),
                getString(R.string.Vault_Confirm_delete_Description),
                getString(R.string.Vault_Delete_anyway),
                getString(R.string.action_cancel),
                consumer = object : ActionConfirmed {
                    override fun accept(isConfirmed: Boolean) {
                        if (isConfirmed) {
                            deleteFiles(vaultFiles)
                        }
                    }

                })
        } else {
            deleteFiles(vaultFiles)
        }
    }

    private fun deleteFiles(vaultFiles: List<VaultFile?>) {
        if (vaultFiles.isEmpty()) return

        if (vaultFiles.size > 1) {
            viewModel.deleteVaultFiles(attachmentsAdapter.selectedMediaFiles)
        } else {
            vaultFiles[0]?.let { viewModel.deleteVaultFile(it) }
        }
    }

    private fun onMediaFilesAdded() {
        viewModel.getFiles(currentRootID, filterType, sort)
    }

    private fun onMediaFilesDeleted(num: Int) {
        onMediaFilesAdded()
        selectMode = SelectMode.SELECT_ALL
        handleSelectMode()
    }

    private fun onMediaFileDeleted(file: VaultFile) {
        onMediaFilesAdded()
        selectMode = SelectMode.SELECT_ALL
        handleSelectMode()
    }

    private fun onMediaFileDeletionError(throwable: Throwable?) {
        DialogUtils.showBottomMessage(
            baseActivity, getString(R.string.gallery_toast_fail_deleting_files), true
        )
    }

    private fun onMediaExported(num: Int) {
        DialogUtils.showBottomMessage(
            baseActivity,
            resources.getQuantityString(R.plurals.gallery_toast_files_exported, num, num),
            false
        )

    }

    private fun onExportStarted(state: Boolean) {
        if (state) {
            progressDialog = DialogsUtil.showProgressDialog(
                baseActivity, getString(R.string.gallery_save_to_device_dialog_progress_expl)
            )
            binding.fabButton.hide()
        } else {
            hideProgressDialog()
            binding.fabButton.show()
        }
    }

    private fun onRenameFileSuccess(vaultFile: VaultFile) {
        onMediaFilesAdded()
        enableMoveTheme(false)
    }

    private fun onCreateFolderSuccess(vaultFile: VaultFile) {
        onMediaFilesAdded()
    }

    private fun onGetRootIdSuccess(vaultFile: VaultFile?) {
        currentRootID = vaultFile?.id
        binding.breadcrumbsView.addItem(BreadcrumbItem.createSimpleItem(Item("", vaultFile?.id)))
        onMediaFilesAdded()
    }

    private fun onMoveFilesSuccess(filesSize: Int) {
        onMediaFilesAdded()
        enableMoveTheme(false)
        currentMove = null
        selectMode = SelectMode.SELECT_ALL
        handleSelectMode()
        DialogUtils.showBottomMessage(
            baseActivity,
            resources.getQuantityString(R.plurals.Vault_File_Successfully_Moved, filesSize),
            false
        )
    }

    private fun onMoveFilesError(error: Throwable?) {
        enableMoveTheme(false)
        currentMove = null
        selectMode = SelectMode.SELECT_ALL
        handleSelectMode()
    }


    private fun onGetProgressPercent(progressPercent: Pair<Double, Int>) {
        val numberFilesImported = progressPercent.first
        val totalFilesToImport = progressPercent.second
        showProgressImportSheet(
            baseActivity.supportFragmentManager,
            getString(R.string.Vault_Importing_SheetTitle),
            totalFilesToImport,
            resources.getQuantityString(
                R.plurals.Vault_Importing_SheetProgress, totalFilesToImport
            ),
            viewModel.counterData,
            getString(R.string.action_cancel).uppercase(),
            viewLifecycleOwner
        ) {
            viewModel.cancelImportVaultFiles()
        }
    }

    private fun exportVaultFiles(isMultipleFiles: Boolean, vaultFile: VaultFile?, path: Uri?) {
        if (isMultipleFiles) {
            val selected: List<VaultFile> = attachmentsAdapter.selectedMediaFiles
            viewModel.exportMediaFiles(withMetadata, selected, path)
        } else {
            vaultFile?.let {
                viewModel.exportMediaFiles(withMetadata, arrayListOf(vaultFile), path)
            }
        }
    }

    private fun hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog?.dismiss()
            progressDialog = null
        }
    }

    @SuppressLint("NeedOnRequestPermissionsResult")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WRITE_REQUEST_CODE) {
            context?.let {
                performFileSearch(attachmentsAdapter.selectedMediaFiles.size == 0, vaultFile)
            }
            LockTimeoutManager().lockTimeout = Preferences.getLockTimeout()
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
                    onMediaFilesAdded()
                    enableMoveTheme(false)
                }
            })
    }

    private fun onCaptureEventListener() {
        disposables.wire(CaptureEvent::class.java, object : EventObserver<CaptureEvent?>() {
            override fun onNext(event: CaptureEvent) {
                onMediaFilesAdded()
            }
        })
    }

    private fun handleSortSheet() {
        VaultSheetUtils.showVaultSortSheet(
            baseActivity.supportFragmentManager,
            getString(R.string.gallery_subheading_sort_by),
            getString(R.string.Vault_SortNameAsc_SheetAction),
            getString(R.string.Vault_SortNameDesc_SheetAction),
            getString(R.string.Vault_SortDateAsc_SheetAction),
            getString(R.string.Vault_SortDateDesc_SheetAction),
            sort = createVaultSortActions()
        )
    }

    private fun performSort(sortType: Sort.Type, sortDirection: Sort.Direction) {
        binding.filterNameTv.text = when (sortType) {
            Sort.Type.DATE -> {
                when (sortDirection) {
                    Sort.Direction.ASC -> getString(R.string.Vault_SortDateAsc_SheetAction)
                    Sort.Direction.DESC -> getString(R.string.Vault_SortDateDesc_SheetAction)
                }
            }
            Sort.Type.NAME -> {
                when (sortDirection) {
                    Sort.Direction.ASC -> getString(R.string.Vault_SortNameAsc_SheetAction)
                    Sort.Direction.DESC -> getString(R.string.Vault_SortNameDesc_SheetAction)
                }
            }
        }

        sort.type = sortType
        sort.direction = sortDirection
        onMediaFilesAdded()
    }

    private fun createVaultSortActions(): VaultSheetUtils.IVaultSortActions {
        return object : VaultSheetUtils.IVaultSortActions {
            override fun onSortDateASC() = performSort(Sort.Type.DATE, Sort.Direction.ASC)
            override fun onSortDateDESC() = performSort(Sort.Type.DATE, Sort.Direction.DESC)
            override fun onSortNameDESC() = performSort(Sort.Type.NAME, Sort.Direction.DESC)
            override fun onSortNameASC() = performSort(Sort.Type.NAME, Sort.Direction.ASC)
        }
    }

    private fun isLocationSettingsRequestCode(requestCode: Int): Boolean {
        return requestCode == C.START_CAMERA_CAPTURE || requestCode == C.START_AUDIO_RECORD
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!isLocationSettingsRequestCode(requestCode) && resultCode != Activity.RESULT_OK) {
            return  // user canceled evidence acquiring
        }
        when (requestCode) {
            C.IMPORT_MULTIPLE_FILES -> handleImportMultipleFiles(data)
            C.CAMERA_CAPTURE, C.RECORDED_AUDIO -> onMediaFilesAdded()
            WRITE_REQUEST_CODE -> performFileSearch(false, vaultFile)
            PICKER_FILE_REQUEST_CODE -> handlePickerFileRequest(data)
        }
    }

    private fun handleImportMultipleFiles(data: Intent?) {
        if (data != null) {
            val listVaultFilesUris = arrayListOf<Uri>()
            if (data.clipData != null) {
                for (i in 0 until data.clipData!!.itemCount) {
                    val uri = data.clipData!!.getItemAt(i).uri
                    listVaultFilesUris.add(uri)
                }
            } else {
                data.data?.let { returnedUri ->
                    listVaultFilesUris.add(returnedUri)
                }
            }
            viewModel.importVaultFiles(
                listVaultFilesUris, currentRootID, importAndDelete
            )
        }
    }

    private fun handlePickerFileRequest(data: Intent?) {
        val treeUri = data?.data
        treeUri?.apply {
            baseActivity.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        exportVaultFiles(
            isMultipleFiles = attachmentsAdapter.selectedMediaFiles.isNotEmpty(),
            vaultFile,
            treeUri
        )
    }

    private fun requestStoragePermissions() {
        baseActivity.maybeChangeTemporaryTimeout()
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data =
                    Uri.parse(String.format("package:%s", baseActivity.application.packageName))
                startActivityForResult(intent, WRITE_REQUEST_CODE)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, WRITE_REQUEST_CODE)
            }
        } else {
            //below android 11
            ActivityCompat.requestPermissions(
                baseActivity, arrayOf(WRITE_EXTERNAL_STORAGE), WRITE_REQUEST_CODE
            )
        }

    }

    private fun createItem(file: VaultFile): BreadcrumbItem {
        val list: MutableList<Item> = ArrayList()
        list.add(Item(file.name, file.id))
        return BreadcrumbItem(list)
    }

    private fun getSheetName(vaultFile: VaultFile?, isMultipleFiles: Boolean): String? {
        return if (isMultipleFiles) {
            attachmentsAdapter.selectedMediaFiles.size.toString() + " " + "items"
        } else {
            vaultFile?.name
        }
    }

    private fun handleOnBackPressed() {
        binding.toolbar.backClickListener = {
            handleBackStack()
        }
        (activity as MainActivity).onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackStack()
                }
            })
    }

    private fun handleBackStack() {
        val selectedFilesSize = attachmentsAdapter.selectedMediaFiles.size
        val breadcrumbsSize = binding.breadcrumbsView.items.size

        when {
            selectedFilesSize > 0 || (selectedFilesSize == 0 && isListCheckOn) -> {
                selectMode = SelectMode.SELECT_ALL
                handleSelectMode()
            }
            breadcrumbsSize > 1 -> {
                handleBreadcrumbs(breadcrumbsSize)
            }
            filterType == FilterType.PHOTO_VIDEO -> {
                navigateToCameraAndFinish()
            }
            else -> {
                nav().navigateUp()
            }
        }
    }

    private fun handleBreadcrumbs(size: Int) {
        if (size == 2) {
            binding.breadcrumbsView.visibility = View.GONE
        }
        binding.breadcrumbsView.removeLastItem()
        currentRootID = binding.breadcrumbsView.getCurrentItem<BreadcrumbItem>().selectedItem.id
        onMediaFilesAdded()
    }

    private fun navigateToCameraAndFinish() {
        nav().navigate(R.id.action_attachments_screen_to_camera)
        baseActivity.finish()
    }

    private fun enableMoveTheme(enable: Boolean) {
        isMoveModeEnabled = enable
        moveModeUIUpdater.updateUI(enable, baseActivity)
        attachmentsAdapter.enableMoveMode(enable)
    }

    private fun highlightMoveBackground() {
        if (currentMove != currentRootID) {
            binding.moveHere.setOnClickListener(this)
            binding.moveHere.setTextColor(getColor(activity, R.color.wa_white))
        } else {
            binding.moveHere.setOnClickListener(null)
            binding.moveHere.setTextColor(getColor(activity, R.color.wa_white_12))
        }
    }

    private fun deleteFileFromExternalStorage(uri: Uri) {
        val fileToDelete = DocumentFile.fromSingleUri(baseActivity, uri)
        try {
            fileToDelete?.delete()
        } catch (exn: java.lang.Exception) {
            exn.printStackTrace()
        }
    }

    private fun maybeShowUploadIcon(menu: Menu) {
        menu.findItem(R.id.action_upload).isVisible = false
    }

    override fun onResume() {
        super.onResume()
        handleOnBackPressed()
    }

    override fun onBackPressed(): Boolean {
        handleBackStack()
        return true
    }

    private fun performFileSearch(isMultipleFiles: Boolean, vaultFile: VaultFile?) {
        if (hasStoragePermissions(baseActivity)) {
            if (SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivityForResult(intent, PICKER_FILE_REQUEST_CODE)
            } else {
                exportVaultFiles(isMultipleFiles, vaultFile, null)
            }
        } else {
            requestStoragePermissions()
        }
    }

    private fun exportVaultFilesWithMetadataCheck(vaultFile: VaultFile?) {
        val selected: List<VaultFile> = attachmentsAdapter.selectedMediaFiles
        val isMultipleFiles = selected.isNotEmpty()
        val withMetadata =
            selected.any { file -> file.metadata != null } || vaultFile?.metadata != null

        if (withMetadata) {
            showExportWithMetadataDialog(isMultipleFiles, vaultFile)
        } else {
            baseActivity.maybeChangeTemporaryTimeout {
                performFileSearch(isMultipleFiles, vaultFile)
            }
        }
    }

    private fun showExportWithMetadataDialog(isMultipleFiles: Boolean, vaultFile: VaultFile?) {
        val options = LinkedHashMap<Int, Int>()
        options[1] = R.string.verification_share_select_media_and_verification
        options[0] = R.string.verification_share_select_only_media
        showRadioListOptionsSheet(baseActivity.supportFragmentManager,
            requireContext(),
            options,
            getString(R.string.verification_share_dialog_title),
            getString(R.string.verification_share_dialog_expl),
            getString(R.string.action_ok),
            getString(R.string.action_cancel),
            object : RadioOptionConsumer {
                override fun accept(option: Int) {
                    withMetadata = option > 0
                    baseActivity.maybeChangeTemporaryTimeout {
                        performFileSearch(isMultipleFiles, vaultFile)
                    }
                }
            })
    }
}
