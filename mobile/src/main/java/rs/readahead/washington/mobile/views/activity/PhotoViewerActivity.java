package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.InflateException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.hzontal.tella_vault.VaultFile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
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
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.VaultFileUrlLoader;
import rs.readahead.washington.mobile.mvp.contract.IMediaFileViewerPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.MediaFileViewerPresenter;
import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;
import rs.readahead.washington.mobile.views.fragment.ShareDialogFragment;
import rs.readahead.washington.mobile.views.fragment.vault.info.VaultInfoFragment;

import static rs.readahead.washington.mobile.views.activity.MetadataViewerActivity.VIEW_METADATA;

import org.hzontal.shared_ui.appbar.ToolbarComponent;
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils;
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils;
import org.hzontal.shared_ui.utils.DialogUtils;


@RuntimePermissions
public class PhotoViewerActivity extends BaseLockActivity implements
        IMediaFileViewerPresenterContract.IView,
        ShareDialogFragment.IShareDialogFragmentHandler {
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
    private ToolbarComponent toolbar;
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
        toolbar.setStartTextTitle(vaultFile.name);
        toolbar.setBackClickListener(() -> {
            onBackPressed();
            return Unit.INSTANCE;
        });

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

        dismissShareDialog();

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
            presenter.exportNewMediaFile(vaultFile);
        }
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showWriteExternalStorageRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.permission_dialog_expl_device_storage));
    }

    private void openMedia(){
        showGalleryImage(vaultFile);
        if (!actionsDisabled) {
            showActions = true;
            invalidateOptionsMenu();
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
        DialogUtils.showBottomMessage(this,getString(R.string.gallery_toast_fail_deleting_files),true);
    }

    @Override
    public void onMediaFileRename(VaultFile vaultFile) {
        toolbar.setStartTextTitle(vaultFile.name);
        MyApplication.bus().post(new VaultFileRenameEvent());
    }

    @Override
    public void onMediaFileRenameError(Throwable throwable) {
        //TODO CHECK ERROR MSG WHEN RENAME
        DialogUtils.showBottomMessage(this,getString(R.string.gallery_toast_fail_deleting_files),true);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        menu.findItem(R.id.menu_item_more).setVisible(true);
        if (vaultFile.metadata != null) {menu.findItem(R.id.menu_item_metadata).setVisible(true);}
        toolbar.setStartTextTitle(vaultFile.name);
        finish();
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
        if (vaultFile == null) {
            return;
        }

        if (vaultFile.metadata != null) {
            ShareDialogFragment.newInstance().show(getSupportFragmentManager(), ShareDialogFragment.TAG);
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

    private void dismissShareDialog() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(ShareDialogFragment.TAG);
        if (fragment instanceof ShareDialogFragment) {
            ((ShareDialogFragment) fragment).dismiss();
        }
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

    private void showVaultActionsDialog(VaultFile vaultFile){
        VaultSheetUtils.showVaultActionsSheet(getSupportFragmentManager(),
                vaultFile.name,
                getString(R.string.action_upload),
                getString(R.string.action_share),
                getString(R.string.vault_move_to_another_folder),
                getString(R.string.vault_rename),
                getString(R.string.action_save),
                getString(R.string.vault_file_information),
                getString(R.string.action_delete),
                false,
                false,
                Preferences.isOfflineMode(),
                new VaultSheetUtils.IVaultActions() {
                    @Override
                    public void upload() {

                    }

                    @Override
                    public void share() {
                        startShareActivity(false);
                    }

                    @Override
                    public void move() {

                    }

                    @Override
                    public void rename() {
                        VaultSheetUtils.showVaultRenameSheet(
                                getSupportFragmentManager(),
                                getString(R.string.vault_rename_file),
                                getString(R.string.action_cancel),
                                getString(R.string.action_ok),
                                PhotoViewerActivity.this,
                                vaultFile.name,
                                (name) -> {
                                    presenter.renameVaultFile(vaultFile.id,name);
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
                                    PhotoViewerActivityPermissionsDispatcher.exportMediaFileWithPermissionCheck(PhotoViewerActivity.this);                          }
                        );
                    }

                    @Override
                    public void info() {
                        toolbar.setStartTextTitle(getString(R.string.vault_file_info));
                        menu.findItem(R.id.menu_item_more).setVisible(false);
                        menu.findItem(R.id.menu_item_metadata).setVisible(false);
                        invalidateOptionsMenu();
                        addFragment(new VaultInfoFragment().newInstance(vaultFile,false),R.id.photo_viewer_container);
                    }

                    @Override
                    public void delete() {
                        BottomSheetUtils.showConfirmSheet(
                                getSupportFragmentManager(),
                                getString(R.string.vault_delete_file),
                                getString(R.string.vault_delete_file_msg),
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
}
