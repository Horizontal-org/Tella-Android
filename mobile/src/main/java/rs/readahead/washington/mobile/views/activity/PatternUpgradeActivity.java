package rs.readahead.washington.mobile.views.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.hzontal.tella_locking_ui.TellaKeysUI;
import com.hzontal.tella_locking_ui.patternlock.ConfirmPatternActivity;
import com.hzontal.tella_locking_ui.patternlock.PatternUtils;
import com.hzontal.tella_locking_ui.patternlock.PatternView;

import org.hzontal.tella.keys.MainKeyStore;
import org.hzontal.tella.keys.config.IUnlockRegistryHolder;
import org.hzontal.tella.keys.config.UnlockConfig;
import org.hzontal.tella.keys.key.MainKey;

import java.security.GeneralSecurityException;
import java.util.List;

import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import info.guardianproject.cacheword.PassphraseSecrets;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.LocaleManager;
import timber.log.Timber;

public class PatternUpgradeActivity extends ConfirmPatternActivity {
    private ProgressDialog dialog;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.getInstance().getLocalizedContext(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Button button = findViewById(R.id.pl_right_button);
        button.setVisibility(View.INVISIBLE);
    }

    @Override
    protected boolean isPatternCorrect(List<PatternView.Cell> pattern) {
        dialog = DialogsUtil.showProgressDialog(this, getString(R.string.lock_dialog_expl_unlock_app));

        try {
            PassphraseSecrets secrets = PassphraseSecrets.fetchSecrets(this, PatternUtils.patternToSha1String(pattern).toCharArray());
            MainKey mainKey = new MainKey(new SecretKeySpec(secrets.getSecretKey().getEncoded(), "AES"));
            PBEKeySpec keySpec = new PBEKeySpec(PatternUtils.patternToSha1String(pattern).toCharArray());

            IUnlockRegistryHolder holder = (IUnlockRegistryHolder) getApplicationContext();
            UnlockConfig config = holder.getUnlockRegistry().getActiveConfig(this);

            TellaKeysUI.getMainKeyStore().store(mainKey, config.wrapper, keySpec, new MainKeyStore.IMainKeyStoreCallback() {
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
