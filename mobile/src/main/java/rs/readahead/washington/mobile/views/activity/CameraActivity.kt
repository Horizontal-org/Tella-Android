package rs.readahead.washington.mobile.views.activity

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Bundle
import android.view.OrientationEventListener
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.hzontal.tella_vault.VaultFile
import com.otaliastudios.cameraview.CameraException
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraOptions
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Flash
import com.otaliastudios.cameraview.controls.Grid
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.gesture.Gesture
import com.otaliastudios.cameraview.gesture.GestureAction
import com.otaliastudios.cameraview.size.SizeSelector
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.CaptureEvent
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.databinding.ActivityCameraBinding
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.mvp.contract.ICameraPresenterContract
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadSchedulePresenterContract
import rs.readahead.washington.mobile.mvp.presenter.CameraPresenter
import rs.readahead.washington.mobile.mvp.presenter.MetadataAttacher
import rs.readahead.washington.mobile.mvp.presenter.TellaFileUploadSchedulePresenter
import rs.readahead.washington.mobile.util.C
import rs.readahead.washington.mobile.util.DialogsUtil
import rs.readahead.washington.mobile.util.VideoResolutionManager
import rs.readahead.washington.mobile.views.activity.camera.CameraViewModel
import rs.readahead.washington.mobile.views.custom.CameraCaptureButton
import rs.readahead.washington.mobile.views.custom.CameraDurationTextView
import rs.readahead.washington.mobile.views.custom.CameraFlashButton
import rs.readahead.washington.mobile.views.custom.CameraGridButton
import rs.readahead.washington.mobile.views.custom.CameraResolutionButton
import rs.readahead.washington.mobile.views.custom.CameraSwitchButton
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.VAULT_FILE_KEY
import java.io.File

class CameraActivity : MetadataActivity(), ICameraPresenterContract.IView,
    ITellaFileUploadSchedulePresenterContract.IView, IMetadataAttachPresenterContract.IView {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraView: CameraView
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
    private var presenter: CameraPresenter? = null
    private var uploadPresenter: TellaFileUploadSchedulePresenter? = null
    private var metadataAttacher: MetadataAttacher? = null
    private var mode: CameraMode? = null
    private var modeLocked = false
    private var intentMode: IntentMode? = null
    private var videoRecording = false
    private var progressDialog: ProgressDialog? = null
    private var mOrientationEventListener: OrientationEventListener? = null
    private var zoomLevel = 0
    private var capturedMediaFile: VaultFile? = null
    private var videoQualityDialog: AlertDialog? = null
    private var videoResolutionManager: VideoResolutionManager? = null
    private var lastClickTime = System.currentTimeMillis()
    private var currentRootParent: String? = null
    private var captureWithAutoUpload = true
    private val viewModel by viewModels<CameraViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(
            layoutInflater
        )
        setContentView(binding.getRoot())
        initView()
        initListeners()
        overridePendingTransition(R.anim.slide_in_up, R.anim.fade_out)
        presenter = CameraPresenter(this)
        uploadPresenter = TellaFileUploadSchedulePresenter(this)
        metadataAttacher = MetadataAttacher(this)
        mode = CameraMode.PHOTO
        if (intent.hasExtra(CAMERA_MODE)) {
            mode = CameraMode.valueOf(
                intent.getStringExtra(CAMERA_MODE)!!
            )
            modeLocked = true
        }
        intentMode = IntentMode.RETURN
        if (intent.hasExtra(INTENT_MODE)) {
            intentMode = IntentMode.valueOf(
                intent.getStringExtra(INTENT_MODE)!!
            )
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
        setupShutterSound()
        checkLocationSettings(C.START_CAMERA_CAPTURE) {}
    }

    override fun onResume() {
        super.onResume()
        mOrientationEventListener!!.enable()
        startLocationMetadataListening()
        cameraView.open()
        setVideoQuality()
        mSeekBar.progress = zoomLevel
        setCameraZoom()
        presenter!!.getLastMediaFile()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            maybeChangeTemporaryTimeout()
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onPause() {
        super.onPause()
        stopLocationMetadataListening()
        mOrientationEventListener!!.disable()
        if (videoRecording) {
            captureButton.performClick()
        }
        cameraView.close()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPresenter()
        hideProgressDialog()
        hideVideoResolutionDialog()
        cameraView.destroy()
    }

    override fun onBackPressed() {
        if (maybeStopVideoRecording()) return
        super.onBackPressed()
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up)
    }

    override fun onAddingStart() {
        progressDialog = DialogsUtil.showLightProgressDialog(
            this,
            getString(R.string.gallery_dialog_expl_encrypting)
        )
        if (Preferences.isShutterMute()) {
            val mgr = getSystemService(AUDIO_SERVICE) as AudioManager
            mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false)
        }
    }

    override fun onAddingEnd() {
        hideProgressDialog()
        showToast(R.string.gallery_toast_file_encrypted)
    }

    override fun onAddSuccess(bundle: VaultFile) {
        capturedMediaFile = bundle
        if (intentMode != IntentMode.COLLECT) {
            previewView.visibility = View.VISIBLE
            Glide.with(this).load(bundle.thumb).into(previewView)
        }
        if (!Preferences.isAnonymousMode()) {
            attachMediaFileMetadata(capturedMediaFile, metadataAttacher)
        } else {
            returnIntent(bundle)
        }
        if (captureWithAutoUpload) {
            scheduleFileUpload(capturedMediaFile)
        }
        MyApplication.bus().post(CaptureEvent())
    }

    override fun onAddError(error: Throwable) {
        showToast(R.string.gallery_toast_fail_saving_file)
    }

    override fun onMetadataAttached(vaultFile: VaultFile) {
        returnIntent(vaultFile)
    }

    private fun returnIntent(vaultFile: VaultFile) {
        val data = Intent()
        if (intentMode == IntentMode.ODK) {
            capturedMediaFile!!.metadata = vaultFile.metadata
            data.putExtra(MEDIA_FILE_KEY, capturedMediaFile)
            setResult(RESULT_OK, data)
            finish()
        } else if (intentMode == IntentMode.COLLECT) {
            capturedMediaFile!!.metadata = vaultFile.metadata
            val list: MutableList<String> = ArrayList()
            list.add(vaultFile.id)
            data.putExtra(VAULT_FILE_KEY, Gson().toJson(list))
            setResult(RESULT_OK, data)
            finish()
        } else {
            data.putExtra(C.CAPTURED_MEDIA_FILE_ID, vaultFile.metadata)
            setResult(RESULT_OK, data)
        }
    }

    override fun onMetadataAttachError(throwable: Throwable) {
        onAddError(throwable)
    }

    private fun initListeners() {
        binding.close.setOnClickListener { view: View? -> closeCamera() }
        captureButton.setOnClickListener { view: View? -> onCaptureClicked() }
        binding.photoMode.setOnClickListener { view: View? -> onPhotoClicked() }
        binding.videoMode.setOnClickListener { view: View? -> onVideoClicked() }
        gridButton.setOnClickListener { view: View? -> onGridClicked() }
        switchButton.setOnClickListener { view: View? -> onSwitchClicked() }
        resolutionButton.setOnClickListener { view: View? -> chooseVideoResolution() }
        previewView.setOnClickListener { view: View? -> onPreviewClicked() }
    }

    private fun closeCamera() {
        onBackPressed()
    }

    override fun rotateViews(rotation: Int) {
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

    override fun onLastMediaFileSuccess(vaultFile: VaultFile) {
        if (intentMode != IntentMode.COLLECT) {
            previewView.visibility = View.VISIBLE
            Glide.with(this).load(vaultFile.thumb)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(previewView)
        }
    }

    override fun onLastMediaFileError(throwable: Throwable) {
        if (intentMode != IntentMode.COLLECT || intentMode == IntentMode.ODK) {
            previewView.visibility = View.GONE
        }
    }

    override fun getContext(): Context {
        return this
    }

    override fun onMediaFilesUploadScheduled() {
        if (intentMode != IntentMode.STAND) {
            finish()
        }
    }

    override fun onMediaFilesUploadScheduleError(throwable: Throwable) {}
    override fun onGetMediaFilesSuccess(mediaFiles: List<VaultFile>) {}
    override fun onGetMediaFilesError(error: Throwable) {}
    fun onCaptureClicked() {
        if (Preferences.isShutterMute()) {
            val mgr = getSystemService(AUDIO_SERVICE) as AudioManager
            mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true)
        }
        if (cameraView.mode == Mode.PICTURE) {
            cameraView.takePicture()
        } else {
            gridButton.visibility =
                if (videoRecording) View.VISIBLE else View.GONE
            switchButton.visibility = if (videoRecording) View.VISIBLE else View.GONE
            resolutionButton.visibility = if (videoRecording) View.VISIBLE else View.GONE
            if (videoRecording) {
                if (System.currentTimeMillis() - lastClickTime >= CLICK_DELAY) {
                    cameraView.stopVideo()
                    videoRecording = false
                    gridButton.visibility = View.VISIBLE
                    switchButton.visibility = View.VISIBLE
                    resolutionButton.visibility = View.VISIBLE
                }
            } else {
                setVideoQuality()
                lastClickTime = System.currentTimeMillis()
                cameraView.takeVideo(MediaFileHandler.getTempFile())
                captureButton.displayStopVideo()
                durationView.start()
                videoRecording = true
                gridButton.visibility = View.GONE
                switchButton.visibility = View.GONE
                resolutionButton.visibility = View.GONE
            }
        }
    }

    fun onPhotoClicked() {
        if (modeLocked) {
            return
        }
        if (System.currentTimeMillis() - lastClickTime < CLICK_MODE_DELAY) {
            return
        }
        if (cameraView.mode == Mode.PICTURE) {
            return
        }
        if (cameraView.flash == Flash.TORCH) {
            cameraView.flash = Flash.AUTO
        }
        setPhotoActive()
        captureButton.displayPhotoButton()
        captureButton.contentDescription =
            context.getString(R.string.Uwazi_WidgetMedia_Take_Photo)
        cameraView.mode = Mode.PICTURE
        mode = CameraMode.PHOTO
        resetZoom()
        lastClickTime = System.currentTimeMillis()
    }

    fun onVideoClicked() {
        if (modeLocked) {
            return
        }
        if (System.currentTimeMillis() - lastClickTime < CLICK_MODE_DELAY) {
            return
        }
        if (cameraView.mode == Mode.VIDEO) {
            return
        }
        cameraView.mode = Mode.VIDEO
        turnFlashDown()
        captureButton.displayVideoButton()
        captureButton.contentDescription =
            context.getString(R.string.Uwazi_WidgetMedia_Take_Video)
        setVideoActive()
        mode = CameraMode.VIDEO
        resetZoom()
        lastClickTime = System.currentTimeMillis()
    }

    fun onGridClicked() {
        if (cameraView.grid == Grid.DRAW_3X3) {
            cameraView.grid = Grid.OFF
            gridButton.displayGridOff()
            gridButton.contentDescription = getString(R.string.action_show_gridview)
        } else {
            cameraView.grid = Grid.DRAW_3X3
            gridButton.displayGridOn()
            gridButton.contentDescription = getString(R.string.action_hide_gridview)
        }
    }

    fun onSwitchClicked() {
        if (cameraView.facing == Facing.BACK) {
            switchCamera(Facing.FRONT, R.string.action_switch_to_back_camera)
        } else {
            switchCamera(Facing.BACK, R.string.action_switch_to_front_camera)
        }
    }

    private fun switchCamera(facing: Facing, contentDescriptionResId: Int) {
        cameraView.facing = facing
        switchButton.displayCamera(facing)
        switchButton.contentDescription = getString(contentDescriptionResId)
    }

    fun onPreviewClicked() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.PHOTO_VIDEO_FILTER, "filter")
        startActivity(intent)
        finish()
    }

    private fun resetZoom() {
        zoomLevel = 0
        mSeekBar.progress = 0
        setCameraZoom()
    }

    fun chooseVideoResolution() {
        if (videoResolutionManager != null) {
            videoQualityDialog = DialogsUtil.showVideoResolutionDialog(
                this,
                { videoSize: SizeSelector -> setVideoSize(videoSize) },
                videoResolutionManager
            )
        }
    }

    private fun setCameraZoom() {
        cameraView.zoom = zoomLevel.toFloat() / 100
    }

    private fun maybeStopVideoRecording(): Boolean {
        if (videoRecording) {
            captureButton.performClick()
            return true
        }
        return false
    }

    private fun stopPresenter() {
        if (presenter != null) {
            presenter!!.destroy()
            presenter = null
        }
    }

    private fun showConfirmVideoView(video: File) {
        displayVideoCaptureButton()
        durationView.stop()
        presenter!!.addMp4Video(video, currentRootParent)
    }

    /* handle display settings for the video capture button.*/
    private fun displayVideoCaptureButton() {
        captureButton.displayVideoButton()
        captureButton.contentDescription =
            context.getString(R.string.Uwazi_WidgetMedia_Take_Video)
    }

    private fun setupCameraView() {
        if (mode == CameraMode.PHOTO) {
            setCameraMode(Mode.PICTURE)
            displayPhotoCaptureButton()
        } else {
            setCameraMode(Mode.VIDEO)
            displayVideoCaptureButton()
        }

        //cameraView.setEnabled(PermissionUtil.checkPermission(this, Manifest.permission.CAMERA));
        cameraView.mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS)
        setOrientationListener()
        cameraView.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) {
                presenter!!.addJpegPhoto(result.data, currentRootParent)
            }

            override fun onVideoTaken(result: VideoResult) {
                showConfirmVideoView(result.file)
            }

            override fun onCameraError(exception: CameraException) {
                FirebaseCrashlytics.getInstance().recordException(exception)
            }

            override fun onCameraOpened(options: CameraOptions) {
                if (options.supports(Grid.DRAW_3X3)) {
                    gridButton.visibility = View.VISIBLE
                    setUpCameraGridButton()
                } else {
                    gridButton.visibility = View.GONE
                }
                if (options.supportedFacing.size < 2) {
                    switchButton.visibility = View.GONE
                } else {
                    switchButton.visibility = View.VISIBLE
                    setupCameraSwitchButton()
                }
                if (options.supportedFlash.size < 2) {
                    flashButton.visibility = View.INVISIBLE
                } else {
                    flashButton.visibility = View.VISIBLE
                    setupCameraFlashButton(options.supportedFlash)
                }
                if (options.supportedVideoSizes.size > 0) {
                    videoResolutionManager = VideoResolutionManager(options.supportedVideoSizes)
                }
                // options object has info
                super.onCameraOpened(options)
            }
        })
        mSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                zoomLevel = i
                setCameraZoom()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    /* handle the behavior of setting the camera mode.*/
    private fun setCameraMode(mode: Mode) {
        cameraView.mode = mode
    }

    /* handle display settings for the photo capture button.*/
    private fun displayPhotoCaptureButton() {
        captureButton.displayPhotoButton()
        captureButton.contentDescription =
            context.getString(R.string.Uwazi_WidgetMedia_Take_Photo)
    }

    private fun setupCameraModeButton() {
        if (cameraView.mode == Mode.PICTURE) {
            setPhotoActive()
        } else {
            setVideoActive()
        }
    }

    private fun setUpCameraGridButton() {
        if (cameraView.grid == Grid.DRAW_3X3) {
            gridButton.displayGridOn()
            gridButton.contentDescription = getString(R.string.action_hide_gridview)
        } else {
            gridButton.displayGridOff()
            gridButton.contentDescription = getString(R.string.action_show_gridview)
        }
    }

    private fun setupCameraSwitchButton() {
        if (cameraView.facing == Facing.FRONT) {
            switchCamera(Facing.FRONT, R.string.action_switch_to_back_camera)
        } else {
            switchCamera(Facing.BACK, R.string.action_switch_to_front_camera)
        }
    }

    private fun setupImagePreview() {
        if (intentMode == IntentMode.COLLECT || intentMode == IntentMode.ODK) {
            previewView.visibility = View.GONE
        }
    }

    private fun setupCameraFlashButton(supported: Collection<Flash>) {
        if (cameraView.flash == Flash.AUTO) {
            flashButton.displayFlashAuto()
        } else if (cameraView.flash == Flash.OFF) {
            flashButton.displayFlashOff()
            flashButton.contentDescription = getString(R.string.action_enable_flash)
        } else {
            flashButton.displayFlashOn()
            flashButton.contentDescription = getString(R.string.action_disable_flash)
        }
        flashButton.setOnClickListener { view: View? ->
            if (cameraView.mode == Mode.VIDEO) {
                if (cameraView.flash == Flash.OFF && supported.contains(Flash.TORCH)) {
                    flashButton.displayFlashOn()
                    cameraView.flash = Flash.TORCH
                } else {
                    turnFlashDown()
                }
            } else {
                if (cameraView.flash == Flash.ON || cameraView.flash == Flash.TORCH) {
                    turnFlashDown()
                } else if (cameraView.flash == Flash.OFF && supported.contains(Flash.AUTO)) {
                    flashButton.displayFlashAuto()
                    cameraView.flash = Flash.AUTO
                } else {
                    flashButton.displayFlashOn()
                    cameraView.flash = Flash.ON
                }
            }
        }
    }

    private fun turnFlashDown() {
        flashButton.displayFlashOff()
        cameraView.flash = Flash.OFF
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
                        presenter!!.handleRotation(orientation)
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

    private fun hideVideoResolutionDialog() {
        if (videoQualityDialog != null) {
            videoQualityDialog!!.dismiss()
            videoQualityDialog = null
        }
    }

    private fun setVideoQuality() {
        if (cameraView != null && videoResolutionManager != null) {
            cameraView.setVideoSize(videoResolutionManager!!.videoSize)
        }
    }

    private fun setVideoSize(videoSize: SizeSelector) {
        if (cameraView != null) {
            cameraView.setVideoSize(videoSize)
            cameraView.close()
            cameraView.open()
        }
    }

    private fun scheduleFileUpload(vaultFile: VaultFile?) {
        if (Preferences.isAutoUploadEnabled()) {
            uploadPresenter!!.scheduleUploadReportFiles(
                vaultFile,
                Preferences.getAutoUploadServerId()
            )
        }
    }

    private fun setupShutterSound() {
        cameraView.playSounds = !Preferences.isShutterMute()
    }

    private fun initView() {
        cameraView = binding.camera
        gridButton = binding.gridButton
        switchButton = binding.switchButton
        flashButton = binding.flashButton
        captureButton = binding.captureButton
        durationView = binding.durationView
        mSeekBar = binding.cameraZoom
        videoLine = binding.videoLine
        photoLine = binding.photoLine
        previewView = binding.previewImage
        photoModeText = binding.photoText
        videoModeText = binding.videoText
        resolutionButton = binding.resolutionButton
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
    }
}