package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
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
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;
import rs.readahead.washington.mobile.views.fragment.ShareDialogFragment;
import timber.log.Timber;

import static rs.readahead.washington.mobile.views.activity.MetadataViewerActivity.VIEW_METADATA;

@RuntimePermissions
public class AudioPlayActivity extends BaseLockActivity implements
        IAudioPlayPresenterContract.IView,
        IMediaFileViewerPresenterContract.IView,
        ShareDialogFragment.IShareDialogFragmentHandler {
    public static final String PLAY_MEDIA_FILE = "pmf";
    public static final String PLAY_MEDIA_FILE_ID_KEY = "pmfik";
    public static final String NO_ACTIONS = "na";
    private static final String TIME_FORMAT = "%02d:%02d:%02d";

    @BindView(R.id.play_audio)
    ImageButton mPlay;
    @BindView(R.id.rwd_button)
    ImageButton mRwd;
    @BindView(R.id.fwd_button)
    ImageButton mFwd;
    @BindView(R.id.audio_time)
    TextView mTimer;
    @BindView(R.id.duration)
    TextView mDuration;
    @BindView(R.id.forward)
    View forward;
    @BindView(R.id.rewind)
    View rewind;

    private AudioPlayPresenter presenter;
    private MediaFile handlingMediaFile;
    private AudioPlayer audioPlayer;
    private AudioPlayer.Listener audioPlayerListener;

    private MediaFileViewerPresenter viewerPresenter;

    private boolean showActions = false;
    private boolean actionsDisabled = false;
    private AlertDialog alertDialog;
    private ProgressDialog progressDialog;

    private boolean paused = true;


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
        enablePlay();

        if (getIntent().hasExtra(NO_ACTIONS)) {
            actionsDisabled = true;
        }

        audioPlayerListener = new AudioPlayer.Listener() {

            @Override
            public void onStart(int duration) {
                mDuration.setText(timeToString(duration));
            }

            @Override
            public void onStop() {
                stopPlayer();
                paused = true;
                enablePlay();
                showTimeRemaining(0);
            }

            @Override
            public void onProgress(int currentPosition) {
                showTimeRemaining(currentPosition);
            }

            private void showTimeRemaining(int left) {
                mTimer.setText(timeToString(left));
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

            if (handlingMediaFile != null && handlingMediaFile.getMetadata() != null) {
                MenuItem item = menu.findItem(R.id.menu_item_metadata);
                item.setVisible(true);
            }
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
            shareMediaFile();
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

        if (id == R.id.menu_item_metadata) {
            showMetadata();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick({R.id.play_audio, R.id.fwd_button, R.id.rwd_button})
    public void manageClick(View view) {
        int SEEK_DELAY = 15000;
        switch (view.getId()) {
            case R.id.play_audio:
                if (paused) {
                    handlePlay();
                } else {
                    handlePause();
                }
                break;
            case R.id.rwd_button:
                audioPlayer.rwd(SEEK_DELAY);
                break;
            case R.id.fwd_button:
                audioPlayer.ffwd(SEEK_DELAY);
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
        handlePause();
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

        dismissShareDialog();

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
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.permission_dialog_expl_device_storage));
    }

    @Override
    public void onMediaFileSuccess(MediaFile mediaFile) {
        handlingMediaFile = mediaFile;
        //handlePlay();

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
        showToast(getResources().getQuantityString((R.plurals.gallery_toast_files_exported), 1, 1));
    }

    @Override
    public void onExportError(Throwable error) {
        showToast(R.string.gallery_toast_fail_exporting_to_device);
    }

    @Override
    public void onExportStarted() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.gallery_save_to_device_dialog_progress_expl));
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
        showToast(R.string.gallery_toast_fail_deleting_files);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void sharingMediaMetadataSelected() {
        dismissShareDialog();
        startShareActivity(true);
    }

    @Override
    public void sharingMediaOnlySelected() {
        dismissShareDialog();
        startShareActivity(false);
    }

    private void shareMediaFile() {
        if (handlingMediaFile == null) {
            return;
        }

        if (handlingMediaFile.getMetadata() != null) {
            ShareDialogFragment.newInstance().show(getSupportFragmentManager(), ShareDialogFragment.TAG);
        } else {
            startShareActivity(false);
        }
    }

    private void startShareActivity(boolean includeMetadata) {
        if (handlingMediaFile == null) {
            return;
        }

        MediaFileHandler.startShareActivity(this, handlingMediaFile, includeMetadata);
    }

    private void dismissShareDialog() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(ShareDialogFragment.TAG);
        if (fragment instanceof ShareDialogFragment) {
            ((ShareDialogFragment) fragment).dismiss();
        }
    }

    private void showExportDialog() {
        alertDialog = DialogsUtil.showExportMediaDialog(this, (dialog, which) ->
                AudioPlayActivityPermissionsDispatcher.exportMediaFileWithPermissionCheck(AudioPlayActivity.this));
    }

    private void showDeleteMediaDialog() {
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.gallery_delete_files_dialog_title)
                .setMessage(R.string.gallery_delete_files_dialog_expl)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    if (viewerPresenter != null && handlingMediaFile != null) {
                        viewerPresenter.deleteMediaFiles(handlingMediaFile);
                    }
                })
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();
    }

    /*private void handleStop() {
        stopPlayer();
    }*/

    private void handlePlay() {
        if (handlingMediaFile == null) {
            return;
        }

        if (audioPlayer != null) {
            audioPlayer.resume();
        } else {
            audioPlayer = new AudioPlayer(this, audioPlayerListener);
            audioPlayer.play(handlingMediaFile);
        }

        paused = false;
        disablePlay();
        disableScreenTimeout();
    }

    private void handlePause() {
        if (handlingMediaFile == null) {
            return;
        }

        enablePlay();
        paused = true;

        if (audioPlayer != null) {
            audioPlayer.pause();
        }
        enableScreenTimeout();
    }

    private void onPlayerStop() {
        enablePlay();
    }

    private void disablePlay() {
        mPlay.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_pause_black_24dp));
        enableButton(forward, mFwd);
        enableButton(rewind, mRwd);
    }

    private void enablePlay() {
        mPlay.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));
        disableButton(forward, mFwd);
        disableButton(rewind, mRwd);
    }

    private void enableButton(View view, ImageButton button) {
        button.setClickable(true);
        view.setAlpha(1f);
    }

    private void disableButton(View view, ImageButton button) {
        button.setClickable(false);
        view.setAlpha(.3f);
    }

    private void stopPlayer() {
        if (audioPlayer != null) {
            audioPlayer.stop();
            audioPlayer = null;
            onPlayerStop();
            enableScreenTimeout();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void showMetadata() {
        Intent viewMetadata = new Intent(this, MetadataViewerActivity.class);
        viewMetadata.putExtra(VIEW_METADATA, handlingMediaFile);
        startActivity(viewMetadata);
    }

    private String timeToString(long duration) {
        return String.format(Locale.ROOT, TIME_FORMAT,
                TimeUnit.MILLISECONDS.toHours(duration),
                TimeUnit.MILLISECONDS.toMinutes(duration) -
                        TimeUnit.MINUTES.toMinutes(TimeUnit.MILLISECONDS.toHours(duration)),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
    }

    private void disableScreenTimeout() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void enableScreenTimeout() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
