package org.horizontal.tella.mobile.views.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import com.hzontal.tella_locking_ui.patternlock.PatternUtils;
import com.hzontal.tella_locking_ui.patternlock.PatternView;
import com.hzontal.tella_locking_ui.patternlock.SetPatternActivity;

import java.security.GeneralSecurityException;
import java.util.List;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import org.horizontal.tella.mobile.MyApplication;
import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.util.DialogsUtil;
import org.horizontal.tella.mobile.util.LocaleManager;
import timber.log.Timber;


public class NewPatternActivity extends SetPatternActivity implements ICacheWordSubscriber {

    private CacheWordHandler mCacheWord;
    private Context context = NewPatternActivity.this;
    private ProgressDialog dialog;
    private String mNewPassphrase;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.getInstance().getLocalizedContext(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCacheWord = new CacheWordHandler(this);
    }

    @Override
    protected void onCanceled() {
        super.onCanceled();
        finish();
    }

    @Override
    protected int getMinPatternSize() {
        return 6;
    }

    @Override
    protected void onSetPattern(List<PatternView.Cell> pattern) {
        mNewPassphrase = PatternUtils.patternToSha1String(pattern);
    }

    @Override
    protected void onConfirmed() {
        dialog = DialogsUtil.showProgressDialog(context, getString(R.string.lock_dialog_expl_save_pattern));
        new Thread(() -> {
            try {
                mCacheWord.setPassphrase(mNewPassphrase.toCharArray());
            } catch (GeneralSecurityException e) {
                Timber.e(e, "CacheWord pass initialization failed");
            }
        }).start();
    }

    @Override
    public void onCacheWordUninitialized() {

    }

    @Override
    public void onCacheWordLocked() {

    }

    @Override
    public void onCacheWordOpened() {
        if (dialog != null) {
            dialog.dismiss();
        }

        MyApplication.startMainActivity(this);
        finish();

        overridePendingTransition(0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCacheWord.connectToService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCacheWord.disconnectFromService();
    }

    @Override
    protected void onDestroy() {
        if (dialog != null) {
            dialog.dismiss();
        }
        super.onDestroy();
    }
}
