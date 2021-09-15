package rs.readahead.washington.mobile.views.fragment

import android.Manifest
import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.mvp.contract.IAudioCapturePresenterContract
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract
import rs.readahead.washington.mobile.mvp.presenter.AudioCapturePresenter
import rs.readahead.washington.mobile.mvp.presenter.MetadataAttacher
import rs.readahead.washington.mobile.util.C.RECORD_REQUEST_CODE
import rs.readahead.washington.mobile.util.DateUtil.getDateTimeString
import rs.readahead.washington.mobile.util.StringUtils
import rs.readahead.washington.mobile.views.base_ui.MetadataBaseLockFragment
import rs.readahead.washington.mobile.views.fragment.vault.home.VAULT_FILTER

import java.util.*
import java.util.concurrent.TimeUnit

const val TIME_FORMAT: String = "%02d:%02d"
const val UPDATE_SPACE_TIME_MS: Long = 60000

@Suppress("SameParameterValue")
class MicFragment : MetadataBaseLockFragment(),
    IAudioCapturePresenterContract.IView,
    IMetadataAttachPresenterContract.IView {
    //var RECORDER_MODE = "rm"

    private var animator: ObjectAnimator? = null

    private var notRecording = false

    private var lastUpdateTime: Long = 0

    // handling MediaFile
    private var handlingMediaFile: VaultFile? = null

    // recording
    private val presenter by lazy { AudioCapturePresenter(this) }

    private val bundle by lazy { Bundle() }

    private lateinit var metadataAttacher: MetadataAttacher
    private lateinit var mRecord: ImageButton
    private lateinit var mPlay: ImageButton
    private lateinit var mStop: ImageButton
    private lateinit var mTimer: TextView
    private lateinit var freeSpace: TextView
    private lateinit var redDot: ImageView
    private lateinit var recordingName: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mic, container, false)
        initView(view)
        return view
    }

    fun initView(view: View) {
        mRecord = view.findViewById(R.id.record_audio)
        mPlay = view.findViewById(R.id.play_audio)
        mStop = view.findViewById(R.id.stop_audio)
        mTimer = view.findViewById(R.id.audio_time)
        freeSpace = view.findViewById(R.id.free_space)
        redDot = view.findViewById(R.id.red_dot)
        recordingName = view.findViewById(R.id.rec_name)
        mRecord.setOnClickListener {
            if (notRecording) {
                if (hastRecordingPermissions(requireContext())) {
                    handleRecord()
                } else {
                    requestRecordingPermissions(RECORD_REQUEST_CODE)
                }
            } else {
                handlePause()
            }
        }

        recordingName.text = getString(R.string.mic_recording).plus(" ").plus(getDateTimeString())
        recordingName.setOnClickListener {
            BottomSheetUtils.showFileRenameSheet(
                activity.supportFragmentManager,
                getString(R.string.mic_rename_recording),
                getString(R.string.action_cancel),
                getString(R.string.action_ok),
                requireActivity(),
                recordingName.text.toString()
            ) { it1 -> updateRecordingName(it1) }
        }

        mStop.setOnClickListener { handleStop() }

        mPlay.setOnClickListener { openRecordings() }

        metadataAttacher = MetadataAttacher(this)

        notRecording = true

        animator = AnimatorInflater.loadAnimator(
            activity,
            R.animator.fade_in
        ) as ObjectAnimator

        mTimer.text = timeToString(0)
        disableStop()
    }

    override fun onStart() {
        super.onStart()
        activity.startLocationMetadataListening()
    }

    override fun onStop() {
        activity.stopLocationMetadataListening()

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
        presenter.checkAvailableStorage()
    }


    private fun hastRecordingPermissions(context: Context): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
            return true
        return false
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
        if (presenter.isAudioRecorder) {   //first start or restart
            disablePlay()
            handlingMediaFile = null
            cancelRecorder()
            presenter.startRecording(recordingName.text.toString())
        } else {
            cancelPauseRecorder()
        }
        disableRecord()
        enableStop()
    }

    override fun onDurationUpdate(duration: Long) {
        activity.runOnUiThread { mTimer.text = timeToString(duration) }

        if (duration > UPDATE_SPACE_TIME_MS + lastUpdateTime) {
            lastUpdateTime += UPDATE_SPACE_TIME_MS
            presenter.checkAvailableStorage()
        }
    }

    override fun onAddingStart() {
    }

    override fun onAddingEnd() {
    }

    override fun onAddSuccess(vaultFile: VaultFile) {
        activity.attachMediaFileMetadata(vaultFile, metadataAttacher)
        showToast(
            String.format(
                getString(R.string.recorder_toast_recording_saved),
                getString(R.string.app_name)
            )
        )
    }

    override fun onAddError(error: Throwable?) {
        showToast(getString(R.string.gallery_toast_fail_saving_file))
    }

    override fun onAvailableStorage(memory: Long) {
        updateStorageSpaceLeft(memory)
    }

    override fun onAvailableStorageFailed(throwable: Throwable?) {
    }

    override fun onMetadataAttached(vaultFile: VaultFile?) {
        /*val intent = Intent()

        if (mode == AudioRecordActivity2.Mode.COLLECT) {
            intent.putExtra(QuestionAttachmentActivity.MEDIA_FILE_KEY, handlingMediaFile)
        } else {
            intent.putExtra(C.CAPTURED_MEDIA_FILE_ID, vaultFile!!.id)
        }*/

        //setResult(Activity.RESULT_OK, intent)
        mTimer.text = timeToString(0)

        //scheduleFileUpload(handlingMediaFile)
    }

    override fun onMetadataAttachError(throwable: Throwable?) {
        showToast(getString(R.string.gallery_toast_fail_saving_file))
    }
    //    private void returnData() {
    //        if (handlingMediaFile != null) {
    //            presenter.addMediaFile(handlingMediaFile);
    //        }
    //    }

    private fun handleStop() {
        notRecording = true
        stopRecorder()
    }

    private fun handlePause() {
        pauseRecorder()
        enableRecord()
        notRecording = true
    }

    override fun onRecordingStopped(vaultFile: VaultFile?) {
        if (vaultFile == null) {
            handlingMediaFile = null
            disableStop()
            disablePlay()
            enableRecord()
        } else {
            handlingMediaFile = vaultFile
            handlingMediaFile!!.size = MediaFileHandler.getSize(vaultFile)
            disableStop()
            enablePlay()
            enableRecord()

            // returnData();
            onAddSuccess(vaultFile)
        }
    }

    override fun onRecordingError() {
        handlingMediaFile = null
        disableStop()
        disablePlay()
        enableRecord()
        mTimer.text = timeToString(0)
        showToast(getString(R.string.recorder_toast_fail_recording))
    }

    private fun disableRecord() {
        mRecord.background =
            AppCompatResources.getDrawable(
                requireContext(),
                R.drawable.light_purple_circle_background
            )
        mRecord.setImageResource(R.drawable.ic__pause__white_24)
        redDot.visibility = View.VISIBLE
        animator?.target = redDot
        animator?.start()
    }

    private fun enableRecord() {
        mRecord.background =
            AppCompatResources.getDrawable(
                requireContext(),
                R.drawable.audio_record_button_background
            )
        mRecord.setImageResource(R.drawable.ic_mic_white)
        redDot.visibility = View.GONE
        animator?.end()
    }

    private fun disablePlay() {
        disableButton(mPlay)
    }

    private fun enablePlay() {
        enableButton(mPlay)
    }

    private fun disableStop() {
        mStop.isEnabled = false
        mStop.visibility = View.INVISIBLE
        //disableButton(mStop)
    }

    private fun enableStop() {
        mStop.isEnabled = true
        mStop.visibility = View.VISIBLE
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
        presenter.stopRecorder()
    }

    private fun pauseRecorder() {
        presenter.pauseRecorder()
    }

    private fun cancelPauseRecorder() {
        presenter.cancelPauseRecorder()
    }

    private fun cancelRecorder() {
        presenter.cancelRecorder()
    }

    private fun stopPresenter() {
        presenter.stopRecorder()
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
}