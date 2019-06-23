package rs.readahead.washington.mobile.views.activity;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.LocaleManager;


public abstract class BaseActivity extends AppCompatActivity {
    private boolean manualOrientation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isManualOrientation() && !getResources().getBoolean(R.bool.isTablet)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.getInstance().getLocalizedContext(base));
    }

    public boolean isManualOrientation() {
        return manualOrientation;
    }

    public void setManualOrientation(boolean manualOrientation) {
        this.manualOrientation = manualOrientation;
    }

    protected void showToast(@StringRes int resId) {
        showToast(getString(resId));
    }

    protected void showToast(String string) {
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
    }
}
