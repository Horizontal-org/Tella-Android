package rs.readahead.washington.mobile.views.activity;

import static rs.readahead.washington.mobile.views.activity.MetadataViewerActivity.VIEW_METADATA;
import static rs.readahead.washington.mobile.views.fragment.vault.attachements.AttachmentsFragmentKt.PICKER_FILE_REQUEST_CODE;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.hzontal.tella_vault.VaultFile;

import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils;
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils;
import org.hzontal.shared_ui.utils.DialogUtils;

import java.util.LinkedHashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
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
import rs.readahead.washington.mobile.media.VaultFileUrlLoader;
import rs.readahead.washington.mobile.mvp.contract.IMediaFileViewerPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.MediaFileViewerPresenter;
import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;
import rs.readahead.washington.mobile.views.fragment.vault.info.VaultInfoFragment;


@RuntimePermissions
public class PhotoViewerActivity extends BaseLockActivity implements
        IMediaFileViewerPresenterContract.IView {
    public static final String VIEW_PHOTO = "vp";
    public static final String NO_ACTIONS = "na";

    @BindView(R.id.photoImageView)
    ImageView photoImageView;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    private MediaFileViewerPresenter presenter;
    private VaultFile vaultFile;

    private boolean showActions = false;
    private boolean actionsDisabled = false;
    private AlertDialog alertDialog;
    private Toolbar toolbar;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo_viewer);
        overridePendingTransition(R.anim.slide_in_start, R.anim.fade_out);
        ButterKnife.bind(this);
        setTitle(null);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById(R.id.appbar).setOutlineProvider(null);
        } else {
            findViewById(R.id.appbar).bringToFront();
        }

        presenter = new MediaFileViewerPresenter(this);

        if (getIntent().hasExtra(VIEW_PHOTO)) {
            //noinspection ConstantConditions
            VaultFile vaultFile = (VaultFile) getIntent().getExtras().get(VIEW_PHOTO);
            if (vaultFile != null) {
                this.vaultFile = vaultFile;
            }
        }
        setTitle(vaultFile.name);

        if (getIntent().hasExtra(NO_ACTIONS)) {
            actionsDisabled = true;
        }

        openMedia();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!actionsDisabled && showActions) {
            getMenuInflater().inflate(R.menu.photo_view_menu, menu);
            if (vaultFile.metadata != null) {
                MenuItem item = menu.findItem(R.id.menu_item_metadata);
                item.setVisible(true);
            }
        }
        this.menu = menu;
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
            showVaultActionsDialog(vaultFile);
            return true;
        }

        if (id == R.id.menu_item_metadata) {
            showMetadata();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_start);
    }

    @Override
    protected void onDestroy() {

        stopPresenter();

        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PhotoViewerActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
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
            performFileSearch();
        }
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showWriteExternalStorageRationale(final PermissionRequest request) {
        maybeChangeTemporaryTimeout();
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.permission_dialog_expl_device_storage));
    }

    private void openMedia() {
        showGalleryImage(vaultFile);
        if (!actionsDisabled) {
            showActions = true;
            invalidateOptionsMenu();
        }
    }

    private void performFileSearch() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, PICKER_FILE_REQUEST_CODE);
        } else {
            presenter.exportNewMediaFile(vaultFile, null);
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
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onExportEnded() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onMediaFileDeleted() {
        MyApplication.bus().post(new MediaFileDeletedEvent());
        finish();
    }

    @Override
    public void onMediaFileDeletionError(Throwable throwable) {
        DialogUtils.showBottomMessage(this, getString(R.string.gallery_toast_fail_deleting_files), true);
    }

    @Override
    public void onMediaFileRename(VaultFile vaultFile) {
        toolbar.setTitle(vaultFile.name);
        MyApplication.bus().post(new VaultFileRenameEvent());
    }

    @Override
    public void onMediaFileRenameError(Throwable throwable) {
        //TODO CHECK ERROR MSG WHEN RENAME
        DialogUtils.showBottomMessage(this, getString(R.string.gallery_toast_fail_deleting_files), true);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (menu.findItem(R.id.menu_item_more) != null) {
            menu.findItem(R.id.menu_item_more).setVisible(true);
        }
        if (vaultFile.metadata != null && menu.findItem(R.id.menu_item_metadata) != null) {
            menu.findItem(R.id.menu_item_metadata).setVisible(true);
        }
        toolbar.setTitle(vaultFile.name);
        finish();
    }

    @Override
    public Context getContext() {
        return this;
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

    private void showGalleryImage(VaultFile vaultFile) {
        Glide.with(this)
                .using(new VaultFileUrlLoader(this, new MediaFileHandler()))
                .load(new VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.ORIGINAL))
                .listener(new RequestListener<VaultFileLoaderModel, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, VaultFileLoaderModel model,
                                               Target<GlideDrawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, VaultFileLoaderModel model,
                                                   Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(photoImageView);
    }

    private void showMetadata() {
        Intent viewMetadata = new Intent(this, MetadataViewerActivity.class);
        viewMetadata.putExtra(VIEW_METADATA, vaultFile);
        startActivity(viewMetadata);
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
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
                        shareMediaFile();
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
                                PhotoViewerActivity.this,
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
                                    maybeChangeTemporaryTimeout(() -> {
                                        PhotoViewerActivityPermissionsDispatcher.exportMediaFileWithPermissionCheck(PhotoViewerActivity.this);
                                        return Unit.INSTANCE;
                                    });
                                }
                        );
                    }

                    @Override
                    public void info() {
                        toolbar.setTitle(getString(R.string.Vault_FileInfo));
                        menu.findItem(R.id.menu_item_more).setVisible(false);
                        menu.findItem(R.id.menu_item_metadata).setVisible(false);
                        invalidateOptionsMenu();
                        addFragment(new VaultInfoFragment().newInstance(vaultFile, false), R.id.photo_viewer_container);
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKER_FILE_REQUEST_CODE) {
            assert data != null;
            presenter.exportNewMediaFile(vaultFile, data.getData());
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
