package org.horizontal.tella.mobile.views.fragment.recorder

import android.Manifest
import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.MyApplication
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.event.AudioRecordEvent
import org.horizontal.tella.mobile.data.sharedpref.Preferences
import org.horizontal.tella.mobile.media.MediaFileHandler
import org.horizontal.tella.mobile.mvp.contract.IMetadataAttachPresenterContract
import org.horizontal.tella.mobile.mvp.presenter.MetadataAttacher
import org.horizontal.tella.mobile.util.C.RECORD_REQUEST_CODE
import org.horizontal.tella.mobile.util.StringUtils
import org.horizontal.tella.mobile.views.activity.MetadataActivity
import org.horizontal.tella.mobile.views.activity.camera.CameraActivity.Companion.VAULT_CURRENT_ROOT_PARENT
import org.horizontal.tella.mobile.views.activity.viewer.toolBar
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_VAULT_FILE
import org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow.PREPARE_UPLOAD_ENTRY
import org.horizontal.tella.mobile.views.interfaces.VerificationWorkStatusCallback
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MicActivity : MetadataActivity(),
    IMetadataAttachPresenterContract.IView {
    private var animator: ObjectAnimator? = null
    private var isCollect: Boolean = false
    private var isReport: Boolean = false
    private var isPrepareUpload = false
    private var notRecording = false
    private var lastUpdateTime: Long = 0
    private var isAddingInProgress = false

    // handling MediaFile
    private var handlingMediaFile: VaultFile? = null

    private val viewModel by viewModels<AudioCaptureViewModel>()

    private val bundle by lazy { Bundle() }

    private var callback: VerificationWorkStatusCallback? = null

    private lateinit var metadataAttacher: MetadataAttacher
    private lateinit var mRecord: ImageButton
    private lateinit var mPlay: ImageButton
    private lateinit var mPause: ImageButton
    private lateinit var mTimer: TextView
    private lateinit var freeSpace: TextView
    private lateinit var redDot: ImageView
    private lateinit var recordingName: TextView
    private var currentRootParent: String? = null
    private var currentRecordName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_mic) // Set your activity layout here

        if (intent != null) {
            isCollect = intent.getBooleanExtra(COLLECT_ENTRY, false)
            isReport = intent.getBooleanExtra(REPORT_ENTRY, false)
            isPrepareUpload = intent.getBooleanExtra(PREPARE_UPLOAD_ENTRY, false)
            currentRootParent = intent.getStringExtra(VAULT_CURRENT_ROOT_PARENT)
        }

        initView()

        handleBackStack()
    }

    fun initView() {
        mRecord = findViewById(R.id.record_audio)
        mPlay = findViewById(R.id.play_audio)
        mPause = findViewById(R.id.stop_audio)
        mTimer = findViewById(R.id.audio_time)
        freeSpace = findViewById(R.id.free_space)
        redDot = findViewById(R.id.red_dot)
        recordingName = findViewById(R.id.rec_name)
        toolBar = findViewById(R.id.toolbar)

        if (isCollect || isReport || isPrepareUpload || currentRootParent?.isNotEmpty() == true) {
            mPlay.visibility = View.GONE
        }

        if (isCollect || isPrepareUpload || currentRootParent?.isNotEmpty() == true) {
            toolBar.navigationIcon =
                ContextCompat.getDrawable(this, R.drawable.ic_close_white)

            // Set a click listener for the navigation icon
            toolBar.setNavigationOnClickListener {
                // Handle back or close action here
                finish()

            }
        }

        mRecord.setOnClickListener {
            if (notRecording) {
                if (hastRecordingPermissions(this)) {
                    handleRecord()
                } else {
                    maybeChangeTemporaryTimeout()
                    requestRecordingPermissions(RECORD_REQUEST_CODE)
                }
            } else {
                handleStop()
            }
        }

        updateRecordingName()
        recordingName.setOnClickListener {
            BottomSheetUtils.showFileRenameSheet(
                supportFragmentManager,
                getString(R.string.mic_rename_recording),
                getString(R.string.action_cancel),
                getString(R.string.action_ok),
                this,
                recordingName.text.toString()
            ) { it1 -> updateRecordingName(it1) }
        }

        mPause.setOnClickListener { handlePause() }

        mPlay.setOnClickListener { openRecordings() }

        metadataAttacher = MetadataAttacher(this)

        notRecording = true

        animator = AnimatorInflater.loadAnimator(
            this,
            R.animator.fade_in
        ) as ObjectAnimator

        mTimer.text = timeToString(0)
        disablePause()
        initObservers()
    }

    private fun initObservers() {
        viewModel.durationLiveData.observe(this, ::onDurationUpdate)
        viewModel.recordingStoppedLiveData.observe(this, ::onRecordingStopped)
        viewModel.availableStorageLiveData.observe(this, ::onAvailableStorage)
        viewModel.recordingErrorLiveData.observe(this, ::onRecordingError)
        viewModel.mediaFilesUploadScheduleError.observe(
            this,
            ::onMediaFilesUploadScheduleError
        )
        viewModel.addingInProgress.observe(this) { isAdding ->
            callback?.setBackgroundWorkStatus(isAdding && !Preferences.isAnonymousMode())
        }
        viewModel.isFileNameUnique.observe(this) { isUnique ->
            if (isUnique) {
                recordingName.text = currentRecordName
            } else {
                showToast(getString(R.string.file_name_taken))
            }
        }
    }

    private fun handleBackStack() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!Preferences.isAnonymousMode() && isAddingInProgress) {
                        BottomSheetUtils.showConfirmSheet(fragmentManager = supportFragmentManager,
                            getString(R.string.exit_and_discard_verification_info),
                            getString(R.string.recording_in_progress_exit_warning),
                            getString(R.string.exit_and_discard_info),
                            getString(R.string.back),
                            consumer = object : BottomSheetUtils.ActionConfirmed {
                                override fun accept(isConfirmed: Boolean) {

                                }
                            })
                        return
                    }
                }
            })
    }

    override fun onStart() {
        super.onStart()
        startLocationMetadataListening()
    }

    override fun onStop() {
        stopLocationMetadataListening()
        super.onStop()
    }

    override fun onDestroy() {
        animator?.end()
        animator = null
        cancelRecorder()
        stopPresenter()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkAvailableStorage()
    }

    private fun hastRecordingPermissions(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("NewApi")
    private fun requestRecordingPermissions(requestCode: Int) {
        requestPermissions(
            arrayOf(
                Manifest.permission.RECORD_AUDIO
            ), requestCode
        )
    }

    private fun handleRecord() {
        notRecording = false
        if (viewModel.isAudioRecorder()) {   //first start or restart
            disablePlay()
            handlingMediaFile = null
            cancelRecorder()
            viewModel.startRecording(recordingName.text.toString(), currentRootParent)
        } else {
            cancelPauseRecorder()
        }
        disableRecord()
        enablePause()
    }

    private fun onDurationUpdate(duration: Long) {
        runOnUiThread { mTimer.text = timeToString(duration) }

        if (duration > UPDATE_SPACE_TIME_MS + lastUpdateTime) {
            lastUpdateTime += UPDATE_SPACE_TIME_MS
            viewModel.checkAvailableStorage()
        }
    }

    private fun onAddSuccess(vaultFile: VaultFile) {
        divviupUtils.runAudioTakenEvent()
        if (!isCollect) {
            DialogUtils.showBottomMessage(
                this,
                String.format(
                    getString(R.string.recorder_toast_recording_saved),
                    getString(R.string.app_name)
                ), false
            )
        }

        if (!Preferences.isAnonymousMode()) {
            attachMediaFileMetadata(vaultFile, metadataAttacher)
        } else {
            onMetadataAttached(vaultFile)
        }
        scheduleFileUpload(vaultFile)
    }

    private fun onAddError(error: Throwable?) {
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.gallery_toast_fail_saving_file),
            true
        )
    }

    private fun onAvailableStorage(memory: Long) {
        updateStorageSpaceLeft(memory)
    }

    override fun onMetadataAttached(vaultFile: VaultFile?) {
        mTimer.text = timeToString(0)
        maybeReturnCollectRecording(vaultFile)
    }

    private fun scheduleFileUpload(vaultFile: VaultFile) {
        if (Preferences.isAutoUploadEnabled()) {
            viewModel.scheduleUploadReportFiles(
                vaultFile,
                Preferences.getAutoUploadServerId()
            )
        }
    }

    private fun maybeReturnCollectRecording(vaultFile: VaultFile?) {
        if (isCollect || isPrepareUpload) {
            MyApplication.bus().post(AudioRecordEvent(vaultFile))
        }
        if (isReport) {
            val bundle = Bundle()
            bundle.putSerializable(BUNDLE_REPORT_VAULT_FILE, vaultFile)
            //setFragmentResult(BUNDLE_REPORT_AUDIO, bundle)
            /// nav().navigateUp()
        }
    }

    override fun onMetadataAttachError(throwable: Throwable?) {
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.gallery_toast_fail_saving_file),
            true
        )
    }

    override fun getContext(): Context {
        return this
    }

    private fun onMediaFilesUploadScheduled() {
        val isAutoUploadEnabled = Preferences.isAutoUploadEnabled()
        val isAutoDeleteEnabled = Preferences.isAutoDeleteEnabled()

        val message: String = if (isAutoUploadEnabled && isAutoDeleteEnabled) {
            getString(R.string.Auto_Upload_Recording_Imported_Report_And_Deleted)
        } else if (isAutoUploadEnabled) {
            getString(R.string.Auto_Upload_Recording_Report)
        } else {
            return
        }

        DialogUtils.showBottomMessage(this, message, false)
    }

    private fun onMediaFilesUploadScheduleError(throwable: Throwable?) {
    }

    private fun handleStop() {
        disablePause()
        notRecording = true
        stopRecorder()
    }

    private fun handlePause() {
        pauseRecorder()
        enableRecord()
        notRecording = true
    }

    private fun onRecordingStopped(vaultFile: VaultFile?) {
        if (vaultFile == null) {
            handlingMediaFile = null
            disablePause()
            disablePlay()
            enableRecord()
        } else {
            handlingMediaFile =
                MediaFileHandler.renameFile(vaultFile, recordingName.text.toString())
            handlingMediaFile!!.size = MediaFileHandler.getSize(vaultFile)
            disablePause()
            enablePlay()
            enableRecord()

            // returnData();
            onAddSuccess(vaultFile)
        }
        updateRecordingName()
    }

    private fun onRecordingError(throwable: Throwable?) {
        handlingMediaFile = null
        disablePause()
        disablePlay()
        enableRecord()
        mTimer.text = timeToString(0)
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.recorder_toast_fail_recording),
            true
        )
    }

    private fun disableRecord() {
        mRecord.apply {
            background =
                AppCompatResources.getDrawable(this@MicActivity, R.drawable.red_circle_background)
            setImageResource(R.drawable.stop_white)
            contentDescription = getString(R.string.action_stop)
        }
        redDot.visibility = View.VISIBLE
        animator?.target = redDot
        animator?.start()
    }

    private fun enableRecord() {
        mRecord.apply {
            background = ContextCompat.getDrawable(
                this@MicActivity,
                R.drawable.audio_record_button_background
            )
            setImageResource(R.drawable.ic_mic_white)
            contentDescription = getString(R.string.action_record)
        }
        redDot.visibility = View.GONE
        animator?.end()
    }

    private fun disablePlay() {
        disableButton(mPlay)
    }

    private fun enablePlay() {
        enableButton(mPlay)
    }

    private fun disablePause() {
        //mStop.isEnabled = false
        //mStop.visibility = View.INVISIBLE
        disableButton(mPause)
    }

    private fun enablePause() {
        //mStop.isEnabled = true
        //mStop.visibility = View.VISIBLE
        enableButton(mPause)
    }

    private fun enableButton(button: ImageButton) {
        button.isEnabled = true
        button.alpha = 1f
    }

    private fun disableButton(button: ImageButton) {
        button.isEnabled = false
        button.alpha = .2f
    }

    private fun openRecordings() {
        // bundle.putString(VAULT_FILTER, FilterType.AUDIO.name)
        // nav().navigate(R.id.action_micScreen_to_attachments_screen, bundle)
    }

    private fun stopRecorder() {
        viewModel.stopRecorder()
    }

    private fun pauseRecorder() {
        viewModel.pauseRecorder()
    }

    private fun cancelPauseRecorder() {
        viewModel.cancelPauseRecorder()
    }

    private fun cancelRecorder() {
        viewModel.cancelRecorder()
    }

    private fun stopPresenter() {
        viewModel.stopRecorder()
    }

    private fun timeToString(duration: Long): String {
        return String.format(
            Locale.ROOT, TIME_FORMAT,
            TimeUnit.MILLISECONDS.toMinutes(duration),
            TimeUnit.MILLISECONDS.toSeconds(duration) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        )
    }

    private fun updateStorageSpaceLeft(memoryLeft: Long) {
        val timeMinutes = memoryLeft / 262144.0 // 4 minutes --> 1MB approximation/1024*256
        // todo: move this (262144.0) number to recorder to provide
        val days = (timeMinutes / 1440).toInt()
        val hours = ((timeMinutes - days * 1440) / 60).toInt()
        val minutes = (timeMinutes - days * 1440 - hours * 60).toInt()
        val spaceLeft = StringUtils.getFileSize(memoryLeft)
        if (days < 1 && hours < 12) {
            freeSpace.text =
                getString(R.string.recorder_meta_space_available_hours, hours, minutes, spaceLeft)
        } else {
            freeSpace.text =
                getString(R.string.recorder_meta_space_available_days, days, hours, spaceLeft)
        }
    }

    private fun updateRecordingName(name: String) {
        currentRecordName = name
        viewModel.checkFileName(fileName = name)
    }

    @SuppressLint("SetTextI18n")
    private fun updateRecordingName() {
        recordingName.text = UUID.randomUUID().toString() + ".aac"
    }

}