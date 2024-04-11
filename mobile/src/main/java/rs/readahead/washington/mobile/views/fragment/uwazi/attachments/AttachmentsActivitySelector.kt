package rs.readahead.washington.mobile.views.fragment.uwazi.attachments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hzontal.tella_locking_ui.common.extensions.toggleVisibility
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.utils.MediaFile
import org.hzontal.shared_ui.breadcrumb.DefaultBreadcrumbsCallback
import org.hzontal.shared_ui.breadcrumb.model.BreadcrumbItem
import org.hzontal.shared_ui.breadcrumb.model.Item
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentAttachmentsSelectorBinding
import rs.readahead.washington.mobile.util.setMargins
import rs.readahead.washington.mobile.views.activity.camera.CameraActivity
import rs.readahead.washington.mobile.views.activity.viewer.AudioPlayActivity
import rs.readahead.washington.mobile.views.activity.viewer.PhotoViewerActivity
import rs.readahead.washington.mobile.views.activity.viewer.VideoViewerActivity
import rs.readahead.washington.mobile.views.base_ui.BaseActivity

const val RETURN_ODK = "rodk"
const val VAULT_FILE_KEY = "vfk"
const val VAULT_FILES_FILTER = "vff"
const val VAULT_PICKER_SINGLE = "vps"

class AttachmentsActivitySelector : BaseActivity(), ISelectorVaultHandler, View.OnClickListener {

    private lateinit var binding: FragmentAttachmentsSelectorBinding
    private lateinit var gridLayoutManager: GridLayoutManager
    private val viewModel: AttachmentsSelectorViewModel by viewModels()
    private var currentRootID: String? = null
    private var isMultiplePicker = false
    private var isOdkSelect = false
    private var filterType = FilterType.ALL
    private lateinit var attachmentsAdapter: AttachmentsSelectorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentAttachmentsSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        getSelectedMediaFromIntent()
        initObservers()
        setUpBreadCrumb()
    }

    private fun initView() {
        intent.extras?.apply {
            get(VAULT_FILES_FILTER)?.let { filter -> filterType = filter as FilterType }
            getBoolean(VAULT_PICKER_SINGLE).let { isMultiple -> isMultiplePicker = isMultiple }
            getBoolean(RETURN_ODK).let { isOdk -> isOdkSelect = isOdk }
        }
        gridLayoutManager = GridLayoutManager(this@AttachmentsActivitySelector, 1)

        attachmentsAdapter = AttachmentsSelectorAdapter(
            this@AttachmentsActivitySelector, this,
            gridLayoutManager, true, isMultiplePicker
        )
        with(binding) {

            attachmentsRecyclerView.apply {
                adapter = attachmentsAdapter
                layoutManager = gridLayoutManager
            }
            toolbar.backClickListener = { handleBackStack() }
            gridCheck.setOnClickListener(this@AttachmentsActivitySelector)
            listCheck.setOnClickListener(this@AttachmentsActivitySelector)
            toolbar.onRightClickListener = { setResultAndFinish() }

        }
        updateAttachmentsToolbar(attachmentsAdapter.selectedMediaFiles.size)
    }

    private fun initObservers() {
        with(viewModel) {
            rootVaultFile.observe(this@AttachmentsActivitySelector) { vaultFile ->
                vaultFile?.let { root ->
                    currentRootID = root.id
                    getFiles(root.id, filterType, null)
                    binding.breadcrumbsView.addItem(
                        BreadcrumbItem.createSimpleItem(
                            Item(
                                "",
                                vaultFile.id
                            )
                        )
                    )
                }
            }

            vaultFiles.observe(this@AttachmentsActivitySelector) { files ->
                if (files.isEmpty()) {
                    binding.attachmentsRecyclerView.visibility = View.GONE
                    binding.emptyViewMsgContainer.visibility = View.VISIBLE
                } else {
                    binding.attachmentsRecyclerView.visibility = View.VISIBLE
                    binding.emptyViewMsgContainer.visibility = View.GONE
                }
                attachmentsAdapter.setFiles(files)
            }

            selectVaultFiles.observe(this@AttachmentsActivitySelector) { listFiles ->
                if (!listFiles.isNullOrEmpty()) {
                    attachmentsAdapter.selectedMediaFiles = listFiles
                    updateAttachmentsToolbar(attachmentsAdapter.selectedMediaFiles.size)
                }
            }
        }
    }

    override fun playMedia(vaultFile: VaultFile?) {
        if (vaultFile == null) {
            return
        }

        when (vaultFile.type) {
            VaultFile.Type.DIRECTORY -> {
                attachmentsAdapter.clearSelected()
                openDirectory(vaultFile)
            }
            VaultFile.Type.FILE -> {
                if (vaultFile.mimeType != null) {
                    when {
                        MediaFile.isImageFileType(vaultFile.mimeType) -> {
                            val intent = Intent(this, PhotoViewerActivity::class.java)
                            intent.putExtra(PhotoViewerActivity.VIEW_PHOTO, vaultFile)
                            startActivity(intent)
                        }
                        MediaFile.isAudioFileType(vaultFile.mimeType) -> {
                            val intent = Intent(this, AudioPlayActivity::class.java)
                            intent.putExtra(AudioPlayActivity.PLAY_MEDIA_FILE_ID_KEY, vaultFile.id)
                            startActivity(intent)
                        }
                        MediaFile.isVideoFileType(vaultFile.mimeType) -> {
                            val intent = Intent(this, VideoViewerActivity::class.java)
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
        updateAttachmentsToolbar(num)
    }

    override fun onMediaSelected(vaultFile: VaultFile?) {
        vaultFile?.let {
            updateAttachmentsToolbar(attachmentsAdapter.selectedMediaFiles.size)
        }
    }

    private fun updateAttachmentsToolbar(itemsSize: Int) {
        if (itemsSize == 0) {
            binding.toolbar.setStartTextTitle(getString(R.string.Vault_Select_Title))
            binding.toolbar.setRightIcon(-1)
        } else {
            binding.toolbar.setStartTextTitle(
                attachmentsAdapter.selectedMediaFiles.size.toString() + " " + getString(R.string.Vault_Items)
            )
            binding.toolbar.setRightIcon(R.drawable.ic_check_white)
            binding.toolbar.rightIconContentDescription = R.string.action_check
        }
    }

    override fun onMediaDeselected(vaultFile: VaultFile?) {
        vaultFile?.let {
            updateAttachmentsToolbar(attachmentsAdapter.selectedMediaFiles.size)
        }
    }

    override fun openFolder(vaultFile: VaultFile?) {
        vaultFile?.let { openDirectory(it) }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.gridCheck -> {
                binding.gridCheck.toggleVisibility(false)
                binding.listCheck.toggleVisibility(true)
                gridLayoutManager.spanCount = 4
                attachmentsAdapter.setLayoutManager(gridLayoutManager)
                attachmentsAdapter.notifyItemRangeChanged(0, attachmentsAdapter.itemCount)
                binding.attachmentsRecyclerView.setMargins(leftMarginDp = 13, rightMarginDp = 13)
            }
            R.id.listCheck -> {
                binding.gridCheck.toggleVisibility(true)
                binding.listCheck.toggleVisibility(false)
                gridLayoutManager.spanCount = 1
                attachmentsAdapter.setLayoutManager(gridLayoutManager)
                attachmentsAdapter.notifyItemRangeChanged(0, attachmentsAdapter.itemCount)
                binding.attachmentsRecyclerView.setMargins(leftMarginDp = 0, rightMarginDp = 0)
            }
        }
    }


    private fun openDirectory(vaultFile: VaultFile) {
        if (currentRootID != vaultFile.id) {
            currentRootID = vaultFile.id
            viewModel.getFiles(currentRootID, filterType, null)
            binding.breadcrumbsView.visibility = View.VISIBLE
            binding.breadcrumbsView.addItem(createItem(vaultFile))
        }
    }

    private fun createItem(file: VaultFile): BreadcrumbItem {
        val list: MutableList<Item> = ArrayList()
        list.add(Item(file.name, file.id))
        return BreadcrumbItem(list)
    }

    private fun setUpBreadCrumb() {
        binding.breadcrumbsView.setCallback(object : DefaultBreadcrumbsCallback<BreadcrumbItem?>() {
            override fun onNavigateBack(item: BreadcrumbItem?, position: Int) {
                if (position == 0) {
                    binding.breadcrumbsView.visibility = View.GONE
                }
                currentRootID = item?.items?.get(item.selectedIndex)?.id
                viewModel.getFiles(currentRootID, filterType, null)
            }

            override fun onNavigateNewLocation(newItem: BreadcrumbItem?, changedPosition: Int) {
                showToast(changedPosition.toString())
                currentRootID = newItem?.items?.get(newItem.selectedIndex)?.id
                viewModel.getFiles(currentRootID, filterType, null)
            }
        })
    }

    private fun setResultAndFinish() {
        if (isOdkSelect) {
            setResult(
                RESULT_OK,
                Intent().putExtra(
                    CameraActivity.MEDIA_FILE_KEY,
                    attachmentsAdapter.selectedMediaFiles[0]
                )
            )
        } else {
            // to check wafa  return list files to entry
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(
                    VAULT_FILE_KEY,
                    Gson().toJson(attachmentsAdapter.selectedMediaFiles.map { it.id })
                )
            )
        }
        finish()
    }


    private fun getSelectedMediaFromIntent() {
        if (!intent.hasExtra(VAULT_FILE_KEY)) {
            return
        }
        intent.getStringExtra(VAULT_FILE_KEY)?.let { files ->
            val type = object : TypeToken<Array<String>>() {}.type
            val listFiles = Gson().fromJson(files, type) as Array<String>? ?: emptyArray()

            if (!listFiles.isNullOrEmpty()) {
                viewModel.getFiles(listFiles)
            }
        }
    }

    private fun handleBackStack() {
        when {
            binding.breadcrumbsView.items.size > 1 -> {
                if (binding.breadcrumbsView.items.size == 2) {
                    binding.breadcrumbsView.visibility = View.GONE
                }
                binding.breadcrumbsView.removeLastItem()
                currentRootID =
                    binding.breadcrumbsView.getCurrentItem<BreadcrumbItem>().selectedItem.id
                viewModel.getFiles(currentRootID, filterType, null)
            }
            else -> {
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            return;
        }
    }

}