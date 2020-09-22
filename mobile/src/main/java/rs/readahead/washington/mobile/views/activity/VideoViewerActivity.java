package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import butterknife.ButterKnife;
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
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.exo.ExoEventListener;
import rs.readahead.washington.mobile.media.exo.MediaFileDataSourceFactory;
import rs.readahead.washington.mobile.mvp.contract.IMediaFileViewerPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.MediaFileViewerPresenter;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.views.fragment.ShareDialogFragment;

import static rs.readahead.washington.mobile.views.activity.MetadataViewerActivity.VIEW_METADATA;

@RuntimePermissions
public class VideoViewerActivity extends CacheWordSubscriberBaseActivity implements
        PlaybackControlView.VisibilityListener,
        IMediaFileViewerPresenterContract.IView,
        ShareDialogFragment.IShareDialogFragmentHandler {
    public static final String VIEW_VIDEO = "vv";
    public static final String NO_ACTIONS = "na";

    public static final int SDK_INT =
            (Build.VERSION.SDK_INT == 25 && Build.VERSION.CODENAME.charAt(0) == 'O') ? 26
                    : Build.VERSION.SDK_INT;

    private SimpleExoPlayerView simpleExoPlayerView;
    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;

    private boolean needRetrySource;
    private boolean shouldAutoPlay;
    private int resumeWindow;
    private long resumePosition;

    private MediaFile mediaFile;
    private Toolbar toolbar;
    private boolean actionsDisabled = false;
    private MediaFileViewerPresenter presenter;
    private AlertDialog alertDialog;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_viewer);
        ButterKnife.bind(this);

        if (getIntent().hasExtra(NO_ACTIONS)) {
            actionsDisabled = true;
        }

        setupToolbar();

        shouldAutoPlay = true;
        clearResumePosition();

        simpleExoPlayerView = findViewById(R.id.player_view);
        simpleExoPlayerView.setControllerVisibilityListener(this);
        simpleExoPlayerView.requestFocus();

        presenter = new MediaFileViewerPresenter(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        releasePlayer();
        shouldAutoPlay = true;
        clearResumePosition();
        setIntent(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((SDK_INT <= 23 || player == null)) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (SDK_INT > 23) {
            releasePlayer();
        }
    }

    @Override
    protected void onDestroy() {
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }

        hideProgressDialog();

        if (presenter != null) {
            presenter.destroy();
        }

        dismissShareDialog();

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        VideoViewerActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onWriteExternalStoragePermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onWriteExternalStorageNeverAskAgain() {
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void exportMediaFile() {
        if (mediaFile != null && presenter != null) {
            presenter.exportNewMediaFile(mediaFile);
        }
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showWriteExternalStorageRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.permission_dialog_expl_device_storage));
    }


    @Override
    public void onMediaExported() {
        showToast(R.string.gallery_toast_file_exported);
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

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void showExportDialog() {
        alertDialog = DialogsUtil.showExportMediaDialog(this, (dialog, which) ->
                VideoViewerActivityPermissionsDispatcher.exportMediaFileWithPermissionCheck(VideoViewerActivity.this));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Show the controls on any key event.
        simpleExoPlayerView.showController();
        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || simpleExoPlayerView.dispatchMediaKeyEvent(event);
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
        if (mediaFile == null) {
            return;
        }

        if (mediaFile.getMetadata() != null) {
            ShareDialogFragment.newInstance().show(getSupportFragmentManager(), ShareDialogFragment.TAG);
        } else {
            startShareActivity(false);
        }
    }

    private void startShareActivity(boolean includeMetadata) {
        if (mediaFile == null) {
            return;
        }

        MediaFileHandler.startShareActivity(this, mediaFile, includeMetadata);
    }

    private void dismissShareDialog() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(ShareDialogFragment.TAG);
        if (fragment instanceof ShareDialogFragment) {
            ((ShareDialogFragment) fragment).dismiss();
        }
    }

    private void initializePlayer() {
        boolean needNewPlayer = player == null;

        if (needNewPlayer) {
            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
            trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
            player.addListener(new ExoEventListener());
            simpleExoPlayerView.setPlayer(player);

            player.setPlayWhenReady(shouldAutoPlay);
        }

        if (needNewPlayer || needRetrySource) {
            if (getIntent().hasExtra(VIEW_VIDEO) && getIntent().getExtras() != null) {
                MediaFile mediaFile = (MediaFile) getIntent().getExtras().get(VIEW_VIDEO);
                if (mediaFile != null) {
                    this.mediaFile = mediaFile;
                    setupMetadataMenuItem(mediaFile.getMetadata() != null);
                }
            }

            MediaFileDataSourceFactory mediaFileDataSourceFactory = new MediaFileDataSourceFactory(this, mediaFile, null);
            MediaSource mediaSource = new ExtractorMediaSource(
                    MediaFileHandler.getEncryptedUri(this, mediaFile),
                    mediaFileDataSourceFactory,
                    new DefaultExtractorsFactory(),
                    null, null);

            boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
            if (haveResumePosition) {
                player.seekTo(resumeWindow, resumePosition);
            }
            player.prepare(mediaSource, !haveResumePosition, false);
            needRetrySource = false;
        }
    }

    private void releasePlayer() {
        if (player != null) {
            shouldAutoPlay = player.getPlayWhenReady();
            //updateResumePosition(); // todo: fix source skipping..
            player.release();
            player = null;
            trackSelector = null;
            clearResumePosition();
        }
    }

    /*private void updateResumePosition() {
        resumeWindow = player.getCurrentWindowIndex();
        resumePosition = player.isCurrentWindowSeekable() ? Math.max(0, player.getCurrentPosition())
                : C.TIME_UNSET;
    }*/

    private void clearResumePosition() {
        resumeWindow = C.INDEX_UNSET;
        resumePosition = C.TIME_UNSET;
    }

    @Override
    public void onVisibilityChange(int visibility) {
        toolbar.setVisibility(visibility);
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.player_toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (!actionsDisabled) {
            toolbar.inflateMenu(R.menu.video_view_menu);

            if (mediaFile != null) {
                setupMetadataMenuItem(mediaFile.getMetadata() != null);
            }

            toolbar.getMenu().findItem(R.id.menu_item_share).setOnMenuItemClickListener(item -> {
                shareMediaFile();
                return false;
            });

            toolbar.getMenu().findItem(R.id.menu_item_export).setOnMenuItemClickListener(item -> {
                if (mediaFile != null) {
                    showExportDialog();
                }
                return false;
            });

            toolbar.getMenu().findItem(R.id.menu_item_delete).setOnMenuItemClickListener(item -> {
                if (mediaFile != null) {
                    showDeleteMediaDialog();
                }
                return false;
            });
        }
    }

    private void showDeleteMediaDialog() {
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.gallery_delete_files_dialog_title)
                .setMessage(R.string.gallery_delete_files_dialog_expl)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    if (mediaFile != null && presenter != null) {
                        presenter.deleteMediaFiles(mediaFile);
                    }
                })
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();
    }

    private void showMetadata() {
        Intent viewMetadata = new Intent(this, MetadataViewerActivity.class);
        viewMetadata.putExtra(VIEW_METADATA, mediaFile);
        startActivity(viewMetadata);
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void setupMetadataMenuItem(boolean visible) {
        if (actionsDisabled) {
            return;
        }

        MenuItem mdMenuItem = toolbar.getMenu().findItem(R.id.menu_item_metadata);

        if (visible) {
            mdMenuItem.setVisible(true).setOnMenuItemClickListener(item -> {
                showMetadata();
                return false;
            });
        } else {
            mdMenuItem.setVisible(false);
        }
    }
}
