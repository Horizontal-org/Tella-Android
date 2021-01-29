package rs.readahead.washington.mobile.views.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.security.GeneralSecurityException;
import java.util.List;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import me.zhanghai.android.patternlock.ConfirmPatternActivity;
import me.zhanghai.android.patternlock.PatternUtils;
import me.zhanghai.android.patternlock.PatternView;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.LocaleManager;
import timber.log.Timber;


public class PatternUpgradeActivity extends ConfirmPatternActivity implements ICacheWordSubscriber {
    private CacheWordHandler mCacheWord;
    private ProgressDialog dialog;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.getInstance().getLocalizedContext(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCacheWord = new CacheWordHandler(this);

        Button button = findViewById(R.id.pl_right_button);
        button.setVisibility(View.INVISIBLE);
    }

    @Override
    protected boolean isPatternCorrect(List<PatternView.Cell> pattern) {
        dialog = DialogsUtil.showProgressDialog(this, getString(R.string.lock_dialog_expl_unlock_app));

        try {
            mCacheWord.setPassphrase(PatternUtils.patternToSha1String(pattern).toCharArray());
            return true;
        } catch (final GeneralSecurityException e) {
            Timber.d(e, getClass().getName());
            dismissDialog();
            return false;
        }
    }

    @Override
    public void onCacheWordUninitialized() {
    }

    @Override
    public void onCacheWordLocked() {
    }

    @Override
    public void onCacheWordOpened() {
        dismissDialog();

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
        dismissDialog();
        mCacheWord.disconnectFromService();
    }

    private void dismissDialog() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
