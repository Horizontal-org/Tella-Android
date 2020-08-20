package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
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
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

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
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.MediaFileUrlLoader;
import rs.readahead.washington.mobile.mvp.contract.IMediaFileViewerPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.MediaFileViewerPresenter;
import rs.readahead.washington.mobile.presentation.entity.MediaFileLoaderModel;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.views.fragment.ShareDialogFragment;

import static rs.readahead.washington.mobile.views.activity.MetadataViewerActivity.VIEW_METADATA;


@RuntimePermissions
public class PhotoViewerActivity extends CacheWordSubscriberBaseActivity implements
        IMediaFileViewerPresenterContract.IView,
        ShareDialogFragment.IShareDialogFragmentHandler {
    public static final String VIEW_PHOTO = "vp";
    public static final String NO_ACTIONS = "na";

    @BindView(R.id.photoImageView)
    ImageView photoImageView;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    private CacheWordDataSource cacheWordDataSource;
    private MediaFileViewerPresenter presenter;
    private MediaFile mediaFile;

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

        cacheWordDataSource = new CacheWordDataSource(this);
        presenter = new MediaFileViewerPresenter(this);

        if (getIntent().hasExtra(VIEW_PHOTO)) {
            //noinspection ConstantConditions
            MediaFile mediaFile = (MediaFile) getIntent().getExtras().get(VIEW_PHOTO);
            if (mediaFile != null) {
                this.mediaFile = mediaFile;
            }
        }

        if (getIntent().hasExtra(NO_ACTIONS)) {
            actionsDisabled = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!actionsDisabled && showActions) {
            getMenuInflater().inflate(R.menu.photo_view_menu, menu);
            if (mediaFile.getMetadata() != null) {
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
        cacheWordDataSource.dispose();

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
        if (mediaFile != null && presenter != null) {
            presenter.exportNewMediaFile(mediaFile);
        }
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showWriteExternalStorageRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.permission_dialog_expl_device_storage));
    }

    @Override
    public void onCacheWordOpened() {
        super.onCacheWordOpened();
        showGalleryImage(mediaFile);
        if (!actionsDisabled) {
            showActions = true;
            invalidateOptionsMenu();
        }
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

    private void showExportDialog() {
        alertDialog = DialogsUtil.showExportMediaDialog(this, (dialog, which) ->
                PhotoViewerActivityPermissionsDispatcher.exportMediaFileWithPermissionCheck(PhotoViewerActivity.this));
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

    private void showGalleryImage(MediaFile mediaFile) {
        Glide.with(this)
                .using(new MediaFileUrlLoader(this, new MediaFileHandler(cacheWordDataSource)))
                .load(new MediaFileLoaderModel(mediaFile, MediaFileLoaderModel.LoadType.ORIGINAL))
                .listener(new RequestListener<MediaFileLoaderModel, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, MediaFileLoaderModel model,
                                               Target<GlideDrawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, MediaFileLoaderModel model,
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
        viewMetadata.putExtra(VIEW_METADATA, mediaFile);
        startActivity(viewMetadata);
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }
}
