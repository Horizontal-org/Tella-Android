package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.hzontal.tella_vault.VaultFile;

import butterknife.BindView;
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
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.MediaFileUrlLoader;
import rs.readahead.washington.mobile.media.VaultFileUrlLoader;
import rs.readahead.washington.mobile.mvp.contract.IMediaFileViewerPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.MediaFileViewerPresenter;
import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;
import rs.readahead.washington.mobile.views.fragment.ShareDialogFragment;

import static rs.readahead.washington.mobile.views.activity.MetadataViewerActivity.VIEW_METADATA;


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

    //private CacheWordDataSource cacheWordDataSource;
    private KeyDataSource keyDataSource;
    private MediaFileViewerPresenter presenter;
    private VaultFile vaultFile;

    private boolean showActions = false;
    private boolean actionsDisabled = false;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo_viewer);
        ButterKnife.bind(this);

        setTitle(null);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //cacheWordDataSource = new CacheWordDataSource(this);
        keyDataSource = MyApplication.getKeyDataSource();
        presenter = new MediaFileViewerPresenter(this);

        if (getIntent().hasExtra(VIEW_PHOTO)) {
            //noinspection ConstantConditions
            VaultFile vaultFile = (VaultFile) getIntent().getExtras().get(VIEW_PHOTO);
            if (vaultFile != null) {
                this.vaultFile = vaultFile;
            }
        }

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

    @Override
    protected void onDestroy() {
        // keyDataSource.dispose();

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

    private void showExportDialog() {
        alertDialog = DialogsUtil.showExportMediaDialog(this, (dialog, which) ->
                PhotoViewerActivityPermissionsDispatcher.exportMediaFileWithPermissionCheck(PhotoViewerActivity.this));
    }

    private void showDeleteMediaDialog() {
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.gallery_delete_files_dialog_title)
                .setMessage(R.string.gallery_delete_files_dialog_expl)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    if (vaultFile != null && presenter != null) {
                        presenter.deleteMediaFiles(vaultFile);
                    }
                })
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();
    }

    private void showGalleryImage(VaultFile vaultFile) {
        Glide.with(this)
                .using(new VaultFileUrlLoader(this, new MediaFileHandler(keyDataSource)))
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
}
