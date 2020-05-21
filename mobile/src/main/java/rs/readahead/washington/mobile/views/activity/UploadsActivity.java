package rs.readahead.washington.mobile.views.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionAdapter;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.FileUploadProgressEvent;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.domain.entity.FileUploadInstance;
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.TellaFileUploadPresenter;
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

    private TellaFileUploadPresenter presenter;
    private CacheWordDataSource cacheWordDataSource;
    private EventCompositeDisposable disposables;
    private SectionedRecyclerViewAdapter sectionedAdapter;
    private AlertDialog alertDialog;
    private boolean uploadsExist;
    private final int spanCount = 5;

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
                updateProgress(event.getProgress());
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
    protected void onResume() {
        super.onResume();
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
        long set = instances.get(0).getSet();
        this.uploadsExist = instances.size() > 0;

        if (uploadsExist) {
            emptyInfo.setVisibility(View.GONE);
        } else {
            emptyInfo.setVisibility(View.VISIBLE);
        }

        for (FileUploadInstance instance : instances) {
            if (set != instance.getSet()) {
                sectionedAdapter.addSection(new UploadSection(getContext(), new MediaFileHandler(cacheWordDataSource), setInstances, this, set));
                sectionedAdapter.notifyDataSetChanged();

                set = instance.getSet();
                setInstances = new ArrayList<>();
            }
            setInstances.add(instance);
        }
        sectionedAdapter.addSection(new UploadSection(getContext(), new MediaFileHandler(cacheWordDataSource), setInstances, this, set)); // add last set
        sectionedAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();
    }

    @Override
    public void onGetFileUploadInstancesError(Throwable error) {

    }

    @Override
    public void onFileUploadInstancesDeleted() {
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

    @Override
    public void clearScheduled() {
        presenter.deleteFileUploadInstances(ITellaUploadsRepository.UploadStatus.SCHEDULED);
        runOnUiThread(() -> uploadsRecyclerView.removeAllViews());
    }

    @Override
    public void showUploadInformation(long set) {
        startUploadInformationActivity(set);
    }

    @Override
    public void onHeaderRootViewClicked(@NonNull UploadSection section) {
        final SectionAdapter sectionAdapter = sectionedAdapter.getAdapterForSection(section);

        // store info of current section state before changing its state
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
    public void onItemRootViewClicked(@NonNull UploadSection section, int itemAdapterPosition) {
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

    private void updateProgress(long progress) {
        //runOnUiThread(() -> uploadInfo.setText(String.valueOf(progress)));
    }

    private void clearHistory() {
        presenter.deleteFileUploadInstances(ITellaUploadsRepository.UploadStatus.UPLOADED);
        runOnUiThread(() -> uploadsRecyclerView.removeAllViews());// bad & temporary solution to refresh
    }
}
