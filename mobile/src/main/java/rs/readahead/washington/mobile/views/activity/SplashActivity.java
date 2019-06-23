package rs.readahead.washington.mobile.views.activity;

import android.os.Bundle;
import android.os.Handler;

import info.guardianproject.cacheword.ICacheWordSubscriber;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;


public class SplashActivity extends CacheWordSubscriberBaseActivity implements ICacheWordSubscriber {
    private static final long SPLASH_TIMEOUT_MS = 1000L;

    private Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);
        handler = new Handler();
    }

    @Override
    public void onCacheWordOpened() {
        super.onCacheWordOpened();
        handler.postDelayed(() -> {
            MyApplication.startMainActivity(SplashActivity.this);
            finish();
        }, SPLASH_TIMEOUT_MS);
    }
}
