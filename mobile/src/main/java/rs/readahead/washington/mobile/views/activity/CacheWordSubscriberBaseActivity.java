package rs.readahead.washington.mobile.views.activity;

import android.os.Bundle;

import com.crashlytics.android.Crashlytics;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import rs.readahead.washington.mobile.MyApplication;


public abstract class CacheWordSubscriberBaseActivity extends BaseActivity implements
        ICacheWordSubscriber {
    private CacheWordHandler cacheWordHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cacheWordHandler = new CacheWordHandler(this);
        maybeCreateHandler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cacheWordHandler.connectToService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cacheWordHandler.disconnectFromService();
    }

    @Override
    public void onCacheWordUninitialized() {
        MyApplication.startLockScreenActivity(this);
        finish();
    }

    @Override
    public void onCacheWordLocked() {
        MyApplication.startLockScreenActivity(this);
        finish();
    }

    @Override
    public void onCacheWordOpened() {
    }

    protected CacheWordHandler getCacheWordHandler() {
        return cacheWordHandler;
    }

    private void maybeCreateHandler() {
        // try to avoid CacheWord create(Foreground)Service issue? with Android 8
        // calling from activity onCreate
        try {
            MyApplication application = (MyApplication) getApplication();
            application.createCacheWordHandler();
        } catch (Throwable e) {
            Crashlytics.logException(e);
        }
    }
}
