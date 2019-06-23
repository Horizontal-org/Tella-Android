package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.MediaFileDeletedEvent;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.AudioPlayer;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IAudioPlayPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.IMediaFileViewerPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.AudioPlayPresenter;
import rs.readahead.washington.mobile.mvp.presenter.MediaFileViewerPresenter;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.util.ThreadUtil;
import timber.log.Timber;

@RuntimePermissions
public class AudioPlayActivity extends CacheWordSubscriberBaseActivity implements
        IAudioPlayPresenterContract.IView,
        IMediaFileViewerPresenterContract.IView {
    public static final String PLAY_MEDIA_FILE = "pmf";
    public static final String PLAY_MEDIA_FILE_ID_KEY = "pmfik";
    public static final String NO_ACTIONS = "na";

    private static final String TIME_FORMAT = "%02d : %02d";

    @BindView(R.id.play_audio)
    ImageButton mPlay;
    @BindView(R.id.stop_audio)
    ImageButton mStop;
    @BindView(R.id.audio_time)
    TextView mTimer;

    private AudioPlayPresenter presenter;
    private MediaFile handlingMediaFile;
    private AudioPlayer audioPlayer;
    private AudioPlayer.Listener audioPlayerListener;

    private MediaFileViewerPresenter viewerPresenter;

    private boolean showActions = false;
    private boolean actionsDisabled = false;
    private AlertDialog alertDialog;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_audio_play);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        viewerPresenter = new MediaFileViewerPresenter(this);
        disableStop();
        disablePlay();

        if (getIntent().hasExtra(NO_ACTIONS)) {
            actionsDisabled = true;
        }

        audioPlayerListener = new AudioPlayer.Listener() {
            private int duration;

            @Override
            public void onStart(int duration) {
                this.duration = duration;
            }

            @Override
            public void onStop() {
                stopPlayer();
            }

            @Override
            public void onProgress(int currentPosition) {
                showTimeRemaining(duration - currentPosition);
            }

            private void showTimeRemaining(int left) {
                mTimer.setText(String.format(Locale.ROOT, TIME_FORMAT, TimeUnit.MILLISECONDS.toMinutes(left),
                        TimeUnit.MILLISECONDS.toSeconds(left) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(left))));
            }
        };

        if (getIntent().hasExtra(PLAY_MEDIA_FILE)) {
            MediaFile mediaFile = (MediaFile) getIntent().getSerializableExtra(PLAY_MEDIA_FILE);
            if (mediaFile != null) {
                ThreadUtil.runOnMain(() -> onMediaFileSuccess(mediaFile));
            }
        } else if (getIntent().hasExtra(PLAY_MEDIA_FILE_ID_KEY)) {
            long id = getIntent().getLongExtra(PLAY_MEDIA_FILE_ID_KEY, 0);
            if (id != 0) {
                presenter = new AudioPlayPresenter(this);
                presenter.getMediaFile(id);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!actionsDisabled && showActions) {
            getMenuInflater().inflate(R.menu.audio_view_menu, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.menu_item_share) {
            if (handlingMediaFile != null) {
                MediaFileHandler.startShareActivity(this, handlingMediaFile);
            }
            return true;
        }

        if (id == R.id.menu_item_export) {
            showExportDialog();
            return true;
        }

        if (id == R.id.menu_item_delete) {
            showDeleteMediaDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick({R.id.play_audio, R.id.stop_audio})
    public void manageClick(View view) {
        switch (view.getId()) {
            case R.id.play_audio:
                handlePlay();
                break;
            case R.id.stop_audio:
                handleStop();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopPlayer();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPlayer();
    }

    @Override
    protected void onDestroy() {
        audioPlayerListener = null;

        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }

        hideProgressDialog();

        if (presenter != null) {
            presenter.destroy();
        }

        if (viewerPresenter != null) {
            viewerPresenter.destroy();
            viewerPresenter = null;
        }

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AudioPlayActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onWriteExternalStoragePermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onWriteExternalStorageNeverAskAgain() {
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void exportMediaFile() {
        if (handlingMediaFile != null && viewerPresenter != null) {
            viewerPresenter.exportNewMediaFile(handlingMediaFile);
        }
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showWriteExternalStorageRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.ra_media_export_rationale));
    }

    @Override
    public void onMediaFileSuccess(MediaFile mediaFile) {
        handlingMediaFile = mediaFile;
        handlePlay();

        if (!actionsDisabled) {
            showActions = true;
            invalidateOptionsMenu();
        }
    }

    @Override
    public void onMediaFileError(Throwable error) {
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onMediaExported() {
        showToast(R.string.ra_media_exported);
    }

    @Override
    public void onExportError(Throwable error) {
        showToast(R.string.ra_media_export_error);
    }

    @Override
    public void onExportStarted() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.ra_export_media_progress));
    }

    @Override
    public void onExportEnded() {
        hideProgressDialog();
    }

    @Override
    public void onMediaFileDeleted() {
        MyApplication.bus().post(new MediaFileDeletedEvent());
        finish();
    }

    @Override
    public void onMediaFileDeletionError(Throwable throwable) {
        showToast(R.string.ra_media_deleted_error);
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void showExportDialog() {
        alertDialog = DialogsUtil.showExportMediaDialog(this, (dialog, which) ->
                AudioPlayActivityPermissionsDispatcher.exportMediaFileWithCheck(AudioPlayActivity.this));
    }

    private void showDeleteMediaDialog() {
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.ra_delete_media)
                .setMessage(R.string.ra_media_will_be_deleted)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (viewerPresenter != null && handlingMediaFile != null) {
                        viewerPresenter.deleteMediaFiles(handlingMediaFile);
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {})
                .setCancelable(true)
                .show();
    }

    private void handleStop() {
        stopPlayer();
    }

    private void handlePlay() {
        if (handlingMediaFile == null) {
            return;
        }

        enableStop();
        disablePlay();

        audioPlayer = new AudioPlayer(this, audioPlayerListener);
        audioPlayer.play(handlingMediaFile);
    }

    private void onPlayerStop() {
        disableStop();
        enablePlay();
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

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
}
