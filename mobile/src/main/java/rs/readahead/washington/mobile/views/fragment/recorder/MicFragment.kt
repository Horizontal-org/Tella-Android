package rs.readahead.washington.mobile.views.fragment.recorder

import android.Manifest
import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract
import rs.readahead.washington.mobile.mvp.presenter.MetadataAttacher
import rs.readahead.washington.mobile.util.C.RECORD_REQUEST_CODE
import rs.readahead.washington.mobile.util.StringUtils
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.activity.camera.CameraActivity.Companion.VAULT_CURRENT_ROOT_PARENT
import rs.readahead.washington.mobile.views.base_ui.MetadataBaseLockFragment
import rs.readahead.washington.mobile.views.fragment.reports.entry.BUNDLE_REPORT_AUDIO
import rs.readahead.washington.mobile.views.fragment.reports.entry.BUNDLE_REPORT_VAULT_FILE
import rs.readahead.washington.mobile.views.fragment.vault.home.VAULT_FILTER
import rs.readahead.washington.mobile.views.interfaces.ICollectEntryInterface
import rs.readahead.washington.mobile.views.interfaces.IMainNavigationInterface
import rs.readahead.washington.mobile.views.interfaces.VerificationWorkStatusCallback
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit


const val TIME_FORMAT: String = "%02d:%02d"
const val COLLECT_ENTRY = "collect_entry"
const val REPORT_ENTRY = "report_entry"
private const val UPDATE_SPACE_TIME_MS: Long = 60000

@AndroidEntryPoint
class MicFragment : MetadataBaseLockFragment(),
    IMetadataAttachPresenterContract.IView {

    private var animator: ObjectAnimator? = null
    private var isCollect: Boolean = false
    private var isReport: Boolean = false
    private var notRecording = false
    private var lastUpdateTime: Long = 0
    private var isAddingInProgress = false

    // handling MediaFile
    private var handlingMediaFile: VaultFile? = null

    private val viewModel by viewModels<AudioCaptureViewModel>()

    private val bundle by lazy { Bundle() }

    private var callback: VerificationWorkStatusCallback? = null

    companion object {
        @JvmStatic
        fun newInstance(value: Boolean) =
            MicFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(COLLECT_ENTRY, value)
                    putBoolean(REPORT_ENTRY, value)
                }
            }
    }

    private lateinit var metadataAttacher: MetadataAttacher
    private lateinit var mRecord: ImageButton
    private lateinit var mPlay: ImageButton
    private lateinit var mPause: ImageButton
    private lateinit var mTimer: TextView
    private lateinit var freeSpace: TextView
    private lateinit var redDot: ImageView
    private lateinit var recordingName: TextView
    private var currentRootParent: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mic, container, false)
        initView(view)
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isCollect = it.getBoolean(COLLECT_ENTRY)
            isReport = it.getBoolean(REPORT_ENTRY)
        }
    }

    fun initView(view: View) {
        mRecord = view.findViewById(R.id.record_audio)
        mPlay = view.findViewById(R.id.play_audio)
        mPause = view.findViewById(R.id.stop_audio)
        mTimer = view.findViewById(R.id.audio_time)
        freeSpace = view.findViewById(R.id.free_space)
        redDot = view.findViewById(R.id.red_dot)
        recordingName = view.findViewById(R.id.rec_name)

        if (isCollect || isReport) {
            mPlay.visibility = View.GONE
            val activity = context as IMainNavigationInterface
            activity.hideBottomNavigation()
        }

        mRecord.setOnClickListener {
            if (notRecording) {
                if (hastRecordingPermissions(requireContext())) {
                    handleRecord()
                } else {
                    metadataActivity.maybeChangeTemporaryTimeout()
                    requestRecordingPermissions(RECORD_REQUEST_CODE)
                }
            } else {
                handleStop()
            }
        }

        updateRecordingName()
        recordingName.setOnClickListener {
            BottomSheetUtils.showFileRenameSheet(
                metadataActivity.supportFragmentManager,
                getString(R.string.mic_rename_recording),
                getString(R.string.action_cancel),
                getString(R.string.action_ok),
                requireActivity(),
                recordingName.text.toString()
            ) { it1 -> updateRecordingName(it1) }
        }

        mPause.setOnClickListener { handlePause() }

        mPlay.setOnClickListener { openRecordings() }

        metadataAttacher = MetadataAttacher(this)

        notRecording = true

        animator = AnimatorInflater.loadAnimator(
            metadataActivity,
            R.animator.fade_in
        ) as ObjectAnimator

        mTimer.text = timeToString(0)
        disablePause()
        initData()
        initObservers()
    }

    private fun initObservers() {
        viewModel.durationLiveData.observe(viewLifecycleOwner, ::onDurationUpdate)
        viewModel.recordingStoppedLiveData.observe(viewLifecycleOwner, ::onRecordingStopped)
        viewModel.availableStorageLiveData.observe(viewLifecycleOwner, ::onAvailableStorage)
        viewModel.recordingErrorLiveData.observe(viewLifecycleOwner, ::onRecordingError)
        viewModel.mediaFilesUploadScheduleError.observe(
            viewLifecycleOwner,
            ::onMediaFilesUploadScheduleError
        )
        viewModel.addingInProgress.observe(viewLifecycleOwner) { isAdding ->
            callback?.setBackgroundWorkStatus(isAdding && !Preferences.isAnonymousMode())
        }
    }

    private fun handleBackStack() {
        (activity as MainActivity).onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!Preferences.isAnonymousMode() && isAddingInProgress) {
                        BottomSheetUtils.showConfirmSheet(fragmentManager = activity?.supportFragmentManager!!,
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is VerificationWorkStatusCallback) {
            callback = context
        } else {
            throw RuntimeException("$context must implement BackgroundWorkStatusCallback")
        }
    }

    override fun onStart() {
        super.onStart()
        metadataActivity.startLocationMetadataListening()
    }

    override fun onStop() {
        metadataActivity.stopLocationMetadataListening()
        super.onStop()
    }

    override fun onDestroyView() {
        if (isCollect) {
            val activity = context as ICollectEntryInterface
            activity.stopWaitingForData()
        }
        super.onDestroyView()
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
        if (!isCollect) {
            (metadataActivity as MainActivity).selectNavMic()
        }
        viewModel.checkAvailableStorage()
    }

    private fun hastRecordingPermissions(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

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
        metadataActivity.runOnUiThread { mTimer.text = timeToString(duration) }

        if (duration > UPDATE_SPACE_TIME_MS + lastUpdateTime) {
            lastUpdateTime += UPDATE_SPACE_TIME_MS
            viewModel.checkAvailableStorage()
        }
    }

    private fun onAddSuccess(vaultFile: VaultFile) {
        if (!isCollect) {
            DialogUtils.showBottomMessage(
                metadataActivity,
                String.format(
                    getString(R.string.recorder_toast_recording_saved),
                    getString(R.string.app_name)
                ), false
            )
        }

        if (!Preferences.isAnonymousMode()) {
            metadataActivity.attachMediaFileMetadata(vaultFile, metadataAttacher)
        } else {
            onMetadataAttached(vaultFile)
        }
        scheduleFileUpload(vaultFile)
    }

    private fun onAddError(error: Throwable?) {
        DialogUtils.showBottomMessage(
            metadataActivity,
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
        if (isCollect) {
            val activity = context as ICollectEntryInterface
            activity.returnFileToForm(vaultFile)
        }
        if (isReport) {
            val bundle = Bundle()
            bundle.putSerializable(BUNDLE_REPORT_VAULT_FILE, vaultFile)
            setFragmentResult(BUNDLE_REPORT_AUDIO, bundle)
            nav().navigateUp()
        }
    }

    override fun onMetadataAttachError(throwable: Throwable?) {
        DialogUtils.showBottomMessage(
            metadataActivity,
            getString(R.string.gallery_toast_fail_saving_file),
            true
        )
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

        DialogUtils.showBottomMessage(metadataActivity, message, false)
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
            metadataActivity,
            getString(R.string.recorder_toast_fail_recording),
            true
        )
    }

    private fun disableRecord() {
        mRecord.apply {
            background =
                AppCompatResources.getDrawable(requireContext(), R.drawable.red_circle_background)
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
                requireContext(),
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
        bundle.putString(VAULT_FILTER, FilterType.AUDIO.name)
        nav().navigate(R.id.action_micScreen_to_attachments_screen, bundle)
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
        recordingName.text = name
    }

    private fun initData() {
        arguments?.getString(VAULT_CURRENT_ROOT_PARENT)?.let {
            currentRootParent = it
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateRecordingName() {
        recordingName.text = UUID.randomUUID().toString() + ".aac"
    }
}