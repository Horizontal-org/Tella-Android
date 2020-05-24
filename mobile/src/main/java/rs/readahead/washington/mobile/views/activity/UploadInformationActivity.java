package rs.readahead.washington.mobile.views.activity;

import android.annotation.SuppressLint;
import android.content.Context;
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
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.domain.entity.FileUploadInstance;
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.TellaFileUploadPresenter;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.adapters.UploadInformationRecycleViewAdapter;

public class UploadInformationActivity extends BaseActivity implements
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
    private CacheWordDataSource cacheWordDataSource;
    private EventCompositeDisposable disposables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_upload_information);
        ButterKnife.bind(this);

        cacheWordDataSource = new CacheWordDataSource(this);

        disposables = MyApplication.bus().createCompositeDisposable();

        adapter = new UploadInformationRecycleViewAdapter(this, new MediaFileHandler(cacheWordDataSource),this, set);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);

        presenter = new TellaFileUploadPresenter(this);

        setupToolbar();

        if (getIntent().hasExtra(SECTION_SET)) {
            this.set = getIntent().getLongExtra(SECTION_SET, 0);
            if (set != 0) {
                presenter.getFileUploadSetInstances(set);
            }
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
        presenter.getFileUploadSetInstances(set);
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
                .setMessage(R.string.clear_upload_history_dialog)
                .setPositiveButton(R.string.clear, (dialog, which) -> clearUploadInformation())
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
    public void onGetFileUploadSetInstancesSuccess(List<FileUploadInstance> instances) {
        this.instances = instances;
        adapter.setInstances(instances);
        updateView();
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

    @SuppressLint("DefaultLocale")
    private void updateView() {
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
