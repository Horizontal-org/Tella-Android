package rs.readahead.washington.mobile.views.activity;

import static rs.readahead.washington.mobile.views.activity.MetadataViewerActivity.VIEW_METADATA;
import static rs.readahead.washington.mobile.views.fragment.vault.attachements.AttachmentsFragmentKt.PICKER_FILE_REQUEST_CODE;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

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
import com.hzontal.tella_vault.VaultFile;

import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils;
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils;
import org.hzontal.shared_ui.utils.DialogUtils;

import java.util.LinkedHashMap;

import kotlin.Unit;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.MediaFileDeletedEvent;
import rs.readahead.washington.mobile.bus.event.VaultFileRenameEvent;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.exo.ExoEventListener;
import rs.readahead.washington.mobile.media.exo.MediaFileDataSourceFactory;
import rs.readahead.washington.mobile.mvp.contract.IMediaFileViewerPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.MediaFileViewerPresenter;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;
import rs.readahead.washington.mobile.views.fragment.vault.info.VaultInfoFragment;
import rs.readahead.washington.mobile.databinding.ActivityVideoViewerBinding;

@RuntimePermissions
public class VideoViewerActivity extends BaseLockActivity implements
        PlaybackControlView.VisibilityListener,
        IMediaFileViewerPresenterContract.IView {
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
    private boolean withMetadata = false;
    private int resumeWindow;
    private long resumePosition;

    private VaultFile vaultFile;
    private Toolbar toolbar;
    private boolean actionsDisabled = false;
    private MediaFileViewerPresenter presenter;
    private AlertDialog alertDialog;
    private ProgressDialog progressDialog;
    private boolean isInfoShown = false;
    private ActivityVideoViewerBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityVideoViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        overridePendingTransition(R.anim.slide_in_start, R.anim.fade_out);

        if (getIntent().hasExtra(NO_ACTIONS)) {
            actionsDisabled = true;
        }

        setupToolbar();

        shouldAutoPlay = true;
        clearResumePosition();

        simpleExoPlayerView = binding.playerView;
        simpleExoPlayerView.setControllerVisibilityListener(this);
        simpleExoPlayerView.requestFocus();

        presenter = new MediaFileViewerPresenter(this);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_start);
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

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //VideoViewerActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onWriteExternalStoragePermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onWriteExternalStorageNeverAskAgain() {
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void exportMediaFile() {
        if (vaultFile != null && presenter != null) {
            if (vaultFile.metadata != null) {
                showExportWithMetadataDialog();
            } else {
                withMetadata = false;
                maybeChangeTemporaryTimeout(() -> {
                    performFileSearch();
                    return Unit.INSTANCE;
                });
            }
        }
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showWriteExternalStorageRationale(final PermissionRequest request) {
        maybeChangeTemporaryTimeout(() -> {
            alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.permission_dialog_expl_device_storage));
            return Unit.INSTANCE;
        });
    }

    private void performFileSearch() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, PICKER_FILE_REQUEST_CODE);
        } else {
            presenter.exportNewMediaFile(withMetadata, vaultFile, null);
        }
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
    public void onMediaFileRename(VaultFile vaultFile) {
        if (vaultFile != null) {
            toolbar.setTitle(vaultFile.name);
            this.vaultFile = vaultFile;
        }
        MyApplication.bus().post(new VaultFileRenameEvent());
    }

    @Override
    public void onMediaFileRenameError(Throwable throwable) {
        //TODO CHECK ERROR MSG WHEN RENAME
        DialogUtils.showBottomMessage(this, getString(R.string.gallery_toast_fail_deleting_files), true);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void showExportDialog() {
       /*alertDialog = DialogsUtil.showExportMediaDialog(this, (dialog, which) ->{}*/
                //VideoViewerActivityPermissionsDispatcher.exportMediaFileWithPermissionCheck(VideoViewerActivity.this));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Show the controls on any key event.
        simpleExoPlayerView.showController();
        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || simpleExoPlayerView.dispatchMediaKeyEvent(event);
    }

    private void shareMediaFile() {
        if (vaultFile == null) {
            return;
        }

        if (vaultFile.metadata != null) {
            showShareWithMetadataDialog();
        } else {
            startShareActivity(false);
        }
    }

    private void startShareActivity(boolean includeMetadata) {
        if (vaultFile == null) {
            return;
        }

        MediaFileHandler.startShareActivity(this, vaultFile, includeMetadata);
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
                VaultFile vaultFile = (VaultFile) getIntent().getExtras().get(VIEW_VIDEO);
                if (vaultFile != null) {
                    this.vaultFile = vaultFile;
                    toolbar.setTitle(vaultFile.name);
                    setupMetadataMenuItem(vaultFile.metadata != null);
                }
            }

            MediaFileDataSourceFactory mediaFileDataSourceFactory = new MediaFileDataSourceFactory(this, vaultFile, null);
            MediaSource mediaSource = new ExtractorMediaSource(
                    MediaFileHandler.getEncryptedUri(this, vaultFile),
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


    private void clearResumePosition() {
        resumeWindow = C.INDEX_UNSET;
        resumePosition = C.TIME_UNSET;
    }

    @Override
    public void onVisibilityChange(int visibility) {
        if (!isInfoShown) {
            toolbar.setVisibility(visibility);
        } else {
            toolbar.setVisibility(View.VISIBLE);
        }
    }

    private void setupToolbar() {
        toolbar = binding.playerToolbar;
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (!actionsDisabled) {
            toolbar.inflateMenu(R.menu.video_view_menu);

            if (vaultFile != null) {
                setupMetadataMenuItem(vaultFile.metadata != null);
            }

            toolbar.getMenu().findItem(R.id.menu_item_more).setOnMenuItemClickListener(item -> {
                showVaultActionsDialog(vaultFile);
                return false;
            });
        }
    }


    private void showMetadata() {
        Intent viewMetadata = new Intent(this, MetadataViewerActivity.class);
        viewMetadata.putExtra(VIEW_METADATA, vaultFile);
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

    private void showVaultActionsDialog(VaultFile vaultFile) {
        VaultSheetUtils.showVaultActionsSheet(getSupportFragmentManager(),
                vaultFile.name,
                getString(R.string.Vault_Upload_SheetAction),
                getString(R.string.Vault_Share_SheetAction),
                getString(R.string.Vault_Move_SheetDesc),
                getString(R.string.Vault_Rename_SheetAction),
                getString(R.string.gallery_action_desc_save_to_device),
                getString(R.string.Vault_File_SheetAction),
                getString(R.string.Vault_Delete_SheetAction),
                false,
                false,
                false,
                false,
                new VaultSheetUtils.IVaultActions() {
                    @Override
                    public void upload() {

                    }

                    @Override
                    public void share() {
                        maybeChangeTemporaryTimeout(() -> {
                            shareMediaFile();
                            return Unit.INSTANCE;
                        });
                    }

                    @Override
                    public void move() {

                    }

                    @Override
                    public void rename() {
                        VaultSheetUtils.showVaultRenameSheet(
                                getSupportFragmentManager(),
                                getString(R.string.Vault_CreateFolder_SheetAction),
                                getString(R.string.action_cancel),
                                getString(R.string.action_ok),
                                VideoViewerActivity.this,
                                vaultFile.name,
                                (name) -> {
                                    presenter.renameVaultFile(vaultFile.id, name);
                                    return Unit.INSTANCE;
                                }
                        );
                    }

                    @Override
                    public void save() {
                        BottomSheetUtils.showConfirmSheet(
                                getSupportFragmentManager(),
                                getString(R.string.gallery_save_to_device_dialog_title),
                                getString(R.string.gallery_save_to_device_dialog_expl),
                                getString(R.string.action_save),
                                getString(R.string.action_cancel),
                                isConfirmed -> {
                                    //VideoViewerActivityPermissionsDispatcher.exportMediaFileWithPermissionCheck(VideoViewerActivity.this);
                                }
                        );
                    }

                    @Override
                    public void info() {
                        isInfoShown = true;
                        onVisibilityChange(View.VISIBLE);
                        toolbar.setTitle(getString(R.string.Vault_FileInfo));
                        toolbar.getMenu().findItem(R.id.menu_item_more).setVisible(false);
                        toolbar.getMenu().findItem(R.id.menu_item_metadata).setVisible(false);
                        invalidateOptionsMenu();
                        addFragment(new VaultInfoFragment().newInstance(vaultFile, false), R.id.container);

                    }

                    @Override
                    public void delete() {
                        BottomSheetUtils.showConfirmSheet(
                                getSupportFragmentManager(),
                                getString(R.string.Vault_DeleteFile_SheetTitle),
                                getString(R.string.Vault_deleteFile_SheetDesc),
                                getString(R.string.action_delete),
                                getString(R.string.action_cancel),
                                isConfirmed -> {
                                    presenter.deleteMediaFiles(vaultFile);
                                }
                        );

                    }
                }
        );
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        /*toolbar.setStartTextTitle(vaultFile.name);
        toolbar.getMenu().findItem(R.id.menu_item_more).setVisible(true);
        setupMetadataMenuItem(vaultFile.metadata != null);
        invalidateOptionsMenu();
        isInfoShown = false;*/
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKER_FILE_REQUEST_CODE) {
            assert data != null;
            presenter.exportNewMediaFile(withMetadata, vaultFile, data.getData());
        }
    }

    private void showShareWithMetadataDialog() {
        LinkedHashMap<Integer, Integer> options = new LinkedHashMap<>();
        options.put(1, R.string.verification_share_select_media_and_verification);
        options.put(0, R.string.verification_share_select_only_media);

        BottomSheetUtils.showRadioListOptionsSheet(
                getSupportFragmentManager(),
                getContext(),
                options,
                getString(R.string.verification_share_dialog_title),
                getString(R.string.verification_share_dialog_expl),
                getString(R.string.action_ok),
                getString(R.string.action_cancel),
                option -> startShareActivity(option > 0)
        );
    }

    private void showExportWithMetadataDialog() {
        LinkedHashMap<Integer, Integer> options = new LinkedHashMap<>();
        options.put(1, R.string.verification_share_select_media_and_verification);
        options.put(0, R.string.verification_share_select_only_media);
        new Handler().post(() -> {
            BottomSheetUtils.showRadioListOptionsSheet(
                    getSupportFragmentManager(),
                    getContext(),
                    options,
                    getString(R.string.verification_share_dialog_title),
                    getString(R.string.verification_share_dialog_expl),
                    getString(R.string.action_ok),
                    getString(R.string.action_cancel),
                    option -> {
                        withMetadata = option > 0;
                        maybeChangeTemporaryTimeout(() -> {
                            performFileSearch();
                            return Unit.INSTANCE;
                        });
                    }
            );
        });
    }
}
