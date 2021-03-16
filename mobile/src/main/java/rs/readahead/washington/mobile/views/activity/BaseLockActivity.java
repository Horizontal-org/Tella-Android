package rs.readahead.washington.mobile.views.activity;

import android.content.Intent;

import com.hzontal.tella_locking_ui.ui.pattern.PatternSetActivity;
import com.hzontal.tella_locking_ui.ui.pattern.PatternUnlockActivity;

import info.guardianproject.cacheword.SecretsManager;
import rs.readahead.washington.mobile.MyApplication;

public abstract class BaseLockActivity extends BaseActivity {
    private boolean locked;

    private void restrictActivity() {
        if (!MyApplication.getMainKeyStore().isStored()) {
            startKeySetup();
        } else {
            locked = !MyApplication.getMainKeyHolder().exists();

            if (locked) {
                startUnlockingMainKey();
            }
        }
    }

    public boolean isLocked() {
        return locked;
    }

    protected void startKeySetup() {
        Intent intent = new Intent(this, SecretsManager.isInitialized(this) ? PatternUpgradeActivity.class : PatternSetActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        this.startActivity(intent);
    }

    protected void startUnlockingMainKey() {
        Intent intent = new Intent(this, PatternUnlockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        this.startActivity(intent);
    }

    @Override
    protected void onResume() {
        restrictActivity();
        super.onResume();
    }
}
