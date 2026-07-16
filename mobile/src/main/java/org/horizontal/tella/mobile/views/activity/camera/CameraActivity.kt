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
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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
import java.util.concurrent.TimeUnit
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
    private lateinit var mSeekBar: SeekBar
    private lateinit var videoLine: View
    private lateinit var photoLine: View
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
    private var zoomLevel = 0
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
    private var shutterSound: MediaActionSound? = null
    private var hasResumedOnce = false
    private var pendingPhotoCaptureAfterLocationCheck = false
    private var gpsPromptIgnoredForCurrentSession = false

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
        binding.viewFinder.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val mandatory = insets.getInsets(WindowInsetsCompat.Type.mandatorySystemGestures())
            var top = maxOf(statusBars.top, cutout.top, mandatory.top)
            if (top == 0) {
                val ignoring = insets.getInsetsIgnoringVisibility(
                    WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
                )
                top = maxOf(top, ignoring.top)
            }
            val bottom = maxOf(navBars.bottom, mandatory.bottom)
            val left = maxOf(statusBars.left, navBars.left, cutout.left, mandatory.left)
            val right = maxOf(statusBars.right, navBars.right, cutout.right, mandatory.right)
            view.setPadding(left, top, right, bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
        initView()
        overridePendingTransition(R.anim.slide_in_up, R.anim.fade_out)
        metadataAttacher = MetadataAttacher(this)
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
        if (intent.hasExtra(CAPTURE_WITH_AUTO_UPLOAD)) {
            captureWithAutoUpload = intent.getBooleanExtra(CAPTURE_WITH_AUTO_UPLOAD, false)
        }
        setupCameraView()
        setupCameraModeButton()
        setupImagePreview()
        initObservers()
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

    override fun onResume() {
        super.onResume()
        mOrientationEventListener?.enable()
        startLocationMetadataListening()
        mSeekBar.progress = zoomLevel
        viewModel.getLastMediaFile()
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            maybeChangeTemporaryTimeout()
        } else if (hasResumedOnce) {
            resumeCameraPreview()
        }
        hasResumedOnce = true
    }

    override fun onPause() {
        super.onPause()
        stopLocationMetadataListening()
        mOrientationEventListener?.disable()
        if (videoRecording) {
            captureButton.performClick()
        }
        cameraProvider?.unbindAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideProgressDialog()
        hideVideoResolutionDialog()
        shutterSound?.release()
        shutterSound = null
        cameraProvider?.unbindAll()
    }

    private fun resumeCameraPreview() {
        if (mode == CameraMode.VIDEO) {
            startVideo()
        } else {
            startCamera()
        }
        setCameraZoom()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
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

        super.onBackPressed()
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
        if (captureWithAutoUpload) {
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
        onBackPressed()
    }

    private fun rotateViews(rotation: Int) {
        gridButton.rotateView(rotation)
        switchButton.rotateView(rotation)
        flashButton.rotateView(rotation)
        durationView.rotateView(rotation)
        captureButton.rotateView(rotation)
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
        if (modeLocked) return
        if (System.currentTimeMillis() - lastClickTime < CLICK_MODE_DELAY) return
        if (mode == CameraMode.PHOTO) return

        if (mode == CameraMode.VIDEO) {
            turnFlashDown()
        }
        setPhotoActive()
        captureButton.displayPhotoButton()
        captureButton.contentDescription = context.getString(R.string.Uwazi_WidgetMedia_Take_Photo)
        mode = CameraMode.PHOTO
        startCamera()
        resetZoom()
        lastClickTime = System.currentTimeMillis()
    }

    private fun onVideoClicked() {
        if (modeLocked) return
        if (System.currentTimeMillis() - lastClickTime < CLICK_MODE_DELAY) return
        if (mode == CameraMode.VIDEO) return

        startVideo()
        turnFlashDown()
        captureButton.displayVideoButton()
        captureButton.contentDescription = context.getString(R.string.Uwazi_WidgetMedia_Take_Video)
        setVideoActive()
        mode = CameraMode.VIDEO
        resetZoom()
        lastClickTime = System.currentTimeMillis()
    }

    private fun onGridClicked() {
        gridEnabled = !gridEnabled
        updateGridOverlay()
        if (gridEnabled) {
            gridButton.displayGridOn()
            gridButton.contentDescription = getString(R.string.action_hide_gridview)
        } else {
            gridButton.displayGridOff()
            gridButton.contentDescription = getString(R.string.action_show_gridview)
        }
    }

    private fun onSwitchClicked() {
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

    private fun onFlashClicked() {
        if (mode == CameraMode.VIDEO) {
            if (!flashSupportsTorch) return
            if (flashMode == ImageCapture.FLASH_MODE_OFF) {
                flashMode = ImageCapture.FLASH_MODE_ON
                flashButton.displayFlashOn()
                camera?.cameraControl?.enableTorch(true)
            } else {
                turnFlashDown()
            }
        } else {
            when (flashMode) {
                ImageCapture.FLASH_MODE_OFF -> {
                    if (flashSupportsAuto) {
                        flashMode = ImageCapture.FLASH_MODE_AUTO
                        flashButton.displayFlashAuto()
                        imageCapture?.flashMode = ImageCapture.FLASH_MODE_AUTO
                    } else {
                        flashMode = ImageCapture.FLASH_MODE_ON
                        flashButton.displayFlashOn()
                        imageCapture?.flashMode = ImageCapture.FLASH_MODE_ON
                    }
                }
                ImageCapture.FLASH_MODE_AUTO -> {
                    flashMode = ImageCapture.FLASH_MODE_ON
                    flashButton.displayFlashOn()
                    imageCapture?.flashMode = ImageCapture.FLASH_MODE_ON
                }
                else -> {
                    turnFlashDown()
                }
            }
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

    private fun resetZoom() {
        zoomLevel = 0
        mSeekBar.progress = 0
        setCameraZoom()
    }

    private fun setCameraZoom() {
        camera?.cameraControl?.setLinearZoom(zoomLevel.toFloat() / 100f)
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
            startCamera()
        } else {
            captureButton.displayVideoButton()
            startVideo()
        }
        setupCameraGridButton()
        setupPreviewTapToFocus()
        setOrientationListener()
        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                zoomLevel = i
                setCameraZoom()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
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
        updateGridOverlay()
        if (gridEnabled) {
            gridButton.displayGridOn()
            gridButton.contentDescription = getString(R.string.action_hide_gridview)
        } else {
            gridButton.displayGridOff()
            gridButton.contentDescription = getString(R.string.action_show_gridview)
        }
    }

    private fun updateGridOverlay() {
        binding.gridOverlay.visibility = if (gridEnabled) View.VISIBLE else View.GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPreviewTapToFocus() {
        binding.viewFinder.setOnTouchListener { _, event ->
            if (event.action != MotionEvent.ACTION_UP) {
                return@setOnTouchListener false
            }
            val cameraControl = camera?.cameraControl ?: return@setOnTouchListener false
            val factory = binding.viewFinder.meteringPointFactory
            val point = factory.createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()
            cameraControl.startFocusAndMetering(action)
            true
        }
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
    }

    private fun updateFlashCapabilities(localCameraProvider: ProcessCameraProvider) {
        val hasFlash = deviceHasFlash(localCameraProvider)
        flashSupportsAuto = hasFlash && isBackCamera
        flashSupportsTorch = hasFlash
    }

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
        when (flashMode) {
            ImageCapture.FLASH_MODE_AUTO -> {
                flashButton.displayFlashAuto()
                flashButton.contentDescription = getString(R.string.action_disable_flash)
            }
            ImageCapture.FLASH_MODE_ON -> {
                flashButton.displayFlashOn()
                flashButton.contentDescription = getString(R.string.action_disable_flash)
            }
            else -> {
                flashButton.displayFlashOff()
                flashButton.contentDescription = getString(R.string.action_enable_flash)
            }
        }
    }

    private fun turnFlashDown() {
        flashMode = ImageCapture.FLASH_MODE_OFF
        flashButton.displayFlashOff()
        imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
        camera?.cameraControl?.enableTorch(false)
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
        videoLine.visibility = View.GONE
        photoLine.visibility = View.VISIBLE
        photoModeText.alpha = 1f
        videoModeText.alpha = if (modeLocked) 0.1f else 0.5f
        resolutionButton.visibility = View.GONE
    }

    private fun setVideoActive() {
        videoLine.visibility = View.VISIBLE
        photoLine.visibility = View.GONE
        videoModeText.alpha = 1f
        photoModeText.alpha = if (modeLocked) 0.1f else 0.5f
        if (videoResolutionManager != null) {
            resolutionButton.visibility = View.VISIBLE
        }
    }

    private fun chooseVideoResolution() {
        videoResolutionManager?.let { manager ->
            videoQualityDialog = CameraDialogsUtil.showVideoResolutionDialog(
                this,
                CameraDialogsUtil.VideoQualityConsumer { applyVideoQuality() },
                manager
            )
        }
    }

    private fun applyVideoQuality() {
        if (mode == CameraMode.VIDEO) {
            startVideo()
        }
    }

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

    private fun scheduleFileUpload(vaultFile: VaultFile) {
        if (Preferences.isAutoUploadEnabled()) {
            uploadViewModel.scheduleUploadReportFiles(
                vaultFile, Preferences.getAutoUploadServerId()
            )
        }
    }

    private fun initView() {
        if (!hasCameraPermissions(context)) {
            maybeChangeTemporaryTimeout()
            requestCameraPermissions(C.CAMERA_PERMISSION)
        }

        gridButton = binding.gridButton
        switchButton = binding.switchButton
        switchButton.displayCamera(isBackCamera)
        flashButton = binding.flashButton
        updateFlashButtonIcon()
        captureButton = binding.captureButton
        durationView = binding.durationView
        mSeekBar = binding.cameraZoom
        videoLine = binding.videoLine
        photoLine = binding.photoLine
        previewView = binding.previewImage
        photoModeText = binding.photoText
        videoModeText = binding.videoText
        resolutionButton = binding.resolutionButton

        binding.close.setOnClickListener { closeCamera() }

        binding.captureButton.setOnClickListener {
            if (mode == CameraMode.PHOTO) {
                maybeCapturePhoto()
            } else {
                if (videoRecording) {
                    if (System.currentTimeMillis() - lastClickTime >= CLICK_DELAY) {
                        playCameraSound(MediaActionSound.STOP_VIDEO_RECORDING)
                        recordVideo()
                        divviupUtils.runVideoTakenEvent()
                    }
                } else {
                    if (videoCapture == null) {
                        Toast.makeText(context, "Error starting camera", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    videoRecording = true
                    lastClickTime = System.currentTimeMillis()
                    playCameraSound(MediaActionSound.START_VIDEO_RECORDING)
                    if (!recordVideo()) {
                        videoRecording = false
                        return@setOnClickListener
                    }
                    durationView.start()
                    captureButton.displayStopVideo()
                    gridButton.visibility = View.GONE
                    switchButton.visibility = View.GONE
                    resolutionButton.visibility = View.GONE
                }
            }
        }

        binding.photoMode.setOnClickListener { onPhotoClicked() }
        binding.videoMode.setOnClickListener { onVideoClicked() }
        binding.gridButton.setOnClickListener { onGridClicked() }
        binding.switchButton.setOnClickListener { onSwitchClicked() }
        binding.flashButton.setOnClickListener { onFlashClicked() }
        binding.previewImage.setOnClickListener { onPreviewClicked() }
        binding.resolutionButton.setOnClickListener { chooseVideoResolution() }
    }

    private fun maybeCapturePhoto() {
        if (!shouldCheckLocationBeforePhotoCapture()) {
            takePhoto()
            return
        }

        pendingPhotoCaptureAfterLocationCheck = true
        if (!hasLocationPermission()) {
            maybeChangeTemporaryTimeout()
            requestLocationPermission(C.LOCATION_PERMISSION)
            return
        }

        continuePhotoCaptureAfterLocationCheck()
    }

    private fun continuePhotoCaptureAfterLocationCheck() {
        if (!pendingPhotoCaptureAfterLocationCheck) return

        if (!hasLocationPermission()) {
            takePhoto()
            return
        }

        if (gpsPromptIgnoredForCurrentSession) {
            takePhoto()
            return
        }

        startLocationMetadataListening()
        checkLocationSettings(C.START_CAMERA_CAPTURE) {
            takePhoto()
        }
    }

    private fun takePhoto() {
        pendingPhotoCaptureAfterLocationCheck = false
        playCameraSound(MediaActionSound.SHUTTER_CLICK)
        captureImage()
        divviupUtils.runPhotoTakenEvent()
    }

    private fun shouldCheckLocationBeforePhotoCapture(): Boolean {
        return !Preferences.isAnonymousMode()
    }

    enum class CameraMode {
        PHOTO, VIDEO
    }

    enum class IntentMode {
        COLLECT, RETURN, STAND, ODK
    }

    companion object {
        const val MEDIA_FILE_KEY = "mfk"
        const val VAULT_CURRENT_ROOT_PARENT = "vcrf"
        private const val CLICK_DELAY = 1200
        private const val CLICK_MODE_DELAY = 2000

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
        val viewFinder = binding.viewFinder

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val metrics = DisplayMetrics().also { viewFinder.display?.getRealMetrics(it) }
                val aspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
                val rotation = viewFinder.display?.rotation ?: Surface.ROTATION_0

                val localCameraProvider = cameraProvider
                    ?: throw IllegalStateException("Camera initialization failed.")

                localCameraProvider.unbindAll()

                preview = Preview.Builder()
                    .setTargetAspectRatio(aspectRatio)
                    .setTargetRotation(rotation)
                    .build()

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setFlashMode(flashMode)
                    .setTargetAspectRatio(aspectRatio)
                    .setTargetRotation(rotation)
                    .build()

                bindToLifecycle(localCameraProvider, viewFinder)
            } catch (e: InterruptedException) {
                Toast.makeText(context, "Error starting camera", Toast.LENGTH_SHORT).show()
                return@addListener
            } catch (e: ExecutionException) {
                Toast.makeText(context, "Error starting camera", Toast.LENGTH_SHORT).show()
                return@addListener
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startVideo() {
        val viewFinder = binding.viewFinder

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
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
                preview?.setSurfaceProvider(viewFinder.surfaceProvider)
                updateCameraControlVisibility(localCameraProvider)
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
            preview?.setSurfaceProvider(viewFinder.surfaceProvider)
            updateCameraControlVisibility(localCameraProvider)
        } catch (e: Exception) {
            Timber.e("Failed to bind use cases")
        }
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

        if (requestCode != C.LOCATION_PERMISSION || !pendingPhotoCaptureAfterLocationCheck) {
            return
        }

        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            continuePhotoCaptureAfterLocationCheck()
        } else {
            takePhoto()
        }
    }

}
