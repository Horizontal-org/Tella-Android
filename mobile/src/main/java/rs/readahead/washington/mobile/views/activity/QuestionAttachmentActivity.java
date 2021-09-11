package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.Activity;
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

import com.hzontal.tella_vault.VaultFile;
import com.hzontal.utils.MediaFile;

import java.util.List;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hzontal.tella_vault.VaultFile;
import com.hzontal.utils.MediaFile;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
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
    @BindView(R.id.galleryRecyclerView)
    GalleryRecyclerView recyclerView;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.attachments_blank_list_info)
    TextView blankGalleryInfo;
    private int selectedNum;
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

        galleryAdapter = new GalleryRecycleViewAdapter(this, this,
                new MediaFileHandler(),
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
            actionBar.setTitle(R.string.collect_form_select_attachment_app_bar);
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
//        Intent intent = new Intent(this, CameraActivity.class);
//        intent.putExtra(CameraActivity.CAMERA_MODE, CameraActivity.CameraMode.PHOTO.name());
//        startActivityForResult(intent, C.CAMERA_CAPTURE);
    }

    @Override
    public void playMedia(VaultFile vaultFile) {
        if (MediaFile.INSTANCE.isImageFileType(vaultFile.mimeType)) {
            Intent intent = new Intent(this, PhotoViewerActivity.class);
            intent.putExtra(PhotoViewerActivity.VIEW_PHOTO, vaultFile);
            intent.putExtra(PhotoViewerActivity.NO_ACTIONS, true);
            startActivity(intent);
        } else if (MediaFile.INSTANCE.isAudioFileType(vaultFile.mimeType)) {
            Intent intent = new Intent(this, AudioPlayActivity.class);
            intent.putExtra(AudioPlayActivity.PLAY_MEDIA_FILE_ID_KEY, vaultFile.id);
            intent.putExtra(AudioPlayActivity.NO_ACTIONS, true);
            startActivity(intent);
        } else if (MediaFile.INSTANCE.isVideoFileType(vaultFile.mimeType)) {
            Intent intent = new Intent(this, VideoViewerActivity.class);
            intent.putExtra(VideoViewerActivity.VIEW_VIDEO, vaultFile);
            intent.putExtra(VideoViewerActivity.NO_ACTIONS, true);
            startActivity(intent);
        }
    }

    @Override
    public void onRemoveAttachment(VaultFile vaultFile) {
        galleryAdapter.deselectMediaFile(vaultFile);
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
//                if (data == null) break;
//
//                long mediaFileId = data.getLongExtra(C.CAPTURED_MEDIA_FILE_ID, 0);
//                if (mediaFileId == 0) break;
//
//                presenter.addRegisteredMediaFile(mediaFileId);

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
    public void onMediaSelected(VaultFile vaultFile) {
        presenter.setAttachment(vaultFile);
    }

    @Override
    public void onMediaDeselected(VaultFile vaultFile) {
        presenter.setAttachment(null); // should be only one
    }

    @Override
    public void onGetFilesStart() {
    }

    @Override
    public void onGetFilesEnd() {
    }

    @Override
    public void onGetFilesSuccess(List<VaultFile> files) {
        blankGalleryInfo.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
        galleryAdapter.setFiles(files);
    }

    @Override
    public void onGetFilesError(Throwable error) {
        Timber.d(error);
    }

    @Override
    public void onMediaFileAdded(VaultFile vaultFile) {
        presenter.getFiles(filter, sort);
    }

    @Override
    public void onMediaFileAddError(Throwable error) {
        showToast(R.string.collect_toast_fail_attaching_file_to_form);
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onMediaFileImported(VaultFile vaultFile) {
        presenter.addNewMediaFile(vaultFile);
    }

    @Override
    public void onImportError(Throwable error) {
        showToast(R.string.gallery_toast_fail_importing_file);
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onImportStarted() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.gallery_dialog_expl_encrypting));
    }

    @Override
    public void onImportEnded() {
        hideProgressDialog();
        showToast(R.string.gallery_toast_file_encrypted);
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

        VaultFile vaultFile = (VaultFile) getIntent().getSerializableExtra(MEDIA_FILE_KEY);

        if (vaultFile != null ) {
            presenter.setAttachment(vaultFile);
            galleryAdapter.selectMediaFile(vaultFile);
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
