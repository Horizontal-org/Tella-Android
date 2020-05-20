package rs.readahead.washington.mobile.views.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
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
import rs.readahead.washington.mobile.domain.entity.FileUploadInstance;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.TellaFileUploadPresenter;
import rs.readahead.washington.mobile.views.adapters.UploadSection;

public class UploadsActivity extends BaseActivity implements
        ITellaFileUploadPresenterContract.IView {
    @BindView(R.id.uploadsRecyclerView)
    RecyclerView uploadsRecyclerView;
    @BindView(R.id.uploadInfo)
    TextView uploadInfo;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private TellaFileUploadPresenter presenter;
    private CacheWordDataSource cacheWordDataSource;
    private EventCompositeDisposable disposables;
    private SectionedRecyclerViewAdapter sectionAdapter;

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

        sectionAdapter = new SectionedRecyclerViewAdapter();
        final GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 5);

        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(final int position) {
                if (sectionAdapter.getSectionItemViewType(position) == SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER ||
                        sectionAdapter.getSectionItemViewType(position) == SectionedRecyclerViewAdapter.VIEW_TYPE_FOOTER) {
                    return 5;
                }
                return 1;
            }
        });

        uploadsRecyclerView.setLayoutManager(gridLayoutManager);
        uploadsRecyclerView.setAdapter(sectionAdapter);
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

        cacheWordDataSource.dispose();
        stopPresenter();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sectionAdapter.removeAllSections();
        presenter.getFileUploadInstances();
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

        for (FileUploadInstance instance : instances) {
            if (set != instance.getSet()) {
                sectionAdapter.addSection(new UploadSection(getContext(), new MediaFileHandler(cacheWordDataSource), setInstances));
                sectionAdapter.notifyDataSetChanged();

                set = instance.getSet();
                setInstances = new ArrayList<>();
            }
            setInstances.add(instance);
        }
        sectionAdapter.addSection(new UploadSection(getContext(), new MediaFileHandler(cacheWordDataSource), setInstances)); // add last set
        sectionAdapter.notifyDataSetChanged();
    }

    @Override
    public void onGetFileUploadInstancesError(Throwable error) {

    }

    @Override
    public Context getContext() {
        return this;
    }

    private void updateProgress(long progress) {
        runOnUiThread(() -> uploadInfo.setText(String.valueOf(progress)));
    }
}
