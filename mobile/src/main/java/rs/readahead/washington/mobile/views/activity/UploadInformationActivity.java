package rs.readahead.washington.mobile.views.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.FileUploadInstance;
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository;
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.TellaFileUploadPresenter;
import timber.log.Timber;

public class UploadInformationActivity extends BaseActivity implements
        ITellaFileUploadPresenterContract.IView {
    public static final String SECTION_SET = "SET";

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private TellaFileUploadPresenter presenter;
    private AlertDialog alertDialog;
    private long set;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_upload_information);
        ButterKnife.bind(this);

        presenter = new TellaFileUploadPresenter(this);

        setupToolbar();

        if (getIntent().hasExtra(SECTION_SET)) {
            this.set = getIntent().getLongExtra(SECTION_SET, 0);
            if (set != 0) {
                Timber.d("++++ UploadInformation for set %d", set);
                //presenter.get(this);
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
        
    }

    @Override
    public void onGetFileUploadInstancesSuccess(List<FileUploadInstance> instances) {

    }

    @Override
    public void onGetFileUploadInstancesError(Throwable error) {

    }

    @Override
    public void onFileUploadInstancesDeleted() {

    }

    @Override
    public void onFileUploadInstancesDeletionError(Throwable throwable) {

    }

    @Override
    public Context getContext() {
        return this;
    }
}
