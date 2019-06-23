package rs.readahead.washington.mobile.views.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;


public class ExitActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }
}
