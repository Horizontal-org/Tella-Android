package rs.readahead.washington.mobile.views.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.FileUploadProgressEvent;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.domain.entity.FileUploadInstance;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo;
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.TellaFileUploadPresenter;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.adapters.UploadInformationRecycleViewAdapter;

public class UploadInformationActivity extends CacheWordSubscriberBaseActivity implements
        ITellaFileUploadPresenterContract.IView, UploadInformationRecycleViewAdapter.UploadInformationInterface {
    public static final String SECTION_SET = "SET";

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.number_of_files)
    TextView numberOfFilesText;
    @BindView(R.id.started_text)
    TextView startedText;
    @BindView(R.id.completed_text)
    TextView completedText;

    private AlertDialog alertDialog;
    private long set;
    private List<FileUploadInstance> instances;

    private TellaFileUploadPresenter presenter;
    private UploadInformationRecycleViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_upload_information);
        ButterKnife.bind(this);

        CacheWordDataSource cacheWordDataSource = new CacheWordDataSource(this);

        EventCompositeDisposable disposables = MyApplication.bus().createCompositeDisposable();

        disposables.wire(FileUploadProgressEvent.class, new EventObserver<FileUploadProgressEvent>() {
            @Override
            public void onNext(FileUploadProgressEvent event) {
                onProgressUpdateEvent(event.getProgress());
            }
        });


        adapter = new UploadInformationRecycleViewAdapter(this, new MediaFileHandler(cacheWordDataSource), this);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);

        presenter = new TellaFileUploadPresenter(this);

        setupToolbar();

        if (getIntent().hasExtra(SECTION_SET)) {
            this.set = getIntent().getLongExtra(SECTION_SET, 0);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.upload_information);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.menu_item_delete) {
            showClearHistoryDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (alertDialog != null) {
            alertDialog.dismiss();
        }

        stopPresenter();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        if (set != 0) {
            presenter.getFileUploadSetInstances(set);
        } else {
            onBackPressed();
        }
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.upload_information_menu, menu);

        return true;
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
        }
    }

    private void showClearUploadDialog(long id) {
        alertDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setMessage(R.string.stop_upload_dialog)
                .setPositiveButton(R.string.clear, (dialog, which) -> presenter.deleteFileUploadInstance(id))
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();
    }

    private void showClearHistoryDialog() {
        alertDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete_upload_information)
                .setPositiveButton(R.string.delete, (dialog, which) -> clearUploadInformation())
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();
    }

    private void clearUploadInformation() {
        presenter.deleteFileUploadInstances(this.set);
        onBackPressed();
    }

    @Override
    public void onGetFileUploadSetInstancesSuccess(List<FileUploadInstance> newInstances) {
        this.instances = newInstances;
        adapter.setInstances(instances);
        updateHeaderView(instances);
    }

    @Override
    public void onGetFileUploadSetInstancesError(Throwable error) {

    }

    @Override
    public void onGetFileUploadInstancesSuccess(List<FileUploadInstance> instances) {

    }

    @Override
    public void onGetFileUploadInstancesError(Throwable error) {

    }

    @Override
    public void onFileUploadInstancesDeleted() {
        presenter.getFileUploadSetInstances(set);
    }

    @Override
    public void onFileUploadInstancesDeletionError(Throwable throwable) {

    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void clearUpload(long id) {
        showClearUploadDialog(id);
    }

    @Override
    public void onMediaViewItemClicked(MediaFile mediaFile) {
        if (mediaFile != null) {
            playMedia(mediaFile);
        }
    }

    private void playMedia(MediaFile mediaFile) {
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

    private void onProgressUpdateEvent(UploadProgressInfo progress) {
        if (instances == null) {
            return;
        }
        for (int i = 0; i < instances.size(); i++) {
            if (instances.get(i).getMediaFile() == null) {
                continue;
            }
            if (instances.get(i).getMediaFile().getFileName().equals(progress.name)) {
                if (progress.status == UploadProgressInfo.Status.FINISHED) {
                    instances.get(i).setStatus(ITellaUploadsRepository.UploadStatus.UPLOADED);
                } else {
                    instances.get(i).setUploaded(progress.current);
                    instances.get(i).setSize(progress.size);
                }
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private void updateHeaderView(List<FileUploadInstance> instances) {
        long timeStarted = instances.get(0).getStarted();
        long timeCompleted = instances.get(0).getUpdated();
        boolean completed = true;
        for (FileUploadInstance instance : instances) {
            timeStarted = Math.min(instance.getStarted(), timeStarted);
            timeCompleted = Math.max(instance.getUpdated(), timeCompleted);
            if (instance.getStatus() != ITellaUploadsRepository.UploadStatus.UPLOADED) {
                completed = false;
            }
        }
        numberOfFilesText.setText(String.format("%s: %d", getContext().getResources().getString(R.string.number_of_files), instances.size()));
        startedText.setText(String.format("%s: %s", getContext().getResources().getString(R.string.started), Util.getDateTimeString(timeStarted, "dd/MM/yyyy h:mm a")));
        completedText.setText(String.format("%s: %s", getContext().getResources().getString(R.string.completed), Util.getDateTimeString(timeCompleted, "dd/MM/yyyy h:mm a")));
        completedText.setVisibility(completed ? View.VISIBLE : View.GONE);
    }
}
