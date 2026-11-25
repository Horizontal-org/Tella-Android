package org.horizontal.tella.mobile.views.activity.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.SensorManager
import android.media.MediaActionSound
import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.DisplayMetrics
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.gson.Gson
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.utils.MediaFile
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.event.CaptureEvent
import org.horizontal.tella.mobile.data.sharedpref.Preferences
import org.horizontal.tella.mobile.databinding.ActivityCameraBinding
import org.horizontal.tella.mobile.media.MediaFileHandler
import org.horizontal.tella.mobile.mvp.contract.IMetadataAttachPresenterContract
import org.horizontal.tella.mobile.mvp.presenter.MetadataAttacher
import org.horizontal.tella.mobile.mvvm.viewmodel.TellaFileUploadSchedulerViewModel
import org.horizontal.tella.mobile.util.C
import org.horizontal.tella.mobile.util.CameraDialogsUtil
import org.horizontal.tella.mobile.util.VideoResolutionManager
import org.horizontal.tella.mobile.util.crash.CrashReporterProvider
import org.horizontal.tella.mobile.util.getDuplicateErrorMessageResId
import org.horizontal.tella.mobile.util.isDuplicateNameOrFileExistsError
import org.horizontal.tella.mobile.views.activity.MainActivity
import org.horizontal.tella.mobile.views.activity.MetadataActivity
import org.horizontal.tella.mobile.views.activity.viewer.PhotoViewerActivity
import org.horizontal.tella.mobile.views.activity.viewer.VideoViewerActivity
import org.horizontal.tella.mobile.views.custom.CameraCaptureButton
import org.horizontal.tella.mobile.views.custom.CameraDurationTextView
import org.horizontal.tella.mobile.views.custom.CameraFlashButton
import org.horizontal.tella.mobile.views.custom.CameraGridButton
import org.horizontal.tella.mobile.views.custom.CameraResolutionButton
import org.horizontal.tella.mobile.views.custom.CameraSwitchButton
import org.horizontal.tella.mobile.views.fragment.uwazi.attachments.VAULT_FILE_KEY
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import timber.log.Timber
import java.io.File
import java.util.concurrent.ExecutionException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@AndroidEntryPoint
class CameraActivity : MetadataActivity(), IMetadataAttachPresenterContract.IView {
    private lateinit var gridButton: CameraGridButton
    private lateinit var switchButton: CameraSwitchButton
    private lateinit var flashButton: CameraFlashButton
    private lateinit var captureButton: CameraCaptureButton
    private lateinit var durationView: CameraDurationTextView
    private lateinit var previewView: ImageView
    private lateinit var photoModeText: TextView
    private lateinit var videoModeText: TextView
    private lateinit var resolutionButton: CameraResolutionButton
    private var metadataAttacher: MetadataAttacher? = null
    private var mode = CameraMode.PHOTO
    private var modeLocked = false
    private var intentMode: IntentMode? = null
    private var videoRecording = false
    private var progressDialog: ProgressDialog? = null
    private var mOrientationEventListener: OrientationEventListener? = null
    private var capturedMediaFile: VaultFile? = null
    private var lastMediaFile: VaultFile? = null
    private var videoQualityDialog: AlertDialog? = null
    private var videoResolutionManager: VideoResolutionManager? = null
    private var lastClickTime = System.currentTimeMillis()
    private var currentRootParent: String? = null
    private var tempFile: File? = null
    var recording: Recording? = null
    private var isRecording = false
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var hdrCameraSelector: CameraSelector? = null
    private var lensFacing = CameraSelector.DEFAULT_BACK_CAMERA
    private var isBackCamera = true
    private var flashMode = ImageCapture.FLASH_MODE_OFF
    private var flashSupportsAuto = true
    private var flashSupportsTorch = true
    private var gridEnabled = false
    private var photoAspectRatio = PhotoAspectRatio.THREE_FOUR
    private var shutterSound: MediaActionSound? = null
    private var cameraStopped = false
    private var pendingCaptureAfterLocationCheck = PendingCapture.NONE
    private var gpsPromptIgnoredForCurrentSession = false
    private val previewController by lazy {
        CameraPreviewController(
            lifecycleOwner = this,
            previewView = binding.viewFinder,
            focusIndicator = binding.focusIndicator,
            zoomSlider = binding.cameraZoom,
            cameraProvider = { camera },
            isCameraActive = { !cameraStopped },
        )
    }

    private lateinit var binding: ActivityCameraBinding
    private var captureWithAutoUpload = true
    private val viewModel by viewModels<SharedCameraViewModel>()
    private val uploadViewModel by viewModels<TellaFileUploadSchedulerViewModel>()
    private var isAddingInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        photoAspectRatio = PhotoAspectRatio.fromSavedValue(
            savedInstanceState?.getString(PHOTO_ASPECT_RATIO_STATE)
        )
        binding.viewFinder.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        ViewCompat.setOnApplyWindowInsetsListener(binding.cameraControlsContainer) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val mandatory = insets.getInsets(WindowInsetsCompat.Type.mandatorySystemGestures())

            var top = maxOf(statusBars.top, cutout.top, mandatory.top)
            if (top == 0) {
                // Status bar currently hidden: keep its normal height so the layout doesn't jump.
                top = insets.getInsetsIgnoringVisibility(
                    WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
                ).top
            }
            val bottom = maxOf(navBars.bottom, mandatory.bottom)
            val left = maxOf(statusBars.left, navBars.left, cutout.left, mandatory.left)
            val right = maxOf(statusBars.right, navBars.right, cutout.right, mandatory.right)

            view.setPadding(left, top, right, bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.cameraControlsContainer)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        initView()
        overridePendingTransition(R.anim.slide_in_up, R.anim.fade_out)
        metadataAttacher = MetadataAttacher(this)
        applyCaptureIntent(intent)
        setupCameraView()
        setupCameraModeButton()
        setupImagePreview()
        initObservers()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(PHOTO_ASPECT_RATIO_STATE, photoAspectRatio.name)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyCaptureIntent(intent)
    }

    private fun applyCaptureIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.hasExtra(CAMERA_MODE)) {
            mode = CameraMode.valueOf(intent.getStringExtra(CAMERA_MODE)!!)
            modeLocked = true
        }
        intentMode = IntentMode.RETURN
        if (intent.hasExtra(INTENT_MODE)) {
            intentMode = IntentMode.valueOf(intent.getStringExtra(INTENT_MODE)!!)
        }
        if (intent.hasExtra(VAULT_CURRENT_ROOT_PARENT)) {
            currentRootParent = intent.getStringExtra(VAULT_CURRENT_ROOT_PARENT)
        }
        val capturingAsAttachment =
            intentMode == IntentMode.COLLECT || intentMode == IntentMode.ODK
        captureWithAutoUpload = if (intent.hasExtra(CAPTURE_WITH_AUTO_UPLOAD)) {
            intent.getBooleanExtra(CAPTURE_WITH_AUTO_UPLOAD, false) && !capturingAsAttachment
        } else {
            !capturingAsAttachment
        }
    }

    private fun initObservers() {
        viewModel.addError.observe(this) { throwable ->
            onAddError(throwable)
        }

        viewModel.duplicateNameError.observe(this) { isConflict ->
            if (isConflict == true) {
                DialogUtils.showBottomMessage(this, getString(R.string.file_name_taken), true)
            }
        }

        viewModel.addSuccess.observe(this) { vaultFile ->
            onAddSuccess(vaultFile)
        }

        viewModel.addingInProgress.observe(this) { isAdding ->
            isAddingInProgress = isAdding
        }

        viewModel.lastMediaFileSuccess.observe(this) { mediaFile ->
            onLastMediaFileSuccess(mediaFile)
        }

        viewModel.lastMediaFileError.observe(this) { throwable ->
            onLastMediaFileError(throwable)
        }

        viewModel.rotationUpdate.observe(this) { rotation ->
            rotateViews(rotation)
        }

        uploadViewModel.mediaFilesUploadScheduled.observe(this) {
            onMediaFilesUploadScheduled()
        }

        uploadViewModel.mediaFilesUploadScheduleError.observe(this) {
            onMediaFilesUploadScheduleError()
        }
    }

    override fun onStart() {
        super.onStart()
        startLocationMetadataListening()
    }

    override fun onResume() {
        super.onResume()
        mOrientationEventListener?.enable()
        previewController.syncZoomSlider()
        viewModel.getLastMediaFile()
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED &&
            (cameraStopped || cameraProvider == null)
        ) {
            resumeCameraPreview()
        }
    }

    override fun onPause() {
        previewController.resetFocus()
        super.onPause()
        mOrientationEventListener?.disable()
    }

    override fun onStop() {
        if (videoRecording) {
            captureButton.performClick()
        }
        cameraProvider?.unbindAll()
        camera = null
        cameraStopped = true
        stopLocationMetadataListening()
        super.onStop()
    }

    override fun onDestroy() {
        previewController.resetFocus()
        super.onDestroy()
        hideProgressDialog()
        hideVideoResolutionDialog()
        shutterSound?.release()
        shutterSound = null
        cameraProvider?.unbindAll()
    }

    override fun resumePendingLocationSettingsCheck() {
        if (cameraStopped) {
            return
        }
        super.resumePendingLocationSettingsCheck()
    }

    private fun resumeCameraPreview() {
        if (mode == CameraMode.VIDEO) {
            startVideo()
        } else {
            startCamera()
        }
        previewController.applyZoom()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleBackPressed()
        }
    }

    private fun handleBackPressed() {
        if (hideCameraOptions()) return
        if (maybeStopVideoRecording()) return
        if (!Preferences.isAnonymousMode() && isAddingInProgress) {
            BottomSheetUtils.showConfirmSheet(
                fragmentManager = supportFragmentManager,
                getString(R.string.exit_and_discard_verification_info),
                getString(R.string.recording_in_progress_exit_warning),
                getString(R.string.exit_and_discard_info),
                getString(R.string.back),
                consumer = object : BottomSheetUtils.ActionConfirmed {
                    override fun accept(isConfirmed: Boolean) {
                        if (isConfirmed) {
                            finish()
                        }
                    }
                })
            return
        }

        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up)
    }

    private fun onAddSuccess(file: VaultFile) {
        capturedMediaFile = file
        if (intentMode != IntentMode.COLLECT) {
            lastMediaFile = file
            previewView.visibility = View.VISIBLE
            Glide.with(this).load(file.thumb).into(previewView)
        }
        if (!Preferences.isAnonymousMode()) {
            attachMediaFileMetadata(capturedMediaFile, metadataAttacher)
        } else {
            returnIntent(file)
        }
        if (shouldScheduleAutoUpload()) {
            capturedMediaFile?.let { vaultFile -> scheduleFileUpload(vaultFile) }
        }
        MyApplication.bus().post(CaptureEvent())
    }

    private fun onAddError(error: Throwable) {
        val messageResId = when {
            error.isDuplicateNameOrFileExistsError() -> error.getDuplicateErrorMessageResId()
            else -> R.string.gallery_toast_fail_saving_file
        }
        DialogUtils.showBottomMessage(this, getString(messageResId), true)
    }

    override fun onMetadataAttached(vaultFile: VaultFile) {
        returnIntent(vaultFile)
    }

    private fun returnIntent(vaultFile: VaultFile) {
        capturedMediaFile?.metadata = vaultFile.metadata

        Intent().apply {
            when (intentMode) {
                IntentMode.ODK -> {
                    capturedMediaFile!!.metadata = vaultFile.metadata
                    putExtra(MEDIA_FILE_KEY, capturedMediaFile)
                    setResult(RESULT_OK, this)
                    finish()
                }

                IntentMode.COLLECT -> {
                    val list: MutableList<String> = mutableListOf()
                    list.add(vaultFile.id)
                    putExtra(VAULT_FILE_KEY, Gson().toJson(list))
                    setResult(RESULT_OK, this)
                    finish()
                }

                else -> {
                    putExtra(C.CAPTURED_MEDIA_FILE_ID, vaultFile.metadata)
                    setResult(RESULT_OK, this)
                }
            }
        }
    }

    override fun onMetadataAttachError(throwable: Throwable) {
        onAddError(throwable)
    }

    private fun closeCamera() {
        onBackPressedDispatcher.onBackPressed()
    }

    private fun rotateViews(rotation: Int) {
        gridButton.rotateView(rotation)
        switchButton.rotateView(rotation)
        flashButton.rotateView(rotation)
        durationView.rotateView(rotation)
        captureButton.rotateView(rotation)
        binding.menuGridButton.rotateView(rotation)
        binding.menuFlashButton.rotateView(rotation)
        listOf(
            binding.flashPanelTitle,
            binding.flashBackButton
        ).forEach { it.animate().rotation(rotation.toFloat()).start() }
        if (mode != CameraMode.PHOTO) {
            resolutionButton.rotateView(rotation)
        }
        if (intentMode != IntentMode.COLLECT) {
            previewView.animate().rotation(rotation.toFloat()).start()
        }
    }

    private fun onLastMediaFileSuccess(vaultFile: VaultFile) {
        lastMediaFile = vaultFile
        if (intentMode != IntentMode.COLLECT) {
            previewView.visibility = View.VISIBLE
            Glide.with(this).load(vaultFile.thumb).diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true).into(previewView)
        }
    }

    private fun onLastMediaFileError(throwable: Throwable) {
        if (intentMode != IntentMode.COLLECT || intentMode == IntentMode.ODK) {
            previewView.visibility = View.GONE
        }
    }

    override fun getContext(): Context {
        return this
    }

    override fun getGpsMetadataDialogMessageResId(): Int {
        return R.string.verification_prompt_dialog_media_file_expl
    }

    override fun onGpsMetadataDialogConfirmed() {
        gpsPromptIgnoredForCurrentSession = false
    }

    override fun onGpsMetadataDialogIgnored() {
        gpsPromptIgnoredForCurrentSession = true
    }

    private fun onMediaFilesUploadScheduled() {
        val isAutoUploadEnabled = Preferences.isAutoUploadEnabled()
        val isAutoDeleteEnabled = Preferences.isAutoDeleteEnabled()

        val message = if (isAutoUploadEnabled && isAutoDeleteEnabled) {
            getString(R.string.Auto_Upload_Media_Imported_Report_And_Deleted)
        } else if (isAutoUploadEnabled) {
            getString(R.string.Auto_Upload_Media_Report)
        } else {
            return
        }

        DialogUtils.showBottomMessage(this, message, false)
    }

    private fun onMediaFilesUploadScheduleError() {}

    private fun onPhotoClicked() {
        if (modeLocked || videoRecording) return
        if (mode == CameraMode.PHOTO) return

        if (mode == CameraMode.VIDEO) {
            turnFlashDown()
        }
        mode = CameraMode.PHOTO
        setPhotoActive()
        captureButton.displayPhotoButton()
        captureButton.contentDescription = context.getString(R.string.Uwazi_WidgetMedia_Take_Photo)
        startCamera()
        previewController.resetZoom()
    }

    private fun onVideoClicked() {
        if (modeLocked || videoRecording) return
        if (mode == CameraMode.VIDEO) return

        mode = CameraMode.VIDEO
        startVideo()
        turnFlashDown()
        captureButton.displayVideoButton()
        captureButton.contentDescription = context.getString(R.string.Uwazi_WidgetMedia_Take_Video)
        setVideoActive()
        previewController.resetZoom()
    }

    private fun onGridClicked() {
        hideCameraOptions()
        gridEnabled = !gridEnabled
        updateGridUi()
    }

    private fun onSwitchClicked() {
        hideCameraOptions()
        isBackCamera = !isBackCamera
        lensFacing = if (isBackCamera) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        switchButton.displayCamera(isBackCamera)
        switchButton.contentDescription = if (isBackCamera) {
            getString(R.string.action_switch_to_front_camera)
        } else {
            getString(R.string.action_switch_to_back_camera)
        }
        if (mode == CameraMode.PHOTO) {
            startCamera()
        } else {
            startVideo()
        }
    }

    private fun onPreviewClicked() {
        val intent: Intent? = createIntentForMediaFile()
        intent?.let { startActivity(it) }
    }

    private fun createIntentForMediaFile(): Intent? {
        var intent: Intent? = null
        lastMediaFile?.mimeType?.let {
            when {
                MediaFile.isImageFileType(it) -> {
                    intent = Intent(this, PhotoViewerActivity::class.java).apply {
                        putExtra(PhotoViewerActivity.VIEW_PHOTO, lastMediaFile)
                    }
                }

                MediaFile.isVideoFileType(it) -> {
                    intent = Intent(this, VideoViewerActivity::class.java).apply {
                        putExtra(VideoViewerActivity.VIEW_VIDEO, lastMediaFile)
                    }
                }

                else -> {
                    intent = Intent(this, MainActivity::class.java).apply {
                        putExtra(MainActivity.PHOTO_VIDEO_FILTER, FilterType.PHOTO_VIDEO.name)
                    }
                }
            }
        }
        return intent
    }

    private fun maybeStopVideoRecording(): Boolean {
        if (videoRecording) {
            captureButton.performClick()
            return true
        }
        return false
    }

    private fun showConfirmVideoView(video: File) {
        displayVideoCaptureButton()
        durationView.stop()
        viewModel.addMp4Video(video, currentRootParent)
    }

    private fun displayVideoCaptureButton() {
        captureButton.displayVideoButton()
        captureButton.contentDescription = context.getString(R.string.Uwazi_WidgetMedia_Take_Video)
    }

    private fun setupCameraView() {
        if (mode == CameraMode.PHOTO) {
            captureButton.displayPhotoButton()
        } else {
            captureButton.displayVideoButton()
        }
        setupCameraGridButton()
        previewController.attach()
        setOrientationListener()
        if (hasCameraPermissions(this)) {
            resumeCameraPreview()
        }
    }

    private fun displayPhotoCaptureButton() {
        captureButton.displayPhotoButton()
        captureButton.contentDescription = context.getString(R.string.Uwazi_WidgetMedia_Take_Photo)
    }

    private fun setupCameraModeButton() {
        if (mode == CameraMode.PHOTO) {
            setPhotoActive()
        } else {
            setVideoActive()
        }
    }

    private fun setupImagePreview() {
        if (intentMode == IntentMode.COLLECT || intentMode == IntentMode.ODK) {
            previewView.visibility = View.GONE
        }
    }

    private fun setupCameraGridButton() {
        updateGridUi()
    }

    private fun updateGridUi() {
        updateGridOverlay()
        binding.menuGridButton.isSelected = gridEnabled
        val description = getString(
            if (gridEnabled) R.string.action_hide_gridview else R.string.action_show_gridview
        )
        if (gridEnabled) {
            gridButton.displayGridOn()
            binding.menuGridButton.displayGridOn()
        } else {
            gridButton.displayGridOff()
            binding.menuGridButton.displayGridOff()
        }
        gridButton.contentDescription = description
        binding.menuGridButton.contentDescription = description
    }

    private fun updateGridOverlay() {
        binding.gridOverlay.visibility = if (gridEnabled) View.VISIBLE else View.GONE
    }

    private fun updateCameraControlVisibility(localCameraProvider: ProcessCameraProvider) {
        val hasFrontCamera = localCameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
        switchButton.visibility = if (hasFrontCamera) View.VISIBLE else View.GONE
        updateFlashCapabilities(localCameraProvider)
        val showFlash = deviceHasFlash(localCameraProvider) &&
                (mode != CameraMode.VIDEO || flashSupportsTorch)
        if (showFlash) {
            flashButton.visibility = View.VISIBLE
            updateFlashButtonIcon()
        } else {
            flashButton.visibility = View.INVISIBLE
        }
        binding.menuFlashButton.isEnabled = showFlash
    }

    private fun updateFlashCapabilities(localCameraProvider: ProcessCameraProvider) {
        val hasFlash = deviceHasFlash(localCameraProvider)
        flashSupportsAuto = hasFlash && isBackCamera
        flashSupportsTorch = hasFlash
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun deviceHasFlash(localCameraProvider: ProcessCameraProvider): Boolean {
        camera?.cameraInfo?.let { info ->
            if (info.hasFlashUnit()) return true
        }
        val lensFacingInt = if (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraCharacteristics.LENS_FACING_FRONT
        } else {
            CameraCharacteristics.LENS_FACING_BACK
        }
        return localCameraProvider.availableCameraInfos.any { info ->
            Camera2CameraInfo.from(info)
                .getCameraCharacteristic(CameraCharacteristics.LENS_FACING) == lensFacingInt &&
                    Camera2CameraInfo.from(info)
                        .getCameraCharacteristic(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }

    private fun updateFlashButtonIcon() {
        binding.menuFlashButton.isSelected = flashMode != ImageCapture.FLASH_MODE_OFF
        when (flashMode) {
            ImageCapture.FLASH_MODE_AUTO -> {
                flashButton.displayFlashAuto()
                binding.menuFlashButton.displayFlashAuto()
                flashButton.contentDescription = getString(R.string.action_disable_flash)
                binding.menuFlashButton.contentDescription = getString(R.string.camera_flash_auto_description)
            }

            ImageCapture.FLASH_MODE_ON -> {
                flashButton.displayFlashOn()
                binding.menuFlashButton.displayFlashOn()
                flashButton.contentDescription = getString(R.string.action_disable_flash)
                binding.menuFlashButton.contentDescription = getString(R.string.camera_flash_on_description)
            }

            else -> {
                flashButton.displayFlashOff()
                binding.menuFlashButton.displayFlashOff()
                flashButton.contentDescription = getString(R.string.action_enable_flash)
                binding.menuFlashButton.contentDescription = getString(R.string.camera_flash_off_description)
            }
        }
        updateFlashOptionsSelection()
    }

    private fun turnFlashDown() {
        flashMode = ImageCapture.FLASH_MODE_OFF
        imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
        camera?.cameraControl?.enableTorch(false)
        updateFlashButtonIcon()
    }

    private fun hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

    private fun setOrientationListener() {
        mOrientationEventListener =
            object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
                override fun onOrientationChanged(orientation: Int) {
                    if (orientation != ORIENTATION_UNKNOWN) {
                        viewModel.handleRotation(orientation)
                    }
                }
            }
    }

    private fun setPhotoActive() {
        hideCameraOptions()
        binding.photoMode.isSelected = true
        binding.videoMode.isSelected = false
        photoModeText.alpha = 1f
        videoModeText.alpha = if (modeLocked) 0.1f else 1f
        resolutionButton.visibility = View.GONE
        binding.aspectRatioMenuItem.visibility = View.VISIBLE
        updatePhotoAspectRatioUi()
        updatePreviewBounds()
    }

    private fun setVideoActive() {
        hideCameraOptions()
        binding.videoMode.isSelected = true
        binding.photoMode.isSelected = false
        videoModeText.alpha = 1f
        photoModeText.alpha = if (modeLocked) 0.1f else 1f
        binding.aspectRatioMenuItem.visibility = View.GONE
        updatePreviewBounds()
        if (videoResolutionManager != null) {
            resolutionButton.visibility = View.VISIBLE
        }
    }

    private fun chooseVideoResolution() {
        videoResolutionManager?.let { manager ->
            videoQualityDialog = CameraDialogsUtil.showVideoResolutionDialog(
                this,
                { applyVideoQuality() },
                manager
            )
        }
    }

    private fun applyVideoQuality() {
        if (mode == CameraMode.VIDEO) {
            startVideo()
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun initVideoResolutionManager(localCameraProvider: ProcessCameraProvider) {
        val lensFacingInt = if (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraCharacteristics.LENS_FACING_FRONT
        } else {
            CameraCharacteristics.LENS_FACING_BACK
        }
        val cameraInfo: CameraInfo? = localCameraProvider.availableCameraInfos.find { info ->
            Camera2CameraInfo.from(info)
                .getCameraCharacteristic(CameraCharacteristics.LENS_FACING) == lensFacingInt
        }
        val supportedQualities = cameraInfo?.let { QualitySelector.getSupportedQualities(it) }
            ?: emptyList()
        videoResolutionManager = if (supportedQualities.isNotEmpty()) {
            VideoResolutionManager(supportedQualities)
        } else {
            null
        }
        if (mode == CameraMode.VIDEO && videoResolutionManager != null) {
            resolutionButton.visibility = View.VISIBLE
        }
    }

    private fun hideVideoResolutionDialog() {
        if (videoQualityDialog != null) {
            videoQualityDialog!!.dismiss()
            videoQualityDialog = null
        }
    }

    private fun shouldScheduleAutoUpload(): Boolean {
        if (intentMode == IntentMode.COLLECT || intentMode == IntentMode.ODK) {
            return false
        }
        return captureWithAutoUpload && Preferences.isAutoUploadEnabled()
    }

    private fun scheduleFileUpload(vaultFile: VaultFile) {
        if (!shouldScheduleAutoUpload()) return
        uploadViewModel.scheduleUploadReportFiles(
            vaultFile, Preferences.getAutoUploadServerId()
        )
    }

    private fun initView() {
        if (!hasCameraPermissions(context)) {
            requestCameraPermissions(C.CAMERA_PERMISSION)
        }

        gridButton = binding.gridButton
        switchButton = binding.switchButton
        switchButton.displayCamera(isBackCamera)
        flashButton = binding.flashButton
        updateFlashButtonIcon()
        captureButton = binding.captureButton
        durationView = binding.durationView
        previewView = binding.previewImage
        photoModeText = binding.photoText
        videoModeText = binding.videoText
        resolutionButton = binding.resolutionButton

        binding.close.setOnClickListener { closeCamera() }

        binding.captureButton.setOnClickListener {
            hideCameraOptions()
            if (mode == CameraMode.PHOTO) {
                maybeCapturePhoto()
            } else if (videoRecording) {
                if (System.currentTimeMillis() - lastClickTime >= CLICK_DELAY) {
                    playCameraSound(MediaActionSound.STOP_VIDEO_RECORDING)
                    recordVideo()
                    divviupUtils.runVideoTakenEvent()
                }
            } else {
                maybeStartVideoRecording()
            }
        }

        binding.photoMode.setOnClickListener { onPhotoClicked() }
        binding.videoMode.setOnClickListener { onVideoClicked() }
        binding.gridButton.setOnClickListener { onGridClicked() }
        binding.switchButton.setOnClickListener { onSwitchClicked() }
        binding.flashButton.setOnClickListener { showFlashPanel() }
        binding.previewImage.setOnClickListener { onPreviewClicked() }
        binding.resolutionButton.setOnClickListener { chooseVideoResolution() }
        setupCameraOptions()
    }

    private fun setupCameraOptions() {
        binding.moreButton.setOnClickListener {
            if (!hideCameraOptions()) showMorePanel()
        }
        binding.panelDismissArea.setOnClickListener { hideCameraOptions() }
        binding.menuGridButton.setOnClickListener { onGridClicked() }
        binding.menuFlashButton.setOnClickListener { showFlashPanel() }
        binding.menuAspectRatioValue.setOnClickListener { showAspectRatioPanel() }
        binding.flashBackButton.setOnClickListener { showMorePanel() }
        binding.aspectRatioBackButton.setOnClickListener { showMorePanel() }

        listOf(
            binding.flashOffOption to ImageCapture.FLASH_MODE_OFF,
            binding.flashOnOption to ImageCapture.FLASH_MODE_ON,
            binding.flashAutoOption to ImageCapture.FLASH_MODE_AUTO
        ).forEach { (view, flashMode) ->
            view.setOnClickListener { selectFlashMode(flashMode) }
        }
        listOf(
            binding.aspectRatioThreeFour,
            binding.aspectRatioNineSixteen,
            binding.aspectRatioOneOne,
            binding.aspectRatioFull
        ).forEachIndexed { index, view ->
            view.setOnClickListener { selectPhotoAspectRatio(PhotoAspectRatio.entries[index]) }
        }

        updatePhotoAspectRatioUi()
        updateFlashOptionsSelection()
    }

    private fun showMorePanel() {
        setCameraPanel(binding.morePanel)
    }

    private fun showFlashPanel() {
        if (!binding.menuFlashButton.isEnabled) return
        setCameraPanel(binding.flashPanel)
    }

    private fun showAspectRatioPanel() {
        if (mode != CameraMode.PHOTO) return
        updatePhotoAspectRatioUi()
        setCameraPanel(binding.aspectRatioPanel)
    }

    private fun hideCameraOptions(): Boolean {
        val wasVisible = binding.morePanel.isVisible || binding.flashPanel.isVisible ||
            binding.aspectRatioPanel.isVisible
        setCameraPanel(null)
        return wasVisible
    }

    private fun setCameraPanel(panel: View?) {
        binding.morePanel.isVisible = panel === binding.morePanel
        binding.flashPanel.isVisible = panel === binding.flashPanel
        binding.aspectRatioPanel.isVisible = panel === binding.aspectRatioPanel
        binding.panelDismissArea.isVisible = panel != null
        binding.cameraZoom.isVisible = panel == null
        binding.moreButton.isVisible = panel == null
        panel?.requestFocus()
    }

    private fun selectFlashMode(selectedMode: Int) {
        if (selectedMode == ImageCapture.FLASH_MODE_AUTO && !flashSupportsAuto) return
        flashMode = selectedMode
        if (mode == CameraMode.VIDEO) {
            camera?.cameraControl?.enableTorch(selectedMode == ImageCapture.FLASH_MODE_ON)
        } else {
            camera?.cameraControl?.enableTorch(false)
            imageCapture?.flashMode = selectedMode
        }
        updateFlashButtonIcon()
    }

    private fun updateFlashOptionsSelection() {
        binding.flashOffOption.isSelected = flashMode == ImageCapture.FLASH_MODE_OFF
        binding.flashOnOption.isSelected = flashMode == ImageCapture.FLASH_MODE_ON
        binding.flashAutoOption.isSelected = flashMode == ImageCapture.FLASH_MODE_AUTO
        binding.flashAutoOption.visibility =
            if (mode == CameraMode.PHOTO && flashSupportsAuto) View.VISIBLE else View.GONE
    }

    private fun selectPhotoAspectRatio(selectedRatio: PhotoAspectRatio) {
        if (photoAspectRatio == selectedRatio) return
        photoAspectRatio = selectedRatio
        applyPhotoCropAspectRatio()
        updatePhotoAspectRatioUi()
        updatePreviewBounds(animate = true)
        binding.aspectRatioPanel.announceForAccessibility(
            getString(R.string.camera_aspect_ratio_selected, getString(selectedRatio.titleRes))
        )
    }

    private fun updatePhotoAspectRatioUi() {
        val title = getString(photoAspectRatio.titleRes)
        binding.menuAspectRatioValue.text = title
        binding.menuAspectRatioValue.contentDescription =
            "${getString(R.string.camera_aspect_ratio_settings)}: $title"
        listOf(
            binding.aspectRatioThreeFour,
            binding.aspectRatioNineSixteen,
            binding.aspectRatioOneOne,
            binding.aspectRatioFull
        ).forEachIndexed { index, view ->
            val isSelected = photoAspectRatio == PhotoAspectRatio.entries[index]
            view.isSelected = isSelected
            view.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (isSelected) R.color.camera_design_accent else android.R.color.white
                )
            )
        }
    }

    private fun applyPhotoCropAspectRatio() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = binding.root.width.takeIf { it > 0 } ?: displayMetrics.widthPixels
        val screenHeight = binding.root.height.takeIf { it > 0 } ?: displayMetrics.heightPixels
        imageCapture?.setCropAspectRatio(photoAspectRatio.cropRatio(screenWidth, screenHeight))
    }

    private fun updatePreviewBounds(animate: Boolean = false) {
        binding.root.post {
            val rootWidth = binding.root.width
            if (rootWidth <= 0) return@post
            val cameraHeight = (binding.cameraArea.bottom - binding.cameraArea.top).coerceAtLeast(0)
            val threeFourSize = PhotoAspectRatio.THREE_FOUR.fitInside(rootWidth, cameraHeight)
            val threeFourTop = binding.cameraArea.bottom - threeFourSize.height
            val (size, top) = if (mode == CameraMode.VIDEO) {
                Size(rootWidth, cameraHeight) to binding.cameraArea.top
            } else {
                when (photoAspectRatio) {
                    PhotoAspectRatio.THREE_FOUR -> threeFourSize to threeFourTop
                    PhotoAspectRatio.NINE_SIXTEEN -> photoAspectRatio.sizeForWidth(rootWidth).let {
                        it to binding.cameraModeBar.top - it.height
                    }
                    PhotoAspectRatio.ONE_ONE -> photoAspectRatio
                        .fitInside(threeFourSize.width, threeFourSize.height)
                        .let { it to threeFourTop + (threeFourSize.height - it.height) / 2 }
                    PhotoAspectRatio.FULL -> Size(rootWidth, binding.root.height) to 0
                }
            }
            val preview = binding.cameraPreviewFrame
            val layoutParams = preview.layoutParams as FrameLayout.LayoutParams
            if (animate && preview.height > 0) {
                TransitionManager.beginDelayedTransition(
                    binding.root,
                    ChangeBounds().apply {
                        duration = PREVIEW_RATIO_ANIMATION_MS
                        interpolator = AccelerateDecelerateInterpolator()
                        addTarget(preview)
                    }
                )
            }
            layoutParams.width = size.width
            layoutParams.height = size.height
            layoutParams.topMargin = top
            preview.layoutParams = layoutParams
        }
    }

    private fun maybeCapturePhoto() {
        maybeCaptureAfterLocationCheck(PendingCapture.PHOTO)
    }

    private fun maybeStartVideoRecording() {
        if (videoCapture == null) {
            Toast.makeText(context, "Error starting camera", Toast.LENGTH_SHORT).show()
            return
        }
        maybeCaptureAfterLocationCheck(PendingCapture.VIDEO)
    }

    private fun maybeCaptureAfterLocationCheck(pendingCapture: PendingCapture) {
        if (!shouldCheckLocationBeforeCapture()) {
            completePendingCapture(pendingCapture)
            return
        }

        pendingCaptureAfterLocationCheck = pendingCapture
        if (!hasLocationPermission()) {
            maybeChangeTemporaryTimeout()
            requestLocationPermission(C.LOCATION_PERMISSION)
            return
        }

        continueCaptureAfterLocationCheck()
    }

    private fun continueCaptureAfterLocationCheck() {
        if (pendingCaptureAfterLocationCheck == PendingCapture.NONE) return

        if (!hasLocationPermission() || gpsPromptIgnoredForCurrentSession) {
            completePendingCapture()
            return
        }

        startLocationMetadataListening()
        checkLocationSettings(C.START_CAMERA_CAPTURE) {
            completePendingCapture()
        }
    }

    private fun completePendingCapture(pendingCapture: PendingCapture = pendingCaptureAfterLocationCheck) {
        pendingCaptureAfterLocationCheck = PendingCapture.NONE
        when (pendingCapture) {
            PendingCapture.PHOTO -> takePhoto()
            PendingCapture.VIDEO -> startVideoRecording()
            PendingCapture.NONE -> {}
        }
    }

    private fun takePhoto() {
        pendingCaptureAfterLocationCheck = PendingCapture.NONE
        playCameraSound(MediaActionSound.SHUTTER_CLICK)
        captureImage()
        divviupUtils.runPhotoTakenEvent()
    }

    private fun startVideoRecording() {
        pendingCaptureAfterLocationCheck = PendingCapture.NONE
        videoRecording = true
        lastClickTime = System.currentTimeMillis()
        playCameraSound(MediaActionSound.START_VIDEO_RECORDING)
        if (!recordVideo()) {
            videoRecording = false
            return
        }
        durationView.start()
        captureButton.displayStopVideo()
        binding.cameraModeBar.visibility = View.INVISIBLE
        gridButton.visibility = View.GONE
        switchButton.visibility = View.GONE
        resolutionButton.visibility = View.GONE
    }

    private fun shouldCheckLocationBeforeCapture(): Boolean {
        return !Preferences.isAnonymousMode()
    }

    enum class CameraMode {
        PHOTO, VIDEO
    }

    private enum class PendingCapture {
        NONE, PHOTO, VIDEO
    }

    enum class IntentMode {
        COLLECT, RETURN, STAND, ODK
    }

    companion object {
        const val MEDIA_FILE_KEY = "mfk"
        const val VAULT_CURRENT_ROOT_PARENT = "vcrf"
        private const val CLICK_DELAY = 1200
        private const val PHOTO_ASPECT_RATIO_STATE = "photo_aspect_ratio"
        private const val PREVIEW_RATIO_ANIMATION_MS = 350L

        @JvmField
        var CAMERA_MODE = "cm"

        @JvmField
        var INTENT_MODE = "im"
        var CAPTURE_WITH_AUTO_UPLOAD = "capture_with_auto_upload"

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera() {
        previewController.resetFocus()
        camera = null
        val viewFinder = binding.viewFinder

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            if (mode != CameraMode.PHOTO) return@addListener
            try {
                cameraProvider = cameraProviderFuture.get()
                val rotation = viewFinder.display?.rotation ?: Surface.ROTATION_0

                val localCameraProvider = cameraProvider
                    ?: throw IllegalStateException("Camera initialization failed.")

                localCameraProvider.unbindAll()

                preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setTargetRotation(rotation)
                    .build()

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setFlashMode(flashMode)
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setTargetRotation(rotation)
                    .build()
                applyPhotoCropAspectRatio()

                bindToLifecycle(localCameraProvider, viewFinder)
            } catch (_: InterruptedException) {
                Toast.makeText(context, "Error starting camera", Toast.LENGTH_SHORT).show()
                return@addListener
            } catch (_: ExecutionException) {
                Toast.makeText(context, "Error starting camera", Toast.LENGTH_SHORT).show()
                return@addListener
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startVideo() {
        previewController.resetFocus()
        camera = null
        val viewFinder = binding.viewFinder

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            if (mode != CameraMode.VIDEO) return@addListener
            cameraProvider = cameraProviderFuture.get()
            val metrics = DisplayMetrics().also { viewFinder.display?.getRealMetrics(it) }
            val aspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
            val rotation = viewFinder.display?.rotation ?: Surface.ROTATION_0

            val localCameraProvider = cameraProvider
                ?: throw IllegalStateException("Camera initialization failed.")

            preview = Preview.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetRotation(rotation)
                .build()

            initVideoResolutionManager(localCameraProvider)
            val qualitySelector = videoResolutionManager?.qualitySelector
                ?: QualitySelector.fromOrderedList(
                    listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD),
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                )
            val recorder = Recorder.Builder()
                .setExecutor(ContextCompat.getMainExecutor(context))
                .setQualitySelector(qualitySelector)
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            localCameraProvider.unbindAll()

            try {
                camera = localCameraProvider.bindToLifecycle(
                    this,
                    lensFacing,
                    preview,
                    videoCapture,
                )
                preview?.surfaceProvider = viewFinder.surfaceProvider
                updateCameraControlVisibility(localCameraProvider)
                onCameraUseCasesBound()
            } catch (e: Exception) {
                Timber.e("Failed to bind use cases %s", e.message)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun bindToLifecycle(
        localCameraProvider: ProcessCameraProvider,
        viewFinder: PreviewView
    ) {
        try {
            camera = localCameraProvider.bindToLifecycle(
                this,
                hdrCameraSelector ?: lensFacing,
                preview,
                imageCapture,
            )
            preview?.surfaceProvider = viewFinder.surfaceProvider
            updateCameraControlVisibility(localCameraProvider)
            onCameraUseCasesBound()
        } catch (e: Exception) {
            Timber.e("Failed to bind use cases")
        }
    }

    private fun onCameraUseCasesBound() {
        cameraStopped = false
        resumePendingLocationSettingsCheck()
    }

    private fun playCameraSound(soundName: Int) {
        if (!shouldPlayCameraSound(soundName)) return
        try {
            if (shutterSound == null) {
                shutterSound = MediaActionSound()
            }
            shutterSound?.load(soundName)
            shutterSound?.play(soundName)
        } catch (e: Exception) {
            Timber.d(e, "Unable to play camera sound")
        }
    }

    private fun shouldPlayCameraSound(soundName: Int): Boolean {
        if (mustPlayShutterSound(soundName)) {
            return true
        }

        return !Preferences.isShutterMute()
    }

    private fun mustPlayShutterSound(soundName: Int): Boolean {
        val isCaptureSound = soundName == MediaActionSound.SHUTTER_CLICK ||
                soundName == MediaActionSound.START_VIDEO_RECORDING

        return isCaptureSound &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                MediaActionSound.mustPlayShutterSound()
    }

    private fun captureImage() {
        val localImageCapture =
            imageCapture ?: throw IllegalStateException("Camera initialization failed.")

        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA
        }

        val photoTempFile = MediaFileHandler.getTempFile()
        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(photoTempFile).setMetadata(metadata).build()

        localImageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    outputFileResults.savedUri?.let { uri ->
                        contentResolver.openInputStream(uri)?.use { iStream ->
                            viewModel.addJpegPhoto(iStream.readBytes(), currentRootParent)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Timber.e("Photo capture failed: ${exception.message}")
                    CrashReporterProvider.get().recordException(exception)
                }
            }
        )
    }

    // withAudioEnabled() is only called after hasAudioPermission() confirms RECORD_AUDIO is granted.
    @SuppressLint("MissingPermission")
    private fun recordVideo(): Boolean {
        try {
            if (recording != null) {
                recording?.stop()
                return true
            }

            val capture = videoCapture ?: return false

            tempFile = MediaFileHandler.getTempFile()
            val fileOutputOptions = FileOutputOptions.Builder(tempFile!!).build()

            var pendingRecording = capture.output.prepareRecording(context, fileOutputOptions)
            if (hasAudioPermission()) {
                pendingRecording = pendingRecording.withAudioEnabled()
            }

            val file = tempFile
            recording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Finalize -> {
                        if (!event.hasError()) {
                            file?.takeIf { it.exists() && it.length() > 0L }
                                ?.let { showConfirmVideoView(it) }
                        } else {
                            Timber.e(
                                "Video capture ends with error: ${event.error}, cause: ${event.cause}"
                            )
                        }
                        recording?.close()
                        recording = null
                        captureButton.displayVideoButton()
                        durationView.stop()
                        videoRecording = false
                        binding.cameraModeBar.visibility = View.VISIBLE
                        gridButton.visibility = View.VISIBLE
                        switchButton.visibility = View.VISIBLE
                        if (videoResolutionManager != null) {
                            resolutionButton.visibility = View.VISIBLE
                        }
                    }
                }
            }
            isRecording = recording != null
            return recording != null
        } catch (e: Exception) {
            Timber.e(e, "Error recording video")
            return false
        }
    }

    private fun hasAudioPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCameraPermissions(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermissions(requestCode: Int) {
        maybeChangeTemporaryTimeout()
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ), requestCode
        )
    }

    private fun requestLocationPermission(requestCode: Int) {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            requestCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == C.CAMERA_PERMISSION) {
            if (hasCameraPermissions(this)) {
                resumeCameraPreview()
            }
            return
        }

        if (requestCode != C.LOCATION_PERMISSION || pendingCaptureAfterLocationCheck == PendingCapture.NONE) {
            return
        }

        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            continueCaptureAfterLocationCheck()
        } else {
            completePendingCapture()
        }
    }

}
