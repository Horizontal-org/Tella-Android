package rs.readahead.washington.mobile.views.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.hzontal.tella_locking_ui.TellaKeysUI;
import com.hzontal.tella_locking_ui.patternlock.ConfirmPatternActivity;
import com.hzontal.tella_locking_ui.patternlock.PatternUtils;
import com.hzontal.tella_locking_ui.patternlock.PatternView;

import org.hzontal.tella.keys.MainKeyStore;
import org.hzontal.tella.keys.config.IUnlockRegistryHolder;
import org.hzontal.tella.keys.config.UnlockConfig;
import org.hzontal.tella.keys.config.UnlockRegistry;
import org.hzontal.tella.keys.key.MainKey;

import java.security.GeneralSecurityException;
import java.util.List;

import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.cacheword.PassphraseSecrets;
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
        mRightButton.setVisibility(View.INVISIBLE);
        mLeftButton.setVisibility(View.INVISIBLE);
    }

    @Override
    protected boolean isPatternCorrect(List<PatternView.Cell> pattern) {
        dialog = DialogsUtil.showProgressDialog(this, getString(R.string.lock_dialog_expl_unlock_app));
        try {
            PassphraseSecrets secrets = PassphraseSecrets.fetchSecrets(
                    this, PatternUtils.patternToSha1String(pattern).toCharArray());
            mCacheWord.setCachedSecrets(secrets);
            IUnlockRegistryHolder holder = (IUnlockRegistryHolder) getApplicationContext();
            holder.getUnlockRegistry().setActiveMethod(PatternUpgradeActivity.this, UnlockRegistry.Method.TELLA_PATTERN);
            UnlockConfig config = holder.getUnlockRegistry().getActiveConfig(this);
            TellaKeysUI.getMainKeyStore().store(new MainKey(new SecretKeySpec(secrets.getSecretKey().getEncoded(), "AES")), config.wrapper, new PBEKeySpec(PatternUtils.patternToSha1String(pattern).toCharArray()), new MainKeyStore.IMainKeyStoreCallback() {
                @Override
                public void onSuccess(MainKey mainKey) {
                    Timber.d("** MainKey stored: %s **", mainKey);
                    TellaKeysUI.getMainKeyHolder().set(mainKey);
                    TellaKeysUI.getCredentialsCallback().onSuccessfulUnlock(PatternUpgradeActivity.this);
                }

                @Override
                public void onError(Throwable throwable) {
                    Timber.e(throwable, "** MainKey store error **");
                }
            });
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
        startActivity(new Intent(this, MainActivity.class));
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        dismissDialog();
    }

    private void dismissDialog() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
