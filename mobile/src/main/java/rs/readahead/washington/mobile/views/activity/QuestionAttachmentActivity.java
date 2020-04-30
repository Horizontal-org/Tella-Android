package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import rs.readahead.washington.mobile.mvp.contract.IQuestionAttachmentPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.QuestionAttachmentPresenter;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.views.adapters.GalleryRecycleViewAdapter;
import rs.readahead.washington.mobile.views.custom.GalleryRecyclerView;
import rs.readahead.washington.mobile.views.interfaces.IAttachmentsMediaHandler;
import rs.readahead.washington.mobile.views.interfaces.IGalleryMediaHandler;
import timber.log.Timber;


@RuntimePermissions
public class QuestionAttachmentActivity extends MetadataActivity implements
        IQuestionAttachmentPresenterContract.IView,
        IAttachmentsMediaHandler,
        IGalleryMediaHandler {
    public static final String MEDIA_FILE_KEY = "mfk";
    public static final String MEDIA_FILES_FILTER = "mff";
    private int selectedNum;

    @BindView(R.id.galleryRecyclerView)
    GalleryRecyclerView recyclerView;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.attachments_blank_list_info)
    TextView blankGalleryInfo;

    private QuestionAttachmentPresenter presenter;

    private GalleryRecycleViewAdapter galleryAdapter;

    private ProgressDialog progressDialog;
    private IMediaFileRecordRepository.Filter filter;
    private IMediaFileRecordRepository.Sort sort = IMediaFileRecordRepository.Sort.NEWEST;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_question_attachment);
        ButterKnife.bind(this);

        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);

        if (getIntent().hasExtra(MEDIA_FILES_FILTER)) {
            this.filter = (IMediaFileRecordRepository.Filter) getIntent().getSerializableExtra(MEDIA_FILES_FILTER);
        } else {
            throw new IllegalArgumentException();
        }

        startPresenter();
        setupToolbar();
        setupFab();

        CacheWordDataSource cacheWordDataSource = new CacheWordDataSource(this);

        galleryAdapter = new GalleryRecycleViewAdapter(this, this,
                new MediaFileHandler(cacheWordDataSource),
                R.layout.card_gallery_attachment_media_file,
                true, true);
        RecyclerView.LayoutManager galleryLayoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(galleryLayoutManager);
        recyclerView.setAdapter(galleryAdapter);

        getSelectedMediaFromIntent();

        presenter.getFiles(filter, sort);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (selectedNum > 0) {
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

        if (selectedNum > 0) {
            if (id == R.id.menu_item_select) {
                setResultAndFinish();
                return true;
            }
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
        popup.inflate(R.menu.question_attachment_sort_menu);
        popup.show();

        setCheckedSort(sort, popup);

        popup.setOnMenuItemClickListener(item -> {
            item.setChecked(true);

            if (item.getGroupId() == R.id.sort) {
                sort = getGallerySort(item.getItemId());
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
            actionBar.setTitle(R.string.ra_question_attachment_activity_title);
        }
    }

    private void setupFab() {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        QuestionAttachmentActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
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
        intent.putExtra(CameraActivity.CAMERA_MODE, CameraActivity.CameraMode.PHOTO.name());
        startActivityForResult(intent, C.CAMERA_CAPTURE);
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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

            case C.CAMERA_CAPTURE:
            case C.RECORDED_AUDIO:
                if (data == null) break;

                long mediaFileId = data.getLongExtra(C.CAPTURED_MEDIA_FILE_ID, 0);
                if (mediaFileId == 0) break;

                presenter.addRegisteredMediaFile(mediaFileId);

                break;
        }
    }

    @Override
    public void onSelectionNumChange(int num) {
        boolean current = selectedNum > 0, next = num > 0;
        selectedNum = num;

        if (current != next) {
            invalidateOptionsMenu();
        }
    }

    @Override
    public void onMediaSelected(MediaFile mediaFile) {
        presenter.setAttachment(mediaFile);
    }

    @Override
    public void onMediaDeselected(MediaFile mediaFile) {
        presenter.setAttachment(null); // should be only one
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
    public void onMediaFileAdded(MediaFile mediaFile) {
        presenter.getFiles(filter, sort);
    }

    @Override
    public void onMediaFileAddError(Throwable error) {
        showToast(R.string.ra_media_form_attach_error);
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onMediaFileImported(MediaFileBundle mediaFileBundle) {
        presenter.addNewMediaFile(mediaFileBundle);
    }

    @Override
    public void onImportError(Throwable error) {
        showToast(R.string.ra_import_media_error);
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onImportStarted() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.ra_import_media_progress));
    }

    @Override
    public void onImportEnded() {
        hideProgressDialog();
        showToast(R.string.ra_file_encrypted);
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void startPresenter() {
        presenter = new QuestionAttachmentPresenter(this);
    }

    private void getSelectedMediaFromIntent() {
        if (!getIntent().hasExtra(MEDIA_FILE_KEY)) {
            return;
        }

        MediaFile mediaFile = (MediaFile) getIntent().getSerializableExtra(MEDIA_FILE_KEY);

        if (!MediaFile.NONE.equals(mediaFile)) {
            presenter.setAttachment(mediaFile);
            galleryAdapter.selectMediaFile(mediaFile);
            onSelectionNumChange(1);
        }
    }

    private void clearSelection() {
        galleryAdapter.clearSelected();
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

    private void setCheckedSort(IMediaFileRecordRepository.Sort checkedSort, PopupMenu popup) {
        if (popup.getMenu().findItem(getSortId(checkedSort)) != null) {
            popup.getMenu().findItem(getSortId(checkedSort)).setChecked(true);
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
    public int getSortId(IMediaFileRecordRepository.Sort sort) {
        switch (sort) {
            case OLDEST:
                return R.id.oldest;

            default:
                return R.id.newest;
        }
    }

    private void setResultAndFinish() {
        setResult(Activity.RESULT_OK, new Intent().putExtra(MEDIA_FILE_KEY, presenter.getAttachment()));
        finish();
    }
}
