package rs.readahead.washington.mobile.views.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.crashlytics.android.Crashlytics;
import com.simplify.ink.InkView;

import java.io.ByteArrayOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.mvp.contract.ISignaturePresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.SignaturePresenter;
import rs.readahead.washington.mobile.util.DialogsUtil;


public class SignatureActivity extends CacheWordSubscriberBaseActivity implements
        ISignaturePresenterContract.IView {

    public static final String MEDIA_FILE_KEY = "mfk";

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.ink)
    InkView ink;

    private ProgressDialog progressDialog;
    private SignaturePresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setManualOrientation(true);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_signature);
        ButterKnife.bind(this);

        presenter = new SignaturePresenter(this);

        setupToolbar();
        setupSignaturePad();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.signature_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_item_clear) {
            ink.clear();
            return true;
        }

        if (id == R.id.menu_item_save) {
            saveSignature();
            return true;
        }

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        destroyPresenter();
        super.onDestroy();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.collect_form_signature_app_bar);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white);
        }
    }

    public void saveSignature() {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            if (ink.getBitmap((getResources().getColor(R.color.wa_white))).compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                presenter.addPngImage(stream.toByteArray());
            }
        } catch (Exception exception) {
            Crashlytics.logException(exception);
        }
    }

    @Override
    public void onAddingStart() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.gallery_dialog_expl_encrypting));
    }

    @Override
    public void onAddingEnd() {
        hideProgressDialog();
        showToast(R.string.gallery_toast_file_encrypted);
    }

    @Override
    public void onAddSuccess(MediaFile mediafile) {
        setResult(Activity.RESULT_OK, new Intent().putExtra(MEDIA_FILE_KEY, mediafile));
        finish();
    }

    @Override
    public void onAddError(Throwable error) {
        showToast(R.string.collect_form_signature_toast_fail_saving);
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void setupSignaturePad() {
        ink.setColor(getResources().getColor(android.R.color.black));
        ink.setMinStrokeWidth(1.5f);
        ink.setMaxStrokeWidth(6f);
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
}
