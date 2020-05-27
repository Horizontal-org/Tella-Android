package rs.readahead.washington.mobile.views.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionAdapter;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
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
import rs.readahead.washington.mobile.views.adapters.UploadSection;


public class UploadsActivity extends BaseActivity implements
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
    ImageView stopOutlined;

    private TellaFileUploadPresenter presenter;
    private CacheWordDataSource cacheWordDataSource;
    private EventCompositeDisposable disposables;
    private SectionedRecyclerViewAdapter sectionedAdapter;
    private AlertDialog alertDialog;
    private boolean uploadsExist;
    private final int spanCount = 5;
    private long uploadingSet;
    private UploadSection uploadingSection;
    private long lastUpdateTimeStamp = 0;
    private long lastUploadedSize = 0;

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

        RecyclerView.ItemAnimator animator = uploadsRecyclerView.getItemAnimator();
        if (animator != null) {
            animator.setMoveDuration(0);
            animator.setRemoveDuration(0);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.uploads);
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
        List<FileUploadInstance> setInstances = new ArrayList<>();
        boolean uploaded = true;
        long set = instances.get(0).getSet();
        this.uploadsExist = instances.size() > 0;

        if (uploadsExist) {
            emptyInfo.setVisibility(View.GONE);
        } else {
            emptyInfo.setVisibility(View.VISIBLE);
        }

        for (FileUploadInstance instance : instances) {
            if (instance.getStatus().ordinal() > ITellaUploadsRepository.UploadStatus.UPLOADED.ordinal()) {
                uploaded = false; // if any of instances in the set is not uploaded -> set is not uploaded
                uploadingSet = set;
            }
            if (set != instance.getSet()) {
                UploadSection section = new UploadSection(getContext(), new MediaFileHandler(cacheWordDataSource), setInstances, this, set);
                sectionedAdapter.addSection(section);
                sectionedAdapter.notifyDataSetChanged();
                if (!uploaded) {
                    setUploadingHeader(setInstances);
                    uploadingSection = section;
                }
                uploaded = true;
                set = instance.getSet();
                setInstances = new ArrayList<>();
            }
            setInstances.add(instance);
        }
        UploadSection section = new UploadSection(getContext(), new MediaFileHandler(cacheWordDataSource), setInstances, this, set);
        if (!uploaded) {
            setUploadingHeader(setInstances);
            uploadingSection = section;
        }
        sectionedAdapter.addSection(section); // add last set
        sectionedAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();
    }

    @Override
    public void onGetFileUploadInstancesError(Throwable error) {

    }

    @Override
    public void onGetFileUploadSetInstancesSuccess(List<FileUploadInstance> instances) {
        updateProgressStatus(instances);
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

    @OnClick(R.id.stop_outlined)
    public void showClearScheduledDialog() {
        alertDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.stop_upload_dialog)
                .setPositiveButton(R.string.clear, (dialog, which) -> clearScheduled())
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();
    }

    public void clearScheduled() {
        presenter.deleteFileUploadInstancesNotInStatus(ITellaUploadsRepository.UploadStatus.UPLOADED);
        runOnUiThread(() -> uploadsRecyclerView.removeAllViews());
    }

    @Override
    public void showUploadInformation(long set) {
        startUploadInformationActivity(set);
    }

    @Override
    public void onHeaderRootViewClicked(@NonNull UploadSection section) {
        final SectionAdapter sectionAdapter = sectionedAdapter.getAdapterForSection(section);

        final boolean wasExpanded = section.isExpanded();
        final int previousItemsTotal = section.getContentItemsTotal();

        section.setExpanded(!wasExpanded);
        sectionAdapter.notifyHeaderChanged();

        if (wasExpanded) {
            sectionAdapter.notifyItemRangeRemoved(0, previousItemsTotal);
        } else {
            sectionAdapter.notifyAllItemsInserted();
        }
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

    private void startUploadInformationActivity(long set) {
        Intent intent = new Intent(this, UploadInformationActivity.class);
        intent.putExtra(UploadInformationActivity.SECTION_SET, set);
        startActivity(intent);
    }

    private void showClearHistoryDialog() {
        alertDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.clear_upload_history_dialog)
                .setPositiveButton(R.string.clear, (dialog, which) -> clearHistory())
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();
    }

    private void onProgressUpdateEvent(UploadProgressInfo progress) {
        presenter.getFileUploadSetInstances(uploadingSet);
    }

    private void setUploadingHeader(List<FileUploadInstance> instances) {
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
        startedText.setText(String.format("%s: %s", getContext().getResources().getString(R.string.started), Util.getDateTimeString(started, "dd/MM/yyyy h:mm a")));
        statusText.setText(String.format("%s, %s",
                getContext().getResources().getQuantityString(R.plurals.file, instances.size(), instances.size()), getContext().getResources().getString(R.string.connecting)));
    }

    private void updateProgressStatus(List<FileUploadInstance> instances) {
        boolean updateFinished = true;
        long total = 0;
        long uploaded = 0;
        long started = instances.get(0).getStarted();
        long newUpdateTimestamp = 0;
        for (FileUploadInstance instance : instances) {
            if (instance.getStarted() < started) {
                started = instance.getStarted();
            }
            if (instance.getStatus() != ITellaUploadsRepository.UploadStatus.UPLOADED) {
                updateFinished = false;
            }
            if (instance.getUpdated() > newUpdateTimestamp) {
                newUpdateTimestamp = instance.getUpdated();
            }
            uploaded += instance.getUploaded();
            total += instance.getSize();
        }
        if (updateFinished) {
            stopOutlined.setVisibility(View.GONE);
            headerText.setText(getContext().getResources().getQuantityString(R.plurals.files_uploaded, instances.size(), instances.size()));
            statusText.setText(String.format("%s: %s", getContext().getResources().getString(R.string.started), Util.getDateTimeString(started, "dd/MM/yyyy h:mm a")));
            startedText.setVisibility(View.GONE);
            sectionedAdapter.notifyDataSetChanged();
            //uploadingSection ref
        }
        long progressDifference = uploaded - lastUploadedSize;
        long timeDifference = newUpdateTimestamp - lastUpdateTimeStamp;
        long remainingUpload = total - uploaded;
        long projectedRemaininigTime = (remainingUpload * timeDifference) / progressDifference;

        if ( timeDifference < 0) {
            return;
        }
        if (projectedRemaininigTime > 60000) {
            int minutes = (int) projectedRemaininigTime / 60000;
            statusText.setText(String.format("%s, %s",
                    getContext().getResources().getQuantityString(R.plurals.file, instances.size(), instances.size()),
                    getContext().getResources().getQuantityString(R.plurals.minutes_left, minutes, minutes)));
        } else {
            statusText.setText(String.format("%s, %s",
                    getContext().getResources().getQuantityString(R.plurals.file, instances.size(), instances.size()),
                    getContext().getResources().getString(R.string.less_than_a_minute_left)));
        }

        lastUpdateTimeStamp = newUpdateTimestamp;
        lastUploadedSize = uploaded;
    }

    private void clearHistory() {
        presenter.deleteFileUploadInstancesInStatus(ITellaUploadsRepository.UploadStatus.UPLOADED);
        runOnUiThread(() -> uploadsRecyclerView.removeAllViews());// bad & temporary solution to refresh
    }
}
