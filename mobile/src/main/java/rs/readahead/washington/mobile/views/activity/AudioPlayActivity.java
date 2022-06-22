package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.hzontal.tella_vault.VaultFile;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
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
import rs.readahead.washington.mobile.views.fragment.vault.info.VaultInfoFragment;
import timber.log.Timber;

import static rs.readahead.washington.mobile.views.activity.MetadataViewerActivity.VIEW_METADATA;
import static rs.readahead.washington.mobile.views.fragment.vault.attachements.AttachmentsFragmentKt.PICKER_FILE_REQUEST_CODE;

import org.hzontal.shared_ui.appbar.ToolbarComponent;
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils;
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils;

@RuntimePermissions
public class AudioPlayActivity extends BaseLockActivity implements
        IAudioPlayPresenterContract.IView,
        IMediaFileViewerPresenterContract.IView {
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
    private VaultFile handlingVaultFile;
    private AudioPlayer audioPlayer;
    private AudioPlayer.Listener audioPlayerListener;

    private MediaFileViewerPresenter viewerPresenter;

    private boolean showActions = false;
    private boolean actionsDisabled = false;
    private AlertDialog alertDialog;
    private ProgressDialog progressDialog;

    private boolean paused = true;
    private Toolbar toolbar;
    private boolean isInfoShown = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_audio_play);
        overridePendingTransition(R.anim.slide_in_start, R.anim.fade_out);
        ButterKnife.bind(this);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
            VaultFile vaultFile = (VaultFile) getIntent().getSerializableExtra(PLAY_MEDIA_FILE);
            if (vaultFile != null) {
                ThreadUtil.runOnMain(() -> onMediaFileSuccess(vaultFile));
            }
        } else if (getIntent().hasExtra(PLAY_MEDIA_FILE_ID_KEY)) {
            String id = getIntent().getStringExtra(PLAY_MEDIA_FILE_ID_KEY);
            if (id != null) {
                presenter = new AudioPlayPresenter(this);
                presenter.getMediaFile(id);
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_start);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!actionsDisabled && showActions) {
            toolbar.inflateMenu(R.menu.video_view_menu);

            if (handlingVaultFile != null && handlingVaultFile.metadata != null) {
                MenuItem item = toolbar.getMenu().findItem(R.id.menu_item_metadata);
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

        if (id == R.id.menu_item_more) {
            showVaultActionsDialog(handlingVaultFile);
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
        if (isInfoShown) {
            toolbar.getMenu().findItem(R.id.menu_item_more).setVisible(true);
            toolbar.getMenu().findItem(R.id.menu_item_metadata).setVisible(true);
            toolbar.setTitle(handlingVaultFile.name);
        } else {
            stopPlayer();
            finish();
        }
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

        super.onDestroy();
    }


    @SuppressLint("NeedOnRequestPermissionsResult")
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
        changeTemporaryTimeout();
        if (handlingVaultFile != null && viewerPresenter != null) {
            performFileSearch();
        }
    }

    private void performFileSearch() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, PICKER_FILE_REQUEST_CODE);
        } else {
            viewerPresenter.exportNewMediaFile(handlingVaultFile, null);
        }
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showWriteExternalStorageRationale(final PermissionRequest request) {
        changeTemporaryTimeout();
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.permission_dialog_expl_device_storage));
    }

    @Override
    public void onMediaFileSuccess(VaultFile vaultFile) {
        handlingVaultFile = vaultFile;
        toolbar.setTitle(vaultFile.name);
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
    public void onMediaFileRename(VaultFile vaultFile) {
        toolbar.setTitle(vaultFile.name);
        MyApplication.bus().post(new VaultFileRenameEvent());
    }

    @Override
    public void onMediaFileRenameError(Throwable throwable) {

    }

    @Override
    public Context getContext() {
        return this;
    }

    private void shareMediaFile() {
        if (handlingVaultFile == null) {
            return;
        }

        if (handlingVaultFile.metadata != null) {
            showShareWithMetadataDialog();
        } else {
            startShareActivity(false);
        }
    }

    private void startShareActivity(boolean includeMetadata) {
        if (handlingVaultFile == null) {
            return;
        }

        MediaFileHandler.startShareActivity(this, handlingVaultFile, includeMetadata);
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
                    if (viewerPresenter != null && handlingVaultFile != null) {
                        viewerPresenter.deleteMediaFiles(handlingVaultFile);
                    }
                })
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();
    }

    /*private void handleStop() {
        stopPlayer();xr
    }*/

    private void handlePlay() {
        if (handlingVaultFile == null) {
            return;
        }

        if (audioPlayer != null) {
            audioPlayer.resume();
        } else {
            audioPlayer = new AudioPlayer(this, audioPlayerListener);
            audioPlayer.play(handlingVaultFile);
        }

        paused = false;
        disablePlay();
        disableScreenTimeout();
    }

    private void handlePause() {
        if (handlingVaultFile == null) {
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
        mPlay.setImageDrawable(getContext().getResources().getDrawable(R.drawable.big_white_pause_24p));
        enableButton(forward, mFwd);
        enableButton(rewind, mRwd);
    }

    private void enablePlay() {
        mPlay.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
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
        viewMetadata.putExtra(VIEW_METADATA, handlingVaultFile);
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
                        shareMediaFile();
                    }

                    @Override
                    public void move() {

                    }

                    @Override
                    public void rename() {
                        VaultSheetUtils.showVaultRenameSheet(
                                getSupportFragmentManager(),
                                getString(R.string.Vault_RenameFile_SheetTitle),
                                getString(R.string.action_cancel),
                                getString(R.string.action_ok),
                                AudioPlayActivity.this,
                                vaultFile.name,
                                (name) -> {
                                    viewerPresenter.renameVaultFile(vaultFile.id, name);
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
                                isConfirmed -> AudioPlayActivityPermissionsDispatcher.exportMediaFileWithPermissionCheck(AudioPlayActivity.this)
                        );
                    }

                    @Override
                    public void info() {
                        toolbar.setTitle(getString(R.string.Vault_FileInfo));
                        toolbar.getMenu().findItem(R.id.menu_item_more).setVisible(false);
                        toolbar.getMenu().findItem(R.id.menu_item_metadata).setVisible(false);
                        invalidateOptionsMenu();
                        addFragment(new VaultInfoFragment().newInstance(vaultFile, false), R.id.root);
                        isInfoShown = true;
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
                                    viewerPresenter.deleteMediaFiles(vaultFile);
                                }
                        );

                    }
                }
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKER_FILE_REQUEST_CODE) {
            viewerPresenter.exportNewMediaFile(handlingVaultFile, data.getData());
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
}
