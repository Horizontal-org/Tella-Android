package rs.readahead.washington.mobile.views.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.RawFile;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo;
import rs.readahead.washington.mobile.mvp.contract.IFileUploadingPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.FileUploadingPresenter;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.util.ViewUtil;
import rs.readahead.washington.mobile.views.custom.FileSendButtonView;

public class FileUploadingActivity extends CacheWordSubscriberBaseActivity implements
        IFileUploadingPresenterContract.IView {
    public static final String FILE_KEYS = "fke";
    public static final String SERVER_KEY = "sk";
    public static final String METADATA = "meta";

    @BindView(R.id.fileDetailsContainer)
    NestedScrollView filesViewContainer;
    @BindView(R.id.filesList)
    LinearLayout filesList;
    @BindView(R.id.send_button)
    FileSendButtonView sendButton;
    @BindView(R.id.status_text)
    TextView statusText;

    private FileUploadingPresenter presenter;

    private TellaUploadServer server;
    private LinkedHashMap<String, RawFile> mediaFiles = new LinkedHashMap<>();
    private boolean metadata;
    private boolean uploading = false;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_file_uploading);

        ButterKnife.bind(this);

        this.toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.upload_app_bar_upload_screen);
        }

        long[] ids;

        if (getIntent().hasExtra(FILE_KEYS) && getIntent().hasExtra(SERVER_KEY) && getIntent().hasExtra(METADATA)) {
            ids = getIntent().getLongArrayExtra(FILE_KEYS);
            server = (TellaUploadServer) getIntent().getSerializableExtra(SERVER_KEY);
            metadata = getIntent().getBooleanExtra(METADATA, false);
        } else {
            showToast(R.string.gallery_toast_fail_finding_metadata_to_upload);
            finish();
            return;
        }

        statusText.setText(String.format("%s %s", getString(R.string.send_files_to), server.getName()));

        presenter = new FileUploadingPresenter(this);
        presenter.getMediaFiles(ids, metadata);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            if (presenter != null && uploading) {
                DialogsUtil.showExitFileUploadDialog(this,
                        (dialog, which) -> super.onBackPressed(),
                        (dialog, which) -> {
                        });
            } else {
                finish();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (presenter != null && uploading) {
            DialogsUtil.showExitFileUploadDialog(this,
                    (dialog, which) -> super.onBackPressed(),
                    (dialog, which) -> {
                    });
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        stopPresenter();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (presenter != null && uploading) {
            presenter.stopUploading();
        }
    }

    @OnClick(R.id.send_button)
    public void onSendClick(View view) {
        if (presenter == null) {
            return;
        }

        List<RawFile> mediaFiles = new ArrayList<>(this.mediaFiles.values());

        for (RawFile mediaFile : mediaFiles) {
            ViewGroup layout = filesList.findViewWithTag(mediaFile.getFileName());
            if (layout == null) {
                continue;
            }

            setPartCleared(layout);
        }

        presenter.uploadMediaFiles(server, mediaFiles, metadata);
    }

    @Override
    public void onGetMediaFilesSuccess(List<RawFile> mediaFiles) {
        long size = 0;

        for (RawFile mediaFile : mediaFiles) {
            this.mediaFiles.put(mediaFile.getFileName(), mediaFile);
            size += mediaFile.getSize();
        }

        if (size > 0) {
            filesList.addView(createFileListHeaderView(size, this.mediaFiles.size()));
        }

        for (RawFile mediaFile : mediaFiles) {
            filesList.addView(createFormSubmissionPartItemView(mediaFile));
        }

        sendButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onGetMediaFilesError(Throwable error) {
        Toast.makeText(this, R.string.gallery_toast_fail_finding_file_to_upload, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMediaFilesUploadStarted() {
        statusText.setText(R.string.sending);
        sendButton.setVisibility(View.GONE);
        uploading = true;
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
    }

    @Override
    public void onMediaFilesUploadProgress(UploadProgressInfo progressInfo) {
        ViewGroup layout = filesList.findViewWithTag(progressInfo.name);
        if (layout == null) {
            return;
        }

        switch (progressInfo.status) {
            case STARTED:
                setPartStarted(layout);
                break;

            case OK:
                setFileUploading(layout, progressInfo);
                break;

            case CONFLICT:
                setFileOnServer(layout);
                this.mediaFiles.remove(progressInfo.name);
                break;

            case FINISHED:
                setFileUploaded(layout);
                this.mediaFiles.remove(progressInfo.name);
                break;

            default:
                setFileError(layout);
                break;
        }
    }

    @Override
    public void onMediaFilesUploadEnded() {
        if (!this.mediaFiles.isEmpty()) {
            setRemainingErrors();
            sendButton.setVisibility(View.VISIBLE);
            sendButton.setText(R.string.retry);
        } else {
            statusText.setText(R.string.files_successfully_sent);
        }

        uploading = false;
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    private View createFileListHeaderView(long size, int num) {
        @SuppressLint("InflateParams")
        LinearLayout layout = (LinearLayout) LayoutInflater.from(getContext())
                .inflate(R.layout.files_upload_header, null);

        TextView filesNum = layout.findViewById(R.id.filesNum);
        TextView filesSize = layout.findViewById(R.id.filesSize);

        String filesNumber;
        if (num == 1) {
            filesNumber = num + " " + getString(R.string.file);
        } else {
            filesNumber = num + " " + getString(R.string.files);
        }

        filesNum.setText(filesNumber);
        filesSize.setText(FileUtil.getFileSizeString(size));

        return layout;
    }

    private View createFormSubmissionPartItemView(@NonNull RawFile mediaFile) {
        @SuppressLint("InflateParams")
        LinearLayout layout = (LinearLayout) LayoutInflater.from(getContext())
                .inflate(R.layout.file_upload_list_item, null);

        layout.setTag(mediaFile.getFileName());

        TextView nameView = layout.findViewById(R.id.fileName);
        TextView sizeView = layout.findViewById(R.id.fileSize);
        ImageView iconView = layout.findViewById(R.id.fileIcon);

        nameView.setText(mediaFile.getFileName());
        sizeView.setText(FileUtil.getFileSizeString(mediaFile.getSize()));

        int typeResId = R.drawable.ic_attach_file_black_24dp;

        if (mediaFile instanceof MediaFile) {
            switch (((MediaFile)mediaFile).getType()) {
                case IMAGE:
                    typeResId = R.drawable.ic_menu_camera;
                    break;

                case VIDEO:
                    typeResId = R.drawable.ic_videocam_black_24dp;
                    break;

                case AUDIO:
                    typeResId = R.drawable.ic_mic_black_24dp;
                    break;

                case UNKNOWN:
                default:
                    break;
            }
        }

        iconView.setImageResource(typeResId);

        return layout;
    }

    private void setPartCleared(@NonNull ViewGroup layout) {
        showUploading(layout, true);
        layout.findViewById(R.id.uploadProgress).setVisibility(View.GONE);
        layout.findViewById(R.id.uploadResultIcon).setVisibility(View.GONE);
    }

    private void setPartStarted(@NonNull ViewGroup layout) {
        showUploading(layout, true);
        layout.findViewById(R.id.uploadProgress).setVisibility(View.GONE);
        layout.findViewById(R.id.uploadResultIcon).setVisibility(View.GONE);
        layout.findViewById(R.id.uploadProgress).setVisibility(View.VISIBLE);
    }

    private void setFileUploading(@NonNull ViewGroup layout, UploadProgressInfo progressInfo) {
        showUploading(layout, true);
        layout.findViewById(R.id.uploadProgress).setVisibility(View.VISIBLE);
        layout.findViewById(R.id.uploadResultIcon).setVisibility(View.GONE);

        if (progressInfo.size > 0) {
            ProgressBar progress = layout.findViewById(R.id.uploadProgress);
            progress.setProgress((int) (progress.getMax() *
                    ((float) progressInfo.current / (float) progressInfo.size)));
        }
    }

    private void setFileUploaded(@NonNull ViewGroup layout) {
        showUploading(layout, true);
        layout.findViewById(R.id.uploadProgress).setVisibility(View.GONE);
        showUploadSuccessIcon(layout.findViewById(R.id.uploadResultIcon));
    }

    private void setFileOnServer(@NonNull ViewGroup layout) {
        showUploading(layout, false);
        showUploadSuccessIcon(layout.findViewById(R.id.uploadResultIcon));
    }

    private void setFileError(@NonNull ViewGroup layout) {
        layout.findViewById(R.id.uploadInfoLayout).setVisibility(View.VISIBLE);
        layout.findViewById(R.id.uploadProgress).setVisibility(View.GONE);

        ImageView result = layout.findViewById(R.id.uploadResultIcon);
        result.setImageDrawable(ViewUtil.getTintedDrawable(this, R.drawable.ic_error, R.color.wa_red));
        result.setVisibility(View.VISIBLE);
    }

    private void setRemainingErrors() {
        for (Map.Entry<String, RawFile> entry: this.mediaFiles.entrySet()) {
            ViewGroup layout = filesList.findViewWithTag(entry.getValue().getFileName());
            if (layout == null) {
                continue;
            }

            setFileError(layout);
        }
    }

    private void showUploadSuccessIcon(@NonNull ImageView view) {
        view.setImageResource(R.drawable.ic_check_circle_green);
        view.setVisibility(View.VISIBLE);
    }

    private void showUploading(@NonNull ViewGroup layout, boolean uploading) {
        layout.findViewById(R.id.uploadInfoLayout).setVisibility(uploading ? View.VISIBLE : View.GONE);
        layout.findViewById(R.id.fileUploadedInfo).setVisibility(uploading ? View.GONE : View.VISIBLE);
    }
}
