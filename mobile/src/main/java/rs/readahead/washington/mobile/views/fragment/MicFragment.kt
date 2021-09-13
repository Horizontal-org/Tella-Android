package rs.readahead.washington.mobile.views.fragment

import android.Manifest
import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import io.reactivex.disposables.CompositeDisposable
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.OnPermissionDenied
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.media.AudioRecorder
import rs.readahead.washington.mobile.media.AudioRecorder.AudioRecordInterface
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.mvp.contract.IAudioCapturePresenterContract
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadSchedulePresenterContract
import rs.readahead.washington.mobile.mvp.presenter.AudioCapturePresenter
import rs.readahead.washington.mobile.mvp.presenter.MetadataAttacher
import rs.readahead.washington.mobile.mvp.presenter.TellaFileUploadSchedulePresenter
import rs.readahead.washington.mobile.util.DateUtil.getDateTimeString
import rs.readahead.washington.mobile.util.StringUtils
import rs.readahead.washington.mobile.views.activity.GalleryActivity
import rs.readahead.washington.mobile.views.base_ui.MetadataBaseLockFragment
import timber.log.Timber

import java.util.*
import java.util.concurrent.TimeUnit

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MicFragment : MetadataBaseLockFragment(), AudioRecordInterface,
    IAudioCapturePresenterContract.IView,
    ITellaFileUploadSchedulePresenterContract.IView,
    IMetadataAttachPresenterContract.IView {

    // TODO: Rename and change types of parameters
    private val TIME_FORMAT: String = "%02d:%02d"
    var RECORDER_MODE = "rm"

    private var animator: ObjectAnimator? = null

    private var notRecording = false

    private val UPDATE_SPACE_TIME_MS: Long = 60000
    private var lastUpdateTime: Long = 0

    // handling MediaFile
    private var handlingMediaFile: VaultFile? = null

    // recording
    private var uploadPresenter: TellaFileUploadSchedulePresenter? = null
    private val disposable = CompositeDisposable()
    private val rationaleDialog: AlertDialog? = null
    var presenter: AudioCapturePresenter? = null
    var audioRecorder: AudioRecorder? = null

    lateinit var metadataAttacher: MetadataAttacher
    lateinit var mRecord: ImageButton
    lateinit var mPlay: ImageButton
    lateinit var mStop: ImageButton
    lateinit var mTimer: TextView
    lateinit var freeSpace: TextView
    lateinit var redDot: ImageView
    lateinit var recordingName: TextView

    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

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
                handleRecord()
            } else {
                handlePause()
            }
        }

        recordingName.setText(getString(R.string.mic_recording).plus(" ").plus(getDateTimeString()))
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

        presenter = AudioCapturePresenter(this)
        uploadPresenter = TellaFileUploadSchedulePresenter(this)
        metadataAttacher = MetadataAttacher(this)

        notRecording = true

        animator = AnimatorInflater.loadAnimator(
            activity,
            R.animator.fade_in
        ) as ObjectAnimator

        mTimer.text = timeToString(0)
        disableStop()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MicFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MicFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onStart() {
        super.onStart()
        activity.startLocationMetadataListening()
    }

    override fun onStop() {
        activity.stopLocationMetadataListening()
        if (rationaleDialog != null && rationaleDialog.isShowing) {
            rationaleDialog.dismiss()
        }
        super.onStop()
    }

    override fun onDestroy() {
        animator!!.end()
        animator = null
        disposable.dispose()
        cancelRecorder()
        stopPresenter()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (presenter != null) {
            presenter!!.checkAvailableStorage()
        }
    }

    /*override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        MicFragmentPermissionsDispatcher.onRequestPermissionsResult(
            this,
            requestCode,
            grantResults
        )
    }*/

    @OnPermissionDenied(Manifest.permission.RECORD_AUDIO)
    fun onRecordAudioPermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.RECORD_AUDIO)
    fun onRecordAudioNeverAskAgain() {
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    fun handleRecord() {
        notRecording = false
        if (audioRecorder == null) {   //first start or restart
            disablePlay()
            handlingMediaFile = null
            cancelRecorder()
            audioRecorder = AudioRecorder(this)
            disposable.add(
                audioRecorder!!.startRecording(recordingName.text.toString())
                    .subscribe(
                        { vaultFile: VaultFile? ->
                            onRecordingStopped(
                                vaultFile
                            )
                        }
                    ) { throwable: Throwable? ->
                        Timber.d(throwable)
                        onRecordingError()
                    }
            )
        } else {
            cancelPauseRecorder()
        }
        disableRecord()
        enableStop()
    }

    override fun onDurationUpdate(duration: Long) {
        activity.runOnUiThread(Runnable { mTimer.setText(timeToString(duration)) })

        if (duration > UPDATE_SPACE_TIME_MS + lastUpdateTime) {
            lastUpdateTime += UPDATE_SPACE_TIME_MS
            presenter?.checkAvailableStorage()
        }
    }

    override fun onAddingStart() {
    }

    override fun onAddingEnd() {
    }

    override fun onAddSuccess(vaultFile: VaultFile) {
        activity.attachMediaFileMetadata(vaultFile, metadataAttacher)
        activity.showToast(
            String.format(
                getString(R.string.recorder_toast_recording_saved),
                getString(R.string.app_name)
            )
        )
    }

    override fun onAddError(error: Throwable?) {
        activity.showToast(R.string.gallery_toast_fail_saving_file)
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
        activity.showToast(R.string.gallery_toast_fail_saving_file)
    }

    override fun onMediaFilesUploadScheduled() {
    }

    override fun onMediaFilesUploadScheduleError(throwable: Throwable?) {
    }


    override fun onGetMediaFilesSuccess(mediaFiles: MutableList<VaultFile>?) {
    }

    override fun onGetMediaFilesError(error: Throwable?) {
    }

    //    private void returnData() {
    //        if (handlingMediaFile != null) {
    //            presenter.addMediaFile(handlingMediaFile);
    //        }
    //    }
    private fun openRecordings() {
        val intent: Intent = Intent(activity, GalleryActivity::class.java)
        intent.putExtra(GalleryActivity.GALLERY_FILTER, FilterType.AUDIO.name)
        intent.putExtra(GalleryActivity.GALLERY_ALLOWS_ADDING, false)
        startActivity(intent)
    }

    private fun handleStop() {
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

    private fun onRecordingError() {
        handlingMediaFile = null
        disableStop()
        disablePlay()
        enableRecord()
        mTimer.text = timeToString(0)
        activity.showToast(R.string.recorder_toast_fail_recording)
    }

    private fun disableRecord() {
        mRecord.background =
            AppCompatResources.getDrawable(requireContext(),R.drawable.light_purple_circle_background)
        mRecord.setImageResource(R.drawable.ic__pause__white_24)
        redDot.visibility = View.VISIBLE
        animator!!.target = redDot
        animator!!.start()
    }

    private fun enableRecord() {
        mRecord.background =
            AppCompatResources.getDrawable(requireContext(),R.drawable.audio_record_button_background)
        mRecord.setImageResource(R.drawable.ic_mic_white)
        redDot.visibility = View.GONE
        animator!!.end()
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

    /* private fun openRecordings() {
         val intent: Intent = Intent(this, GalleryActivity::class.java)
         intent.putExtra(GalleryActivity.GALLERY_FILTER, FilterType.AUDIO.name)
         intent.putExtra(GalleryActivity.GALLERY_ALLOWS_ADDING, false)
         startActivity(intent)
     }*/

    private fun stopRecorder() {
        if (audioRecorder != null) {
            audioRecorder!!.stopRecording()
            audioRecorder = null
        }
    }

    private fun pauseRecorder() {
        if (audioRecorder != null) {
            audioRecorder!!.pauseRecording()
        }
    }

    private fun cancelPauseRecorder() {
        if (audioRecorder != null) {
            audioRecorder!!.cancelPause()
        }
    }

    private fun cancelRecorder() {
        if (audioRecorder != null) {
            audioRecorder!!.cancelRecording()
            audioRecorder = null
        }
    }

    private fun stopPresenter() {
        if (presenter != null) {
            presenter!!.destroy()
            presenter = null
        }
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
        recordingName.setText(name)
    }
}