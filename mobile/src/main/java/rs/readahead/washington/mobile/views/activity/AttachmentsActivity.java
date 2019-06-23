package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
import rs.readahead.washington.mobile.media.MediaFileBundle;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IAttachmentsPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.AttachmentsPresenter;
import rs.readahead.washington.mobile.presentation.entity.MediaFilesData;
import rs.readahead.washington.mobile.presentation.entity.ViewType;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.views.adapters.AttachmentsRecycleViewAdapter;
import rs.readahead.washington.mobile.views.adapters.GalleryRecycleViewAdapter;
import rs.readahead.washington.mobile.views.custom.GalleryRecyclerView;
import rs.readahead.washington.mobile.views.interfaces.IAttachmentsMediaHandler;
import rs.readahead.washington.mobile.views.interfaces.IGalleryMediaHandler;
import timber.log.Timber;


@RuntimePermissions
public class AttachmentsActivity extends MetadataActivity implements
        IAttachmentsPresenterContract.IView,
        IAttachmentsMediaHandler,
        IGalleryMediaHandler {
    public static final String MEDIA_FILES_KEY = "mfk";
    public static final String GALLERY_ANIMATED = "ga";
    public static final String REPORT_VIEW_TYPE = "type";
    private boolean animated = false;

    @BindView(R.id.galleryRecyclerView)
    GalleryRecyclerView recyclerView;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.attachmentsToolbar)
    View attachmentsToolbar;
    @BindView(R.id.attachmentsRecyclerView)
    RecyclerView attachmentsRecyclerView;
    @BindView(R.id.menu)
    FloatingActionMenu fabMenu;
    @BindView(R.id.attachments_blank_list_info)
    TextView blankGalleryInfo;

    private AttachmentsPresenter presenter;

    private GalleryRecycleViewAdapter galleryAdapter;
    private AttachmentsRecycleViewAdapter attachmentsAdapter;
    private RecyclerView.LayoutManager attachmentsLayoutManager;

    private ProgressDialog progressDialog;
    private IMediaFileRecordRepository.Filter filter = IMediaFileRecordRepository.Filter.ALL;
    private IMediaFileRecordRepository.Sort sort = IMediaFileRecordRepository.Sort.NEWEST;
    private ViewType type = ViewType.EDIT;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_attachments);
        ButterKnife.bind(this);

        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);

        if (getIntent().hasExtra(GALLERY_ANIMATED)) {
            animated = getIntent().getBooleanExtra(GALLERY_ANIMATED, false);
        }

        if (getIntent().hasExtra(REPORT_VIEW_TYPE)) {
            this.type = (ViewType) getIntent().getSerializableExtra(REPORT_VIEW_TYPE);
        }

        startPresenter();
        setupToolbar();
        setupFab();

        CacheWordDataSource cacheWordDataSource = new CacheWordDataSource(this);

        galleryAdapter = new GalleryRecycleViewAdapter(this, this,
                new MediaFileHandler(cacheWordDataSource),
                R.layout.card_gallery_attachment_media_file,
                type != ViewType.PREVIEW, false);
        RecyclerView.LayoutManager galleryLayoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(galleryLayoutManager);
        recyclerView.setAdapter(galleryAdapter);

        attachmentsAdapter = new AttachmentsRecycleViewAdapter(this, this,
                new MediaFileHandler(cacheWordDataSource), type);
        attachmentsLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        attachmentsRecyclerView.setLayoutManager(attachmentsLayoutManager);
        attachmentsRecyclerView.setAdapter(attachmentsAdapter);

        (attachmentsRecyclerView.getItemAnimator()).setMoveDuration(120);
        (attachmentsRecyclerView.getItemAnimator()).setRemoveDuration(120);

        getAttachmentsFromIntent();

        presenter.getFiles(filter, sort);

        updateFabMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (type != ViewType.PREVIEW) {
            getMenuInflater().inflate(R.menu.attachments_menu, menu);
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

        if (id == R.id.menu_item_select) {
            setResultAndFinish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        destroyPresenter();
        super.onDestroy();
    }

    @OnClick(R.id.popupMenu)
    public void showPopupSort(View view) {
        Context wrapper = new ContextThemeWrapper(this, R.style.GalerySortTextColor);
        final PopupMenu popup = new PopupMenu(wrapper, view);
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


    private void setupToolbar() {
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.ra_attachments);
            if (animated) {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white);
            }
        }
    }

    private void setupFab() {
        if (animated || type == ViewType.PREVIEW) {
            fabMenu.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AttachmentsActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnPermissionDenied({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onCameraAndAudioPermissionDenied() {
    }

    @OnNeverAskAgain({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onCameraAndAudioNeverAskAgain() {
    }

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    public void startCameraCaptureActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra(CameraActivity.CAMERA_MODE, CameraActivity.Mode.PHOTO.name());
        startActivityForResult(intent, C.CAMERA_CAPTURE);
    }

    @OnClick(R.id.camera_capture)
    public void recordVideoClick(View view) {
        fabMenu.close(true);
        startCameraCaptureActivity();
    }

    @OnClick(R.id.record_audio)
    public void recordAudioClick(View view) {
        fabMenu.close(true);
        startActivityForResult(new Intent(this, AudioRecordActivity2.class), C.RECORDED_AUDIO);
    }

    @OnClick(R.id.import_photo_from_device)
    public void importPhotoClick(View view) {
        fabMenu.close(true);
        MediaFileHandler.startSelectMediaActivity(this, "image/*", null, C.IMPORT_IMAGE);
    }

    @OnClick(R.id.import_video_from_device)
    public void importVideoClick(View view) {
        fabMenu.close(true);
        MediaFileHandler.startSelectMediaActivity(this, "video/mp4", null, C.IMPORT_VIDEO);
    }

    @OnClick(R.id.import_media_from_device)
    public void importMediaClick(View view) {
        fabMenu.close(true);
        MediaFileHandler.startSelectMediaActivity(this, "image/*",
                new String[]{"image/*", "video/mp4"}, C.IMPORT_MEDIA);
    }

    @Override
    public void playMedia(MediaFile mediaFile) {
        if (mediaFile.getType() == MediaFile.Type.IMAGE) {
            Intent intent = new Intent(this, PhotoViewerActivity.class);
            intent.putExtra(PhotoViewerActivity.VIEW_PHOTO, mediaFile);
            intent.putExtra(PhotoViewerActivity.NO_ACTIONS, true);
            startActivity(intent);
        } else if (mediaFile.getType() == MediaFile.Type.AUDIO) {
            Intent intent = new Intent(this, AudioPlayActivity.class);
            intent.putExtra(AudioPlayActivity.PLAY_MEDIA_FILE_ID_KEY, mediaFile.getId());
            intent.putExtra(AudioPlayActivity.NO_ACTIONS, true);
            startActivity(intent);
        } else if (mediaFile.getType() == MediaFile.Type.VIDEO) {
            Intent intent = new Intent(this, VideoViewerActivity.class);
            intent.putExtra(VideoViewerActivity.VIEW_VIDEO, mediaFile);
            intent.putExtra(VideoViewerActivity.NO_ACTIONS, true);
            startActivity(intent);
        }
    }

    @Override
    public void onRemoveAttachment(MediaFile mediaFile) {
        galleryAdapter.deselectMediaFile(mediaFile);
        updateAttachmentsVisibility();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
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

            case C.CAMERA_CAPTURE:
            case C.RECORDED_AUDIO:
                if (data == null) break;

                long mediaFileId = data.getLongExtra(C.CAPTURED_MEDIA_FILE_ID, 0);
                if (mediaFileId == 0) break;

                presenter.attachRegisteredEvidence(mediaFileId);

                break;
        }
    }

    @Override
    public void onSelectionNumChange(int num) {
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

    @Override
    public void onGetFilesStart() {
    }

    @Override
    public void onGetFilesEnd() {
    }

    @Override
    public void onGetFilesSuccess(List<MediaFile> files) {
        blankGalleryInfo.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
        galleryAdapter.setFiles(files);
    }

    @Override
    public void onGetFilesError(Throwable error) {
    }

    @Override
    public void onEvidenceAttached(MediaFile mediaFile) {
        showToast(getString(R.string.ra_media_attached_to_report)); // "Media attached"

        addAttachmentsAttachment(mediaFile);
        updateAttachmentsVisibility();

        galleryAdapter.selectMediaFile(mediaFile);
        presenter.getFiles(filter, sort);
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
        } else {
            attachmentsToolbar.setVisibility(View.VISIBLE);
            attachmentsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onEvidenceAttachedError(Throwable error) {
        showToast(R.string.ra_media_form_attach_error);
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onEvidenceImported(MediaFileBundle mediaFileBundle) {
        presenter.attachNewEvidence(mediaFileBundle);
    }

    @Override
    public void onImportError(Throwable error) {
        showToast(R.string.ra_import_media_error);
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onImportStarted() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.ra_import_media_progress));
        fabMenu.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onImportEnded() {
        hideProgressDialog();
        fabMenu.setVisibility(View.VISIBLE);
        showToast(R.string.ra_file_encrypted);
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void startPresenter() {
        presenter = new AttachmentsPresenter(this);
    }

    private void getAttachmentsFromIntent() {
        if (!getIntent().hasExtra(MEDIA_FILES_KEY)) {
            return;
        }

        MediaFilesData mediaFilesData = (MediaFilesData) getIntent().getSerializableExtra(MEDIA_FILES_KEY);
        List<MediaFile> attachments = mediaFilesData.getMediaFiles();

        presenter.setAttachments(attachments); // todo: connect presenter to all views
        attachmentsAdapter.setAttachments(presenter.getAttachments());

        galleryAdapter.setSelectedMediaFiles(presenter.getAttachments());

        updateAttachmentsVisibility();
    }

    private void destroyPresenter() {
        if (presenter != null) {
            presenter.destroy();
        }
        presenter = null;
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void updateFabMenu() {
        boolean media = Build.VERSION.SDK_INT >= 19;

        fabMenu.findViewById(R.id.import_photo_from_device).setVisibility(media ? View.GONE : View.VISIBLE);
        fabMenu.findViewById(R.id.import_video_from_device).setVisibility(media ? View.GONE : View.VISIBLE);
        fabMenu.findViewById(R.id.import_media_from_device).setVisibility(media ? View.VISIBLE : View.GONE);

        fabMenu.setClosedOnTouchOutside(true);
    }

    private void setCheckedSort(IMediaFileRecordRepository.Sort checkedSort, PopupMenu popup) {
        if (popup.getMenu().findItem(getSortId(checkedSort)) != null) {
            popup.getMenu().findItem(getSortId(checkedSort)).setChecked(true);
        }
    }

    private void setCheckedFilter(IMediaFileRecordRepository.Filter checkedFilter, PopupMenu popup) {
        if (popup.getMenu().findItem(getFilterId(checkedFilter)) != null) {
            popup.getMenu().findItem(getFilterId(checkedFilter)).setChecked(true);
        }
    }

    public IMediaFileRecordRepository.Filter getGalleryFilter(final int id) {
        switch (id) {
            case R.id.photo:
                return IMediaFileRecordRepository.Filter.PHOTO;

            case R.id.audio:
                return IMediaFileRecordRepository.Filter.AUDIO;

            case R.id.video:
                return IMediaFileRecordRepository.Filter.VIDEO;

       /*     case R.id.files_with_metadata:
                return IMediaFileRecordRepository.Filter.WITH_METADATA;

            case R.id.files_without_metadata:
                return IMediaFileRecordRepository.Filter.WITHOUT_METADATA;
*/
            default:
                return IMediaFileRecordRepository.Filter.ALL;
        }
    }

    public IMediaFileRecordRepository.Sort getGallerySort(final int id) {
        switch (id) {
            case R.id.oldest:
                return IMediaFileRecordRepository.Sort.OLDEST;

            default:
                return IMediaFileRecordRepository.Sort.NEWEST;
        }
    }

    @IdRes
    public int getFilterId(IMediaFileRecordRepository.Filter filter) {
        switch (filter) {
            case PHOTO:
                return R.id.photo;

            case AUDIO:
                return R.id.audio;

            case VIDEO:
                return R.id.video;

            /*case WITH_METADATA:
                return R.id.files_with_metadata;

            case WITHOUT_METADATA:
                return R.id.files_without_metadata;
            */
            default:
                return R.id.all;
        }
    }

    @IdRes
    public int getSortId(IMediaFileRecordRepository.Sort sort) {
        switch (sort) {
            case OLDEST:
                return R.id.oldest;

            default:
                return R.id.newest;
        }
    }

    private void setResultAndFinish() {
        setResult(Activity.RESULT_OK, new Intent().putExtra(MEDIA_FILES_KEY, attachmentsAdapter.getAttachments()));
        finish();
    }
}
