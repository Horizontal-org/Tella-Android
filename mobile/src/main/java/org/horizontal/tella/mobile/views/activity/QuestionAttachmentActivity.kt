package org.horizontal.tella.mobile.views.activity

import android.Manifest
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hzontal.tella_vault.VaultFile
import com.hzontal.utils.MediaFile.isAudioFileType
import com.hzontal.utils.MediaFile.isImageFileType
import com.hzontal.utils.MediaFile.isPDFFile
import com.hzontal.utils.MediaFile.isVideoFileType
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.ActivityQuestionAttachmentBinding
import org.horizontal.tella.mobile.domain.repository.IMediaFileRecordRepository
import org.horizontal.tella.mobile.util.C
import org.horizontal.tella.mobile.util.DialogsUtil
import org.horizontal.tella.mobile.views.activity.camera.CameraActivity
import org.horizontal.tella.mobile.views.activity.viewer.AudioPlayActivity
import org.horizontal.tella.mobile.views.activity.viewer.PDFReaderActivity
import org.horizontal.tella.mobile.views.activity.viewer.PhotoViewerActivity
import org.horizontal.tella.mobile.views.activity.viewer.VideoViewerActivity
import org.horizontal.tella.mobile.views.adapters.GalleryRecycleViewAdapter
import org.horizontal.tella.mobile.views.custom.GalleryRecyclerView
import org.horizontal.tella.mobile.views.fragment.forms.QuestionAttachmentModel
import org.horizontal.tella.mobile.views.interfaces.IAttachmentsMediaHandler
import org.horizontal.tella.mobile.views.interfaces.IGalleryMediaHandler
import org.hzontal.shared_ui.utils.DialogUtils
import timber.log.Timber
import java.io.FileNotFoundException

class QuestionAttachmentActivity : MetadataActivity(), IAttachmentsMediaHandler,
    IGalleryMediaHandler {

    private var recyclerView: GalleryRecyclerView? = null
    var progressBar: ProgressBar? = null
    var toolbar: Toolbar? = null
    private var blankGalleryInfo: TextView? = null
    private var selectedNum = 0
    private var galleryAdapter: GalleryRecycleViewAdapter? = null
    private var progressDialog: ProgressDialog? = null
    private var filter: IMediaFileRecordRepository.Filter? = null
    private var sort = IMediaFileRecordRepository.Sort.NEWEST
    private lateinit var binding: ActivityQuestionAttachmentBinding
    private val attachmentModel: QuestionAttachmentModel by viewModels()


    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val requestCameraAndAudio =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val allGranted = requiredPermissions.all { perm -> result[perm] == true }
            when {
                allGranted -> {
                    Timber.d("All permissions granted -> start capture")
                    startCameraCaptureActivity()
                }
                // "Never ask again" if any denied AND shouldShowRequestPermissionRationale == false
                requiredPermissions.any { perm ->
                    result[perm] == false && !shouldShowRequestPermissionRationale(perm)
                } -> {
                    Timber.w("Permission permanently denied")
                    onCameraAndAudioNeverAskAgain()
                }

                else -> {
                    Timber.w("Permission denied (rationale can be shown)")
                    onCameraAndAudioPermissionDenied()
                }
            }
        }

    private fun hasAllRequiredPermissions(): Boolean =
        requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }

    /** Keep the same entrypoint name that your code likely calls already. */
    fun startCameraCaptureActivityWithPermissionCheck() {
        if (hasAllRequiredPermissions()) {
            startCameraCaptureActivity()
        } else {
            // Optional: show short rationale first if desired
            val showRationale = requiredPermissions.any { shouldShowRequestPermissionRationale(it) }
            if (showRationale) {
                DialogUtils.showBottomMessage(
                    this,
                    getString(R.string.permission_dialog_expl_camera_mic),
                    false
                )
            }
            requestCameraAndAudio.launch(requiredPermissions)
        }
    }

    // Called when user denied but not permanently.
    fun onCameraAndAudioPermissionDenied() {
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.permission_required),
            true
        )
    }

    // Called when user selected "Don't ask again".
    fun onCameraAndAudioNeverAskAgain() {
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.permission_dialog_expl_camera_mic),
            true
        )
        openAppSettings()
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "Failed to open app settings")
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestionAttachmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdge(binding.root)
        initView()

        binding.content.popupMenu.setOnClickListener { view: View? -> showPopupSort(view) }

        toolbar!!.setNavigationIcon(R.drawable.ic_close_white_24dp)

        if (intent.hasExtra(MEDIA_FILES_FILTER)) {
            filter =
                intent.getSerializableExtra(MEDIA_FILES_FILTER) as IMediaFileRecordRepository.Filter?
        } else {
            throw IllegalArgumentException()
        }

        setupToolbar()
        setupFab()

        galleryAdapter = GalleryRecycleViewAdapter(
            this,
            true, true, this
        )
        val galleryLayoutManager: RecyclerView.LayoutManager = GridLayoutManager(this, 3)
        recyclerView!!.layoutManager = galleryLayoutManager
        recyclerView!!.adapter = galleryAdapter

        selectedMediaFromIntent
        initObservers()
        attachmentModel.getFiles(filter, sort)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (selectedNum > 0) {
            menuInflater.inflate(R.menu.attachments_menu, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        if (selectedNum > 0) {
            if (id == R.id.menu_item_select) {
                setResultAndFinish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initObservers() {
        with(attachmentModel) {
            onGetFilesStart.observe(this@QuestionAttachmentActivity) {
                onGetFilesStart()
            }
            onGetFilesEnd.observe(this@QuestionAttachmentActivity) {
                onGetFilesEnd()
            }
            onGetFilesSuccess.observe(this@QuestionAttachmentActivity) { vaultFiles: List<VaultFile> ->
                onGetFilesSuccess(vaultFiles)
            }
            onGetFilesError.observe(this@QuestionAttachmentActivity) { throwable: Throwable? ->
                throwable?.let { onGetFilesError(throwable) }
            }
            onMediaFileAdded.observe(this@QuestionAttachmentActivity) { vaultFile: VaultFile? ->
                vaultFile?.let { onMediaFileAdded(vaultFile) }
            }
            onImportStarted.observe(this@QuestionAttachmentActivity) { onImportStarted() }
            onImportEnded.observe(this@QuestionAttachmentActivity) { onImportEnded() }
            onMediaFileImported.observe(this@QuestionAttachmentActivity) { vaultFile: VaultFile? ->
                vaultFile?.let { onMediaFileImported(vaultFile) }
            }
            onImportError.observe(this@QuestionAttachmentActivity) { throwable: Throwable? ->
                throwable?.let { onImportError(throwable) }
            }
            duplicateNameError.observe(this@QuestionAttachmentActivity, ::onRenameConflictError)
        }
    }

    private fun onRenameConflictError(isConflict: Boolean) {
        if (isConflict) {
            DialogUtils.showBottomMessage(
                this, getString(R.string.file_name_taken), true
            )
        }
    }

    fun showPopupSort(view: View?) {
        val wrapper: Context = ContextThemeWrapper(this, R.style.GalerySortTextColor)
        val popup = PopupMenu(wrapper, view)
        popup.inflate(R.menu.question_attachment_sort_menu)
        popup.show()
        setCheckedSort(sort, popup)
        popup.setOnMenuItemClickListener { item: MenuItem ->
            item.isChecked = true
            if (item.groupId == R.id.sort) {
                sort = getGallerySort(item.itemId)
            }
            attachmentModel.getFiles(filter, sort)
            true
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.collect_form_select_attachment_app_bar)
        }
    }

    private fun setupFab() {
        // If you had a capture FAB, call:
        // fab.setOnClickListener { startCameraCaptureActivityWithPermissionCheck() }
    }

    // PermissionsDispatcher annotations removed; keep your actual action here.
    fun startCameraCaptureActivity() {
        // TODO: launch your camera capture flow
        // Example:
        val intent = Intent(this, CameraActivity::class.java)
        intent.putExtra(CameraActivity.CAMERA_MODE, CameraActivity.CameraMode.PHOTO.name)
        startActivityForResult(intent, C.CAMERA_CAPTURE)
    }

    override fun playMedia(vaultFile: VaultFile) {
        if (isImageFileType(vaultFile.mimeType)) {
            val intent = Intent(this, PhotoViewerActivity::class.java)
            intent.putExtra(PhotoViewerActivity.VIEW_PHOTO, vaultFile)
            intent.putExtra(PhotoViewerActivity.NO_ACTIONS, true)
            startActivity(intent)
        } else if (isAudioFileType(vaultFile.mimeType)) {
            val intent = Intent(this, AudioPlayActivity::class.java)
            intent.putExtra(AudioPlayActivity.PLAY_MEDIA_FILE_ID_KEY, vaultFile.id)
            intent.putExtra(AudioPlayActivity.NO_ACTIONS, true)
            startActivity(intent)
        } else if (isVideoFileType(vaultFile.mimeType)) {
            val intent = Intent(this, VideoViewerActivity::class.java)
            intent.putExtra(VideoViewerActivity.VIEW_VIDEO, vaultFile)
            intent.putExtra(VideoViewerActivity.NO_ACTIONS, true)
            startActivity(intent)
        } else if (isPDFFile(vaultFile.name, vaultFile.mimeType)) {
            val intent = Intent(this, PDFReaderActivity::class.java)
            intent.putExtra(PDFReaderActivity.VIEW_PDF, vaultFile)
            startActivity(intent)
        }
    }

    override fun onRemoveAttachment(vaultFile: VaultFile) {
        galleryAdapter!!.deselectMediaFile(vaultFile)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        when (requestCode) {
            C.IMPORT_IMAGE -> {
                val image = data!!.data
                if (image != null) {
                    attachmentModel.importImage(image)
                }
            }

            C.IMPORT_VIDEO -> {
                val video = data!!.data
                if (video != null) {
                    attachmentModel.importVideo(video)
                }
            }

            C.CAMERA_CAPTURE, C.RECORDED_AUDIO -> { /* handled elsewhere */
            }
        }
    }

    override fun onSelectionNumChange(num: Int) {
        val current = selectedNum > 0
        val next = num > 0
        selectedNum = num
        if (current != next) {
            invalidateOptionsMenu()
        }
    }

    override fun onMediaSelected(vaultFile: VaultFile) {
        attachmentModel.setAttachment(vaultFile)
    }

    override fun onMediaDeselected(vaultFile: VaultFile) {
        attachmentModel.setAttachment(null) // only one
    }

    private fun onGetFilesSuccess(files: List<VaultFile>) {
        blankGalleryInfo!!.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
        galleryAdapter!!.setFiles(files)
    }

    private fun onGetFilesStart() {}

    private fun onGetFilesEnd() {}

    private fun onGetFilesError(error: Throwable?) {
        Timber.d(error)
    }

    private fun onMediaFileAdded(vaultFile: VaultFile) {
        attachmentModel.getFiles(filter, sort)
    }

    private fun onMediaFileAddError(error: Throwable) {
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.collect_toast_fail_attaching_file_to_form),
            true
        )
        Timber.d(error, javaClass.name)
    }

    private fun onMediaFileImported(vaultFile: VaultFile) {
        attachmentModel.addNewMediaFile(vaultFile)
    }

    private fun onImportError(error: Throwable) {
        val messageResId = when (error) {
            is FileNotFoundException -> R.string.error_file_not_found
            else -> R.string.gallery_toast_fail_importing_file
        }
        DialogUtils.showBottomMessage(this, getString(messageResId), true)
        Timber.d(error, javaClass.name)
    }

    private fun onImportStarted() {
        progressDialog =
            DialogsUtil.showProgressDialog(this, getString(R.string.gallery_dialog_expl_encrypting))
    }

    private fun onImportEnded() {
        hideProgressDialog()
        DialogUtils.showBottomMessage(this, getString(R.string.gallery_toast_file_encrypted), false)
    }

    private fun getContext(): Context = this

    private val selectedMediaFromIntent: Unit
        get() {
            if (!intent.hasExtra(MEDIA_FILE_KEY)) return
            val vaultFile = intent.getSerializableExtra(MEDIA_FILE_KEY) as VaultFile?
            if (vaultFile != null) {
                attachmentModel.setAttachment(vaultFile)
                galleryAdapter!!.selectMediaFile(vaultFile)
                onSelectionNumChange(1)
            }
        }

    private fun clearSelection() {
        galleryAdapter!!.clearSelected()
    }

    private fun hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

    private fun setCheckedSort(checkedSort: IMediaFileRecordRepository.Sort, popup: PopupMenu) {
        popup.menu.findItem(getSortId(checkedSort))?.isChecked = true
    }

    fun getGallerySort(id: Int): IMediaFileRecordRepository.Sort {
        return when (id) {
            R.id.oldest -> IMediaFileRecordRepository.Sort.OLDEST
            else -> IMediaFileRecordRepository.Sort.NEWEST
        }
    }

    @IdRes
    fun getSortId(sort: IMediaFileRecordRepository.Sort?): Int {
        return when (sort) {
            IMediaFileRecordRepository.Sort.OLDEST -> R.id.oldest
            else -> R.id.newest
        }
    }

    private fun setResultAndFinish() {
        setResult(RESULT_OK, Intent().putExtra(MEDIA_FILE_KEY, attachmentModel.getAttachment()))
        finish()
    }

    private fun initView() {
        recyclerView = binding.content.galleryRecyclerView
        progressBar = binding.content.progressBar
        toolbar = binding.toolbar
        blankGalleryInfo = binding.content.attachmentsBlankListInfo
    }

    companion object {
        const val MEDIA_FILE_KEY = "mfk"
        const val MEDIA_FILES_FILTER = "mff"
    }
}
