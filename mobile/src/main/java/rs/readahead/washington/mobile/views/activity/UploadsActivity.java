package rs.readahead.washington.mobile.views.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.FileUploadProgressEvent;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.FileUploadInstance;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo;
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.TellaFileUploadPresenter;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.util.jobs.TellaUploadJob;
import rs.readahead.washington.mobile.views.adapters.SectionItemOffset;
import rs.readahead.washington.mobile.views.adapters.UploadSection;
import rs.readahead.washington.mobile.views.custom.StopResumeUploadButton;


public class UploadsActivity extends CacheWordSubscriberBaseActivity implements
        UploadSection.UploadSectionListener,
        ITellaFileUploadPresenterContract.IView {
    @BindView(R.id.uploadsRecyclerView)
    RecyclerView uploadsRecyclerView;
    @BindView(R.id.empty_uploads_text)
    TextView emptyInfo;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.header_status_bar)
    View headerStatus;
    @BindView(R.id.header_text)
    TextView headerText;
    @BindView(R.id.status_text)
    TextView statusText;
    @BindView(R.id.started_text)
    TextView startedText;
    @BindView(R.id.stop_outlined)
    StopResumeUploadButton stopOutlined;

    private TellaFileUploadPresenter presenter;
    private CacheWordDataSource cacheWordDataSource;
    private EventCompositeDisposable disposables;
    private SectionedRecyclerViewAdapter sectionedAdapter;
    private AlertDialog alertDialog;
    private boolean uploadsExist;
    private final int spanCount = 5;
    private long uploadingSet;
    private long lastUpdateTimeStamp = 0;
    private long lastUploadedSize = 0;
    private static final int spacing = 50; // 50px
    private boolean uploadFinished;

    //
    private List<FileUploadInstance> uploadnigList;
    private static final long REFRESH_TIME_MS = 500;
    private long total = 0;
    private long uploaded = 0;
    private long newUpdateTimestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_uploads);
        ButterKnife.bind(this);

        presenter = new TellaFileUploadPresenter(this);

        setupToolbar();

        cacheWordDataSource = new CacheWordDataSource(this);

        disposables = MyApplication.bus().createCompositeDisposable();

        disposables.wire(FileUploadProgressEvent.class, new EventObserver<FileUploadProgressEvent>() {
            @Override
            public void onNext(FileUploadProgressEvent event) {
                onProgressUpdateEvent(event.getProgress());
            }
        });

        sectionedAdapter = new SectionedRecyclerViewAdapter();
        final GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), spanCount);

        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(final int position) {
                if (sectionedAdapter.getSectionItemViewType(position) == SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER ||
                        sectionedAdapter.getSectionItemViewType(position) == SectionedRecyclerViewAdapter.VIEW_TYPE_FOOTER) {
                    return spanCount;
                }
                return 1;
            }
        });

        uploadsRecyclerView.setLayoutManager(gridLayoutManager);
        uploadsRecyclerView.setAdapter(sectionedAdapter);
        uploadsRecyclerView.addItemDecoration(new SectionItemOffset(spacing));

        setupStopResumeButton();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.home_upload_button);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.menu_item_clear) {
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
        if (disposables != null) {
            disposables.dispose();
        }

        if (alertDialog != null) {
            alertDialog.dismiss();
        }

        cacheWordDataSource.dispose();
        stopPresenter();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        sectionedAdapter.removeAllSections();
        presenter.getFileUploadInstances();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (uploadsExist) {
            getMenuInflater().inflate(R.menu.uploads_menu, menu);
        }

        return true;
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
        }
    }

    @Override
    public void onGetFileUploadInstancesSuccess(List<FileUploadInstance> instances) {
        sectionedAdapter.removeAllSections();
        headerStatus.setVisibility(View.GONE);
        List<FileUploadInstance> setInstances = new ArrayList<>();
        boolean uploaded = true;
        long set = instances.get(0).getSet();
        this.uploadsExist = instances.size() > 0;

        if (uploadsExist) {
            emptyInfo.setVisibility(View.GONE);
            uploadsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            uploadsRecyclerView.setVisibility(View.GONE);
            emptyInfo.setVisibility(View.VISIBLE);
        }

        for (FileUploadInstance instance : instances) {
            if (instance.getStatus().ordinal() > ITellaUploadsRepository.UploadStatus.UPLOADED.ordinal() && uploaded) {
                uploaded = false; // if any of instances in the set is not uploaded -> set is not uploaded
                uploadingSet = set;
            }
            if (set != instance.getSet()) {
                insertSection(setInstances, uploaded, set);
                uploaded = true;
                set = instance.getSet();
                setInstances = new ArrayList<>();
            }
            setInstances.add(instance);
        }
        insertSection(setInstances, uploaded, set);
        invalidateOptionsMenu();
    }

    @Override
    public void onGetFileUploadInstancesError(Throwable error) {

    }

    @Override
    public void onGetFileUploadSetInstancesSuccess(List<FileUploadInstance> instances) {
        uploadnigList = instances;
        refreshUploadingProgressData();
    }

    @Override
    public void onGetFileUploadSetInstancesError(Throwable error) {

    }

    @Override
    public void onFileUploadInstancesDeleted() {
        headerStatus.setVisibility(View.GONE);
        sectionedAdapter.removeAllSections();
        presenter.getFileUploadInstances();
    }

    @Override
    public void onFileUploadInstancesDeletionError(Throwable throwable) {

    }

    @Override
    public Context getContext() {
        return this;
    }

    public void onStopClicked() {
        if (Preferences.isAutoUploadPaused()) {
            resumeUpload();
        } else {
            showClearScheduledDialog();
        }
    }

    public void showClearScheduledDialog() {
        alertDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.upload_main_stop_upload_dialog_expl)
                .setPositiveButton(R.string.action_stop, (dialog, which) -> pauseUpload())
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();
    }

    @Override
    public void showUploadInformation(long set) {
        startUploadInformationActivity(set);
    }

    @Override
    public void onItemRootViewClicked(MediaFile mediaFile) {
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

    private void pauseUpload() {
        Preferences.setAutoUploadPased(true);
        statusText.setText(getContext().getResources().getString(R.string.upload_main_heading_stopped));
        stopOutlined.setStopped();
        stopOutlined.button.setOnClickListener(v -> onStopClicked());
    }

    private void resumeUpload() {
        Preferences.setAutoUploadPased(false);
        headerText.setText(getContext().getResources().getString(R.string.upload_main_meta_status_atempting));
        statusText.setText("");
        setProgress();
        stopOutlined.donutProgress.setOnClickListener(v -> onStopClicked());
        TellaUploadJob.scheduleJob();
    }

    private void setupStopResumeButton() {
        if (Preferences.isAutoUploadPaused()) {
            stopOutlined.setStopped();
            stopOutlined.button.setOnClickListener(v -> onStopClicked());
            statusText.setText(getContext().getResources().getString(R.string.upload_main_heading_stopped));
        } else {
            setProgress();
            stopOutlined.donutProgress.setOnClickListener(v -> onStopClicked());
        }
    }

    private void insertSection(List<FileUploadInstance> setInstances, boolean uploaded, long set) {
        UploadSection section = new UploadSection(getContext(), new MediaFileHandler(cacheWordDataSource), setInstances, this, set);
        if (!uploaded) {
            setUploadingHeader(setInstances);
            uploadnigList = setInstances;
            refreshUploadingProgressData();
        }
        sectionedAdapter.addSection(section);
        sectionedAdapter.notifyDataSetChanged();
    }

    private void startUploadInformationActivity(long set) {
        Intent intent = new Intent(this, UploadInformationActivity.class);
        intent.putExtra(UploadInformationActivity.SECTION_SET, set);
        startActivity(intent);
    }

    private void showClearHistoryDialog() {
        alertDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.upload_main_dialog_expl_clear_history)
                .setPositiveButton(R.string.upload_main_dialog_action_clear, (dialog, which) -> clearHistory())
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();
    }

    private void onProgressUpdateEvent(UploadProgressInfo progress) {
        long now = Util.currentTimestamp();
        if (now - lastUpdateTimeStamp < REFRESH_TIME_MS && lastUpdateTimeStamp > 0 && progress.status != UploadProgressInfo.Status.FINISHED) {   //
            return;
        }
        presenter.getFileUploadSetInstances(uploadingSet);
    }

    private void setUploadingHeader(List<FileUploadInstance> instances) {
        if (uploadFinished) {
            return;
        }
        long started = instances.get(0).getStarted();
        lastUpdateTimeStamp = instances.get(0).getUpdated();
        lastUploadedSize = 0;
        headerStatus.setVisibility(View.VISIBLE);
        for (FileUploadInstance instance : instances) {
            if (instance.getStarted() < started) {
                started = instance.getStarted();
            }
            if (instance.getUpdated() > lastUpdateTimeStamp) {
                lastUpdateTimeStamp = instance.getUpdated();
            }
            lastUploadedSize += instance.getUploaded();
        }
        startedText.setText(String.format("%s: %s", getContext().getResources().getString(R.string.upload_meta_date_started), Util.getDateTimeString(started, "dd/MM/yyyy h:mm a")));
        if (Preferences.isAutoUploadPaused()) {
            statusText.setText(String.format("%s %s",
                    getContext().getResources().getQuantityString(R.plurals.upload_main_meta_number_of_files, instances.size(), instances.size()), getContext().getResources().getString(R.string.upload_main_heading_stopped)));
        } else {
            statusText.setText(String.format("%s %s",
                    getContext().getResources().getQuantityString(R.plurals.upload_main_meta_number_of_files, instances.size(), instances.size()), ""));
        }
    }

    private void refreshUploadingProgressData() {
        long started = uploadnigList.get(0).getStarted();
        uploadFinished = true;
        total = 0;
        uploaded = 0;

        for (int i = 0; i < uploadnigList.size(); i++) {
            FileUploadInstance instance = uploadnigList.get(i);
            if (instance.getStarted() < started) {
                started = instance.getStarted();
            }
            if (instance.getStatus() != ITellaUploadsRepository.UploadStatus.UPLOADED) {
                uploadFinished = false;
            } else {
                sectionedAdapter.notifyDataSetChanged();
            }

            if (instance.getUpdated() > newUpdateTimestamp) {
                newUpdateTimestamp = instance.getUpdated();
            }

            if (instance.getStatus() == ITellaUploadsRepository.UploadStatus.UPLOADED) {
                uploaded += instance.getSize();
            } else {
                uploaded += instance.getUploaded();
            }

            total += instance.getSize();
        }
        setProgress();

        if (uploadFinished) {
            stopOutlined.setVisibility(View.GONE);
            headerStatus.setVisibility(View.GONE);
            presenter.getFileUploadInstances();
            return;
        }

        long now = Util.currentTimestamp();

        if (lastUploadedSize > 0) {
            long progressDifference = uploaded - lastUploadedSize;
            long timeDifference = now - lastUpdateTimeStamp;
            long remainingUpload = total - uploaded;
            long projectedRemaininigTime = 0;

            if (progressDifference > 0) {
                projectedRemaininigTime = (remainingUpload * timeDifference) / progressDifference;
            }

            if (projectedRemaininigTime > 3600000) {
                statusText.setText(String.format("%s, %s",
                        getContext().getResources().getQuantityString(R.plurals.upload_main_meta_number_of_files, uploadnigList.size(), uploadnigList.size()),
                        getContext().getResources().getString(R.string.upload_main_heading_hour_left)));
            } else if (projectedRemaininigTime > 60000) {
                int minutes = (int) projectedRemaininigTime / 60000;
                statusText.setText(String.format("%s, %s",
                        getContext().getResources().getQuantityString(R.plurals.upload_main_meta_number_of_files, uploadnigList.size(), uploadnigList.size()),
                        getContext().getResources().getQuantityString(R.plurals.upload_main_meta_minutes_left, minutes, minutes)));
            } else if (projectedRemaininigTime > 0) {
                statusText.setText(String.format("%s, %s",
                        getContext().getResources().getQuantityString(R.plurals.upload_main_meta_number_of_files, uploadnigList.size(), uploadnigList.size()),
                        getContext().getResources().getString(R.string.upload_info_heading_minute_left)));
            }

            headerText.setText(R.string.upload_main_meta_status_upload);
        }
        lastUploadedSize = uploaded;
        lastUpdateTimeStamp = now;
    }

    private void clearHistory() {
        presenter.deleteFileUploadInstancesInStatus(ITellaUploadsRepository.UploadStatus.UPLOADED);
        uploadsRecyclerView.removeAllViews();
    }

    private void setProgress() {
        if (total > 0) {
            int progress = (int) (uploaded * 100 / total);
            stopOutlined.setProgress(progress);
        } else {
            stopOutlined.setProgress(0);
        }
    }
}

