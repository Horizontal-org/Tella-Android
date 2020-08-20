package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.GalleryFlingTopEvent;
import rs.readahead.washington.mobile.bus.event.MediaFileDeletedEvent;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository.Filter;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository.Sort;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IGalleryPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.GalleryPresenter;
import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;
import rs.readahead.washington.mobile.presentation.entity.ViewType;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.views.adapters.AttachmentsRecycleViewAdapter;
import rs.readahead.washington.mobile.views.adapters.GalleryRecycleViewAdapter;
import rs.readahead.washington.mobile.views.custom.GalleryRecyclerView;
import rs.readahead.washington.mobile.views.fragment.ShareDialogFragment;
import rs.readahead.washington.mobile.views.fragment.TellaUploadDialogFragment;
import rs.readahead.washington.mobile.views.interfaces.IAttachmentsMediaHandler;
import rs.readahead.washington.mobile.views.interfaces.IGalleryMediaHandler;
import timber.log.Timber;


@RuntimePermissions
public class GalleryActivity extends MetadataActivity implements
        TellaUploadDialogFragment.IServerMetadataChosenHandler,
        IGalleryPresenterContract.IView,
        IGalleryMediaHandler, IAttachmentsMediaHandler,
        ShareDialogFragment.IShareDialogFragmentHandler {
    public static final String GALLERY_ANIMATED = "ga";
    public static final String GALLERY_FILTER = "gf";
    public static final String GALLERY_ALLOWS_ADDING = "gaa";
    private boolean animated = false;
    private boolean adding = true;

    @BindView(R.id.galleryRecyclerView)
    GalleryRecyclerView recyclerView;
    @BindView(R.id.fab_button)
    FloatingActionButton fabButton;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.attachmentsToolbar)
    View attachmentsToolbar;
    @BindView(R.id.attachmentsRecyclerView)
    RecyclerView attachmentsRecyclerView;
    @BindView(R.id.gallery_blank_list_info)
    TextView blankGalleryInfo;

    private GalleryRecycleViewAdapter adapter;
    private GalleryPresenter presenter;
    private CacheWordDataSource cacheWordDataSource;
    private EventCompositeDisposable disposables;

    private AttachmentsRecycleViewAdapter attachmentsAdapter;
    private RecyclerView.LayoutManager attachmentsLayoutManager;

    private int selectedNum;
    private AlertDialog alertDialog;
    private ProgressDialog progressDialog;
    private Filter filter = Filter.ALL;
    private Sort sort = Sort.NEWEST;
    private ViewType type = ViewType.EDIT;
    private long numOfTUServers;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gallery);
        ButterKnife.bind(this);

        presenter = new GalleryPresenter(this);

        if (getIntent().hasExtra(GALLERY_ANIMATED)) {
            animated = getIntent().getBooleanExtra(GALLERY_ANIMATED, false);
        }

        if (getIntent().hasExtra(GALLERY_ALLOWS_ADDING)) {
            adding = getIntent().getBooleanExtra(GALLERY_ALLOWS_ADDING, false);
        }

        if (getIntent().hasExtra(GALLERY_FILTER)) {
            filter = Filter.valueOf(getIntent().getStringExtra(GALLERY_FILTER));
        }

        setupToolbar();
        setupFab();

        cacheWordDataSource = new CacheWordDataSource(this);

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(GalleryFlingTopEvent.class, new EventObserver<GalleryFlingTopEvent>() {
            @Override
            public void onNext(GalleryFlingTopEvent event) {
                if (animated) {
                    onBackPressed();
                }
            }
        });
        disposables.wire(MediaFileDeletedEvent.class, new EventObserver<MediaFileDeletedEvent>() {
            @Override
            public void onNext(MediaFileDeletedEvent event) {
                showToast(R.string.ra_single_media_deleted_msg);
                presenter.getFiles(filter, sort);
            }
        });

        adapter = new GalleryRecycleViewAdapter(this, this,
                new MediaFileHandler(cacheWordDataSource), R.layout.card_gallery_attachment_media_file);
        final RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);

        attachmentsAdapter = new AttachmentsRecycleViewAdapter(this, this,
                new MediaFileHandler(cacheWordDataSource), type);
        attachmentsLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        attachmentsRecyclerView.setLayoutManager(attachmentsLayoutManager);
        attachmentsRecyclerView.setAdapter(attachmentsAdapter);

        RecyclerView.ItemAnimator animator = attachmentsRecyclerView.getItemAnimator();
        if (animator != null) {
            animator.setMoveDuration(120);
            animator.setRemoveDuration(120);
        }

        presenter.getFiles(filter, sort);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.gallery_app_bar);
            if (animated) {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white);
            }
        }
    }

    private void setupFab() {
        if (animated || !adding) {
            fabButton.hide();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (selectedNum > 0) {
            getMenuInflater().inflate(R.menu.gallery_menu, menu);
            MenuItem item = menu.findItem(R.id.menu_item_tu);
            if (numOfTUServers > 0) {
                item.setVisible(true);
            } else {
                item.setVisible(false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (selectedNum > 0) {
            if (id == R.id.menu_item_delete) {
                showDeleteMediaDialog();
                return true;
            }

            if (id == R.id.menu_item_share) {
                shareMediaFiles();
                return true;
            }

            if (id == R.id.menu_item_export) {
                showExportDialog();
                return true;
            }

            if (id == R.id.menu_item_tu && numOfTUServers > 0) {
                uploadMediaFiles();
                return true;
            }

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (selectedNum == 0) {
            super.onBackPressed();
        } else {
            clearSelection();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (animated) {
            overridePendingTransition(R.anim.slide_in_up, android.R.anim.fade_out);
        }
        presenter.countTUServers();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startLocationMetadataListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (animated) {
            overridePendingTransition(android.R.anim.fade_in, R.anim.slide_out_down);
        }
    }

    @Override
    protected void onStop() {
        stopLocationMetadataListening();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (disposables != null) {
            disposables.dispose();
        }

        cacheWordDataSource.dispose();
        stopPresenter();

        if (alertDialog != null) {
            alertDialog.dismiss();
        }

        hideProgressDialog();

        dismissShareDialog();
        dismissTellaUploadDialog();

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        GalleryActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onWriteExternalStoragePermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onWriteExternalStorageNeverAskAgain() {
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void exportMediaFiles() {
        List<MediaFile> selected = adapter.getSelectedMediaFiles();
        presenter.exportMediaFiles(selected);
    }

    @OnPermissionDenied({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onCameraAndAudioPermissionDenied() {
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationPermissionDenied() {
        startAudioRecordActivityWithLocationChecked();
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationNeverAskAgain() {
        startAudioRecordActivityWithLocationChecked();
    }

    @OnNeverAskAgain({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onCameraAndAudioNeverAskAgain() {
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void startAudioRecorderActivity() {
        checkLocationSettings(C.START_AUDIO_RECORD, this::startAudioRecordActivityWithLocationChecked);
    }

    private void startAudioRecordActivityWithLocationChecked() {
        Intent intent = new Intent(this, AudioRecordActivity2.class);
        intent.putExtra(AudioRecordActivity2.RECORDER_MODE, AudioRecordActivity2.Mode.RETURN.name());
        startActivityForResult(intent, C.RECORDED_AUDIO);
    }

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    public void startCameraCaptureActivity() {
        checkLocationSettings(C.START_CAMERA_CAPTURE, this::startCameraCaptureActivityWithLocationChecked);
    }

    private void startCameraCaptureActivityWithLocationChecked() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, C.CAMERA_CAPTURE);
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showWriteExternalStorageRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.ra_media_export_rationale));
    }

    @OnShowRationale({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showCameraAndAudioRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.ra_camera_rationale));
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void showFineLocationRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(
                this, request, getString(R.string.permission_dialog_expl_GPS));
    }

    @OnClick(R.id.fab_button)
    public void importMedia() {
        MediaFileHandler.startSelectMediaActivity(this, "image/*",
                new String[]{"image/*", "video/mp4"}, C.IMPORT_MEDIA);
    }

    private boolean isLocationSettingsRequestCode(int requestCode) {
        return requestCode == C.START_CAMERA_CAPTURE ||
                requestCode == C.START_AUDIO_RECORD;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!isLocationSettingsRequestCode(requestCode) && resultCode != RESULT_OK) {
            return; // user canceled evidence acquiring
        }

        switch (requestCode) {
            case C.IMPORT_IMAGE:
                Uri image = data.getData();
                if (image != null) {
                    presenter.importImage(image);
                }
                break;

            case C.IMPORT_VIDEO:
                Uri video = data.getData();
                if (video != null) {
                    presenter.importVideo(video);
                }
                break;

            case C.IMPORT_MEDIA:
                Uri media = data.getData();
                if (media == null) break;

                String type = FileUtil.getPrimaryMime(getContentResolver().getType(media));

                if ("image".equals(type)) {
                    presenter.importImage(media);
                } else if ("video".equals(type)) {
                    presenter.importVideo(media);
                }
                break;

            case C.START_CAMERA_CAPTURE:
                startCameraCaptureActivityWithLocationChecked();
                break;

            case C.CAMERA_CAPTURE:
            case C.RECORDED_AUDIO:
                presenter.getFiles(filter, sort);
                break;
        }
    }

    @Override
    public void uploadOnServer(TellaUploadServer server, boolean metadata) {
        List<MediaFile> selected = adapter.getSelectedMediaFiles();
        long[] ids = new long[selected.size()];

        for (int i = 0; i < selected.size(); i++) {
            ids[i] = selected.get(i).getId();
        }

        Intent intent = new Intent(this, FileUploadingActivity.class);
        intent.putExtra(FileUploadingActivity.SERVER_KEY, server);
        intent.putExtra(FileUploadingActivity.FILE_KEYS, ids);
        intent.putExtra(FileUploadingActivity.METADATA, metadata);
        startActivity(intent);
    }

    @Override
    public void onGetFilesStart() {
    }

    @Override
    public void onGetFilesEnd() {
    }

    @Override
    public void onGetFilesSuccess(List<MediaFile> files) {
        blankGalleryInfo.setVisibility((files.isEmpty() && adding) ? View.VISIBLE : View.GONE);
        adapter.setFiles(files);
    }

    @Override
    public void onGetFilesError(Throwable error) {
    }

    @Override
    public void onMediaImported(MediaFile mediaFile, MediaFileThumbnailData thumbnailData) {
        presenter.addNewMediaFile(mediaFile, thumbnailData);
    }

    @Override
    public void onImportError(Throwable error) {
        showToast(R.string.ra_import_media_error);
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onImportStarted() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.ra_import_media_progress));
        fabButton.show();
    }

    @Override
    public void onImportEnded() {
        hideProgressDialog();
        fabButton.show();
        showToast(R.string.ra_file_encrypted);
    }

    @Override
    public void onMediaFilesAdded(MediaFile mediaFile) {
        showToast(R.string.ra_media_added_to_gallery);
        presenter.getFiles(filter, sort);
    }

    @Override
    public void onMediaFilesAddingError(Throwable error) {
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onMediaFilesDeleted(int num) {
        showToast(String.format(getString(R.string.ra_media_deleted_msg), num));
        presenter.getFiles(filter, sort);
    }

    @Override
    public void onMediaFilesDeletionError(Throwable throwable) {
        showToast(R.string.ra_media_deleted_error);
    }

    @Override
    public void onMediaExported(int num) {
        showToast(String.format(getString(R.string.ra_media_export_msg), num));
    }

    @Override

    public void onExportError(Throwable error) {
        showToast(R.string.ra_media_export_error);
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onExportStarted() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.ra_export_media_progress));
        fabButton.hide();
    }

    @Override
    public void onExportEnded() {
        onImportEnded();
    }

    @Override
    public void onCountTUServersEnded(Long num) {
        numOfTUServers = num;
        invalidateOptionsMenu();
    }

    @Override
    public void onCountTUServersFailed(Throwable throwable) {
    }

    /*@Override
    public void onTmpVideoEncrypted(MediaFileBundle mediaFileBundle) {
        presenter.addNewMediaFile(mediaFileBundle.getMediaFile(), mediaFileBundle.getMediaFileThumbnailData());
    }*/

    /*@Override
    public void onTmpVideoEncryptionError(Throwable error) {
        onImportError(error);
    }*/

    @Override
    public Context getContext() {
        return this;
    }


    @Override
    public void playMedia(MediaFile mediaFile) {
        if (mediaFile.getType() == MediaFile.Type.IMAGE) {
            Intent intent = new Intent(this, PhotoViewerActivity.class);
            intent.putExtra(PhotoViewerActivity.VIEW_PHOTO, mediaFile);
            startActivity(intent);
        } else if (mediaFile.getType() == MediaFile.Type.AUDIO) {
            Intent intent = new Intent(this, AudioPlayActivity.class);
            intent.putExtra(AudioPlayActivity.PLAY_MEDIA_FILE_ID_KEY, mediaFile.getId());
            startActivity(intent);
        } else if (mediaFile.getType() == MediaFile.Type.VIDEO) {
            Intent intent = new Intent(this, VideoViewerActivity.class);
            intent.putExtra(VideoViewerActivity.VIEW_VIDEO, mediaFile);
            startActivity(intent);
        }
    }

    @Override
    public void onRemoveAttachment(MediaFile mediaFile) {
        adapter.deselectMediaFile(mediaFile);
        onSelectionNumChange(attachmentsAdapter.getItemCount());
        updateAttachmentsVisibility();
    }

    @Override
    public void onSelectionNumChange(int num) {
        boolean current = selectedNum > 0, next = num > 0;
        selectedNum = num;

        if (getSupportActionBar() != null) {
            if (selectedNum > 0) {
                getSupportActionBar().setTitle(String.valueOf(selectedNum));
            } else {
                getSupportActionBar().setTitle(R.string.gallery_app_bar);
            }
        }

        if (current != next) {
            invalidateOptionsMenu();
        }
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

    private void showExportDialog() {
        alertDialog = DialogsUtil.showExportMediaDialog(this, (dialog, which) ->
                GalleryActivityPermissionsDispatcher.exportMediaFilesWithPermissionCheck(GalleryActivity.this));
    }

    private void showDeleteMediaDialog() {
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.ra_delete_media)
                .setMessage(R.string.ra_media_will_be_deleted)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> removeMediaFiles())
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void removeMediaFiles() {
        List<MediaFile> selected = adapter.getSelectedMediaFiles();
        presenter.deleteMediaFiles(selected);
        clearSelection();
    }

    private void startShareActivity(boolean includeMetadata) {
        List<MediaFile> selected = adapter.getSelectedMediaFiles();

        if (selected.size() > 1) {
            MediaFileHandler.startShareActivity(this, selected, includeMetadata);
        } else {
            MediaFileHandler.startShareActivity(this, selected.get(0), includeMetadata);
        }
    }

    private void shareMediaFiles() {
        boolean metadata = false;
        List<MediaFile> selected = adapter.getSelectedMediaFiles();

        for (MediaFile mediaFile : selected) {
            if (mediaFile.getMetadata() != null) {
                metadata = true;
                break;
            }
        }

        if (metadata) {
            ShareDialogFragment.newInstance().show(getSupportFragmentManager(), ShareDialogFragment.TAG);
        } else {
            startShareActivity(false);
        }
    }

    private void uploadMediaFiles() {
        boolean metadata = false;
        List<MediaFile> selected = adapter.getSelectedMediaFiles();

        for (MediaFile mediaFile : selected) {
            if (mediaFile.getMetadata() != null) {
                metadata = true;
                break;
            }
        }

        TellaUploadDialogFragment.newInstance(metadata).show(getSupportFragmentManager(), TellaUploadDialogFragment.TAG);
    }

    @Override
    public void onMediaSelected(MediaFile mediaFile) {
        addAttachmentsAttachment(mediaFile);
        updateAttachmentsVisibility();
    }

    @Override
    public void onMediaDeselected(MediaFile mediaFile) {
        attachmentsAdapter.removeAttachment(mediaFile);
        updateAttachmentsVisibility();
    }

    private void clearSelection() {
        adapter.clearSelected();
        attachmentsAdapter.clearAttachments();
        updateAttachmentsVisibility();
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void dismissShareDialog() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(ShareDialogFragment.TAG);
        if (fragment instanceof ShareDialogFragment) {
            ((ShareDialogFragment) fragment).dismiss();
        }
    }

    private void dismissTellaUploadDialog() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TellaUploadDialogFragment.TAG);
        if (fragment instanceof TellaUploadDialogFragment) {
            ((TellaUploadDialogFragment) fragment).dismiss();
        }
    }

    private void addAttachmentsAttachment(MediaFile mediaFile) {
        if (sort == IMediaFileRecordRepository.Sort.NEWEST) {
            attachmentsAdapter.prependAttachment(mediaFile);
            attachmentsLayoutManager.scrollToPosition(0);
        } else {
            attachmentsAdapter.appendAttachment(mediaFile);
            attachmentsLayoutManager.scrollToPosition(attachmentsAdapter.getItemCount());
        }
    }

    private void updateAttachmentsVisibility() {
        if (attachmentsAdapter.getItemCount() == 0) {
            attachmentsToolbar.setVisibility(View.GONE);
            attachmentsRecyclerView.setVisibility(View.GONE);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        } else {
            attachmentsToolbar.setVisibility(View.VISIBLE);
            attachmentsRecyclerView.setVisibility(View.VISIBLE);
            toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        }
    }

    @OnClick(R.id.popupMenu)
    public void showPopupSort(View v) {
        Context wrapper = new ContextThemeWrapper(this, R.style.GalerySortTextColor);
        final PopupMenu popup = new PopupMenu(wrapper, v);
        popup.inflate(R.menu.gallery_sort_menu);
        popup.show();

        setCheckedSort(sort, popup);
        setCheckedFilter(filter, popup);

        popup.setOnMenuItemClickListener(item -> {
            item.setChecked(true);

            if (item.getGroupId() == R.id.sort) {
                sort = getGallerySort(item.getItemId());
            } else {
                filter = getGalleryFilter(item.getItemId());
            }

            presenter.getFiles(filter, sort);
            return true;
        });
    }

    private void setCheckedSort(Sort checkedSort, PopupMenu popup) {
        if (popup.getMenu().findItem(getSortId(checkedSort)) != null) {
            popup.getMenu().findItem(getSortId(checkedSort)).setChecked(true);
        }
    }

    private void setCheckedFilter(Filter checkedFilter, PopupMenu popup) {
        if (popup.getMenu().findItem(getFilterId(checkedFilter)) != null) {
            popup.getMenu().findItem(getFilterId(checkedFilter)).setChecked(true);
        }
    }

    public Filter getGalleryFilter(final int id) {
        switch (id) {
            case R.id.photo:
                return Filter.PHOTO;

            case R.id.audio:
                return Filter.AUDIO;

            case R.id.video:
                return Filter.VIDEO;
            /*
            case R.id.files_with_metadata:
                return Filter.WITH_METADATA;

            case R.id.files_without_metadata:
                return Filter.WITHOUT_METADATA;
            */
            default:
                return Filter.ALL;
        }

    }

    public Sort getGallerySort(final int id) {
        if (id == R.id.oldest) {
            return Sort.OLDEST;
        }

        return Sort.NEWEST;
    }

    @IdRes
    public int getFilterId(Filter filter) {
        switch (filter) {
            case PHOTO:
                return R.id.photo;

            case AUDIO:
                return R.id.audio;

            case VIDEO:
                return R.id.video;
            /*
            case WITH_METADATA:
                return R.id.files_with_metadata;

            case WITHOUT_METADATA:
                return R.id.files_without_metadata;
               */
            default:
                return R.id.all;
        }
    }

    @IdRes
    public int getSortId(Sort sort) {
        if (sort == Sort.OLDEST) {
            return R.id.oldest;
        }

        return R.id.newest;
    }
}
