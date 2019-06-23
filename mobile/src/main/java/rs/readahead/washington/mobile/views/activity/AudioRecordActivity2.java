package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.CompositeDisposable;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.media.AudioPlayer;
import rs.readahead.washington.mobile.media.AudioRecorder;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IAudioCapturePresenterContract;
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.AudioCapturePresenter;
import rs.readahead.washington.mobile.mvp.presenter.MetadataAttacher;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.PermissionUtil;


@RuntimePermissions
public class AudioRecordActivity2 extends MetadataActivity implements
        IAudioCapturePresenterContract.IView,
        IMetadataAttachPresenterContract.IView {
    private static final String TIME_FORMAT = "%02d : %02d";
    public static String RECORDER_MODE = "rm";

    @BindView(R.id.record_audio)
    ImageButton mRecord;
    @BindView(R.id.play_audio)
    ImageButton mPlay;
    @BindView(R.id.stop_audio)
    ImageButton mStop;
    @BindView(R.id.evidence)
    AppCompatButton mEvidence;
    @BindView(R.id.audio_time)
    TextView mTimer;
    @BindView(R.id.audio_seek_bar)
    SeekBar mSeekBar;
    @BindView(R.id.recording_progress)
    ProgressBar mProgressBar;
    @BindView(R.id.recording_info)
    TextView mInfo;

    private Handler durationHandler;
    private long startTime = 0L;

    // handling MediaFile
    private MediaFile handlingMediaFile;

    // recording
    private AudioRecorder audioRecorder;
    private AudioCapturePresenter presenter;
    private MetadataAttacher metadataAttacher;
    private CompositeDisposable disposable = new CompositeDisposable();
    private AlertDialog rationaleDialog;

    // playing
    private AudioPlayer audioPlayer;
    private AudioPlayer.Listener audioPlayerListener;

    public enum Mode {
        COLLECT, // todo: mode is return/stand, add another one for view msgs settings
        RETURN,
        STAND
    }

    private Mode mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_audio_record);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.title_activity_audio_record);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        presenter = new AudioCapturePresenter(this);
        metadataAttacher = new MetadataAttacher(this);

        mode = Mode.STAND;
        if (getIntent().hasExtra(RECORDER_MODE)) {
            mode = Mode.valueOf(getIntent().getStringExtra(RECORDER_MODE));
        }

        if (mode == Mode.COLLECT) {
            mEvidence.setText(R.string.attach_audio_to_the_form);
        }

        disableStop();
        disablePlay();

        durationHandler = new Handler();

        audioPlayerListener = new AudioPlayer.Listener() {
            private int duration;

            @Override
            public void onStart(int duration) {
                this.duration = duration;
                //mSeekBar.setMax(duration);
                //mSeekBar.setClickable(false);
                mProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStop() {
                stopPlayer();
            }

            @Override
            public void onProgress(int currentPosition) {
                int timeRemaining = duration - currentPosition;

                mTimer.setText(String.format(Locale.ROOT, TIME_FORMAT, TimeUnit.MILLISECONDS.toMinutes(timeRemaining),
                        TimeUnit.MILLISECONDS.toSeconds(timeRemaining) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeRemaining))));
                //mSeekBar.setProgress(currentPosition);
                //todo: show play progress on mProgressBar
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick({R.id.record_audio, R.id.play_audio, R.id.stop_audio, R.id.evidence})
    public void manageClick(View view) {
        switch (view.getId()) {
            case R.id.record_audio:
                AudioRecordActivity2PermissionsDispatcher.handleRecordWithCheck(this);
                break;
            case R.id.play_audio:
                handlePlay();
                break;
            case R.id.stop_audio:
                handleStop();
                break;
            case R.id.evidence:
                returnData();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        stopPlayer();

        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startLocationMetadataListening();
    }

    @Override
    protected void onStop() {
        stopLocationMetadataListening();

        if (rationaleDialog != null && rationaleDialog.isShowing()) {
            rationaleDialog.dismiss();
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        disposable.dispose();
        cancelRecorder();
        stopPlayer();
        audioPlayerListener = null;
        stopPresenter();

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AudioRecordActivity2PermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnPermissionDenied(Manifest.permission.RECORD_AUDIO)
    void onRecordAudioPermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.RECORD_AUDIO)
    void onRecordAudioNeverAskAgain() {
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    void handleRecord() {
        mInfo.setText(getString(R.string.recording));

        mProgressBar.setVisibility(View.VISIBLE);
        //mSeekBar.setVisibility(View.GONE);
        mEvidence.setVisibility(View.GONE);

        disableRecord();
        enableStop();
        disablePlay();

        handlingMediaFile = null;

        cancelRecorder();

        audioRecorder = new AudioRecorder(this);
        disposable.add(audioRecorder.startRecording()
                .subscribe(this::onRecordingStopped, throwable -> onRecordingError())
        );

        startTime = SystemClock.uptimeMillis();
        durationHandler.postDelayed(updateProgressBarTime, 0);
        //showToast(R.string.recording_started);
    }

    @OnShowRationale(Manifest.permission.RECORD_AUDIO)
    void showRecordAudioRationale(final PermissionRequest request) {
        rationaleDialog = PermissionUtil.showRationale(this, request, getString(R.string.permission_audio));
    }

    @Override
    public void onAddingStart() {
    }

    @Override
    public void onAddingEnd() {
    }

    @Override
    public void onAddSuccess(long mediaFileId) {
        attachMediaFileMetadata(mediaFileId, metadataAttacher);
        showToast(String.format(getString(R.string.recorded_successfully), getString(R.string.app_name)));
    }

    @Override
    public void onAddError(Throwable error) {
        showToast(R.string.ra_capture_error);
    }

    @Override
    public void onMetadataAttached(long mediaFileId, @Nullable Metadata metadata) {
        Intent intent = new Intent();

        if (mode == Mode.COLLECT) {
            intent.putExtra(QuestionAttachmentActivity.MEDIA_FILE_KEY, handlingMediaFile);
        } else {
            intent.putExtra(C.CAPTURED_MEDIA_FILE_ID, mediaFileId);
        }

        setResult(Activity.RESULT_OK, intent);
        disablePlay();
        mEvidence.setVisibility(View.GONE);
        mTimer.setText(String.format(Locale.ROOT, TIME_FORMAT, 0, 0));

        if (mode != Mode.STAND) {
            finish();
        }
    }

    @Override
    public void onMetadataAttachError(Throwable throwable) {
        showToast(R.string.ra_capture_error);
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void handleStop() {
        stopRecorder();
        stopPlayer();
    }

    private void handlePlay() {
        if (handlingMediaFile == null) {
            return;
        }

        mProgressBar.setVisibility(View.GONE);
        //mSeekBar.setVisibility(View.VISIBLE);
        mInfo.setText(getString(R.string.recording_play));
        mEvidence.setEnabled(false);

        disableRecord();
        enableStop();
        disablePlay();

        audioPlayer = new AudioPlayer(this, audioPlayerListener);
        audioPlayer.play(handlingMediaFile);
    }

    private Runnable updateProgressBarTime = new Runnable() {
        public void run() {
            long elapsed = SystemClock.uptimeMillis() - startTime;
            mTimer.setText(String.format(Locale.ROOT, TIME_FORMAT, TimeUnit.MILLISECONDS.toMinutes(elapsed),
                    TimeUnit.MILLISECONDS.toSeconds(elapsed) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed))));
            durationHandler.postDelayed(this, 0);
        }
    };

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void onRecordingStopped(MediaFile mediaFile) {
        if (MediaFile.NONE.equals(mediaFile)) {
            handlingMediaFile = null;

            disableStop();
            disablePlay();
            enableRecord();

            mEvidence.setVisibility(View.GONE);
        } else {
            handlingMediaFile = mediaFile;
            handlingMediaFile.setSize(MediaFileHandler.getSize(getContext(), mediaFile));

            disableStop();
            enablePlay();
            enableRecord();

            mInfo.setText("");
            mEvidence.setVisibility(View.VISIBLE);

            //showToast(R.string.recorded_successfully);
        }

        durationHandler.removeCallbacks(updateProgressBarTime);
        mProgressBar.setVisibility(View.GONE);
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void onRecordingError() {
        handlingMediaFile = null;

        disableStop();
        disablePlay();
        enableRecord();

        durationHandler.removeCallbacks(updateProgressBarTime);
        mProgressBar.setVisibility(View.GONE);
        mTimer.setText(String.format(Locale.ROOT, TIME_FORMAT, 0, 0));
        showToast(R.string.recorded_unsuccessfully);
    }

    private void onPlayerStop() {
        mEvidence.setEnabled(true);
        //mSeekBar.setProgress(duration);
        mProgressBar.setVisibility(View.GONE);
        mInfo.setText("");
        enableRecord();
        disableStop();
        enablePlay();
    }

    private void returnData() {
        if (handlingMediaFile != null) {
            presenter.addMediaFile(handlingMediaFile);
        }
    }

    private void disableRecord() {
        disableButton(mRecord);
    }

    private void enableRecord() {
        enableButton(mRecord);
    }

    private void disablePlay() {
        disableButton(mPlay);
    }

    private void enablePlay() {
        enableButton(mPlay);
    }

    private void disableStop() {
        disableButton(mStop);
    }

    private void enableStop() {
        enableButton(mStop);
    }

    private void enableButton(ImageButton button) {
        button.setEnabled(true);
        button.setAlpha(1f);
    }

    private void disableButton(ImageButton button) {
        button.setEnabled(false);
        button.setAlpha(.3f);
    }

    private void stopPlayer() {
        if (audioPlayer != null) {
            audioPlayer.stop();
            audioPlayer = null;
            onPlayerStop();
        }
    }

    private void stopRecorder() {
        if (audioRecorder != null) {
            audioRecorder.stopRecording();
            audioRecorder = null;
        }
    }

    private void cancelRecorder() {
        if (audioRecorder != null) {
            audioRecorder.cancelRecording();
            audioRecorder = null;
        }
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }
}
