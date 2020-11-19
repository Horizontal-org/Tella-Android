package rs.readahead.washington.mobile;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.StrictMode;

import com.evernote.android.job.JobManager;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.multidex.MultiDexApplication;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.cacheword.ICachedSecrets;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import rs.readahead.washington.mobile.bus.TellaBus;
import rs.readahead.washington.mobile.data.rest.BaseApi;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.domain.entity.KeyBundle;
import rs.readahead.washington.mobile.javarosa.JavaRosa;
import rs.readahead.washington.mobile.javarosa.PropertyManager;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.util.LocaleManager;
import rs.readahead.washington.mobile.util.jobs.PendingFormSendJob;
import rs.readahead.washington.mobile.util.jobs.TellaJobCreator;
import rs.readahead.washington.mobile.util.jobs.TellaUploadJob;
import rs.readahead.washington.mobile.views.activity.ExitActivity;
import rs.readahead.washington.mobile.views.activity.LockScreenActivity;
import rs.readahead.washington.mobile.views.activity.MainActivity;
import rs.readahead.washington.mobile.views.activity.TellaIntroActivity;
import timber.log.Timber;


public class MyApplication extends MultiDexApplication implements ICacheWordSubscriber, LifecycleObserver {
    private static TellaBus bus;

    private CacheWordHandler cacheWordHandler = null;
    private boolean detached = false;

    private static final Object keyLock = new Object();
    private static WeakReference<ICachedSecrets> cachedSecretsReference;
    private static long version = 0;


    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPrefs.getInstance().init(newBase);
        super.attachBaseContext(LocaleManager.getInstance().getLocalizedContext(newBase));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll().penaltyLog()/*.penaltyDeath()*/.build()); // todo: catch those..
        }
        // todo: implement dagger2

        SharedPrefs.getInstance().init(this);

        configureCrashlytics();

        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                Timber.d(throwable, getClass().getName());
                FirebaseCrashlytics.getInstance().recordException(throwable);
            }
        });

        bus = TellaBus.create();

        BaseApi.Builder apiBuilder = new BaseApi.Builder();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            apiBuilder.setLogLevelFull();
        }

        apiBuilder.build(getString(R.string.api_base_url));

        // MediaFile init
        MediaFileHandler.init(this);
        MediaFileHandler.emptyTmp(this);

        // evernote jobs
        JobManager.create(this).addJobCreator(new TellaJobCreator());
        //JobManager.instance().cancelAll(); // for testing, kill them all for now..

        // Collect
        PropertyManager mgr = new PropertyManager();
        JavaRosa.initializeJavaRosa(mgr);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        if (cacheWordHandler != null) {
            cacheWordHandler.lock();
        }
    }

    public static void startMainActivity(@NonNull Context context) {
        Intent intent;

        if (Preferences.isFirstStart()) {
            intent = new Intent(context, TellaIntroActivity.class);
        } else {
            intent = new Intent(context, MainActivity.class);
        }

        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        maybeExcludeIntentFromRecents(intent);
        context.startActivity(intent);
    }

    public static void startLockScreenActivity(@NonNull Context context) {
        Intent intent = new Intent(context, LockScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    @NonNull
    public static TellaBus bus() {
        return bus;
    }

    public synchronized void createCacheWordHandler() {
        if (cacheWordHandler == null) {
            detached = false;
            cacheWordHandler = new CacheWordHandler(getApplicationContext(), this);
            cacheWordHandler.connectToService();
        }
    }

    @Override
    public void onCacheWordUninitialized() {
        detachCacheWordHandler();
        synchronized (keyLock) {
            cachedSecretsReference = null;
            version++;
        }
    }

    @Override
    public void onCacheWordLocked() {
        detachCacheWordHandler();
        synchronized (keyLock) {
            cachedSecretsReference = null;
            version++;
        }
    }

    @Override
    public void onCacheWordOpened() {
        detachCacheWordHandler();
        synchronized (keyLock) {
            cachedSecretsReference = new WeakReference<>(cacheWordHandler.getCachedSecrets());
            version++;
        }

        // fire up jobs that need CacheWord secret - they will quit if nothing to do..
        PendingFormSendJob.scheduleJob();
        TellaUploadJob.scheduleJob();
    }

    @Nullable
    public static KeyBundle getKeyBundle() {
        synchronized (keyLock) {
            if (cachedSecretsReference == null) {
                return null;
            }

            ICachedSecrets cachedSecrets = cachedSecretsReference.get();
            if (cachedSecrets == null) {
                return null;
            }

            return new KeyBundle(cachedSecrets, version);
        }
    }

    /* public static boolean isKeyBundleValid(@NonNull final KeyBundle other) {
        synchronized (keyLock) {
            return other.getKey() != null && version == other.getVersion();
        }
    } */

    public static boolean isConnectedToInternet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static void exit(Context context) {
        Intent intent = new Intent(context, ExitActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NO_ANIMATION |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        context.startActivity(intent);
    }

    private synchronized void detachCacheWordHandler() {
        if (!detached && cacheWordHandler != null) {
            cacheWordHandler.detach();
            detached = true;
        }
    }

    private static void maybeExcludeIntentFromRecents(Intent intent) {
        if (Preferences.isSecretModeActive()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS/* |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK*/);
        }
    }

    private void configureCrashlytics() {
        boolean enabled = (!BuildConfig.DEBUG && Preferences.isSubmittingCrashReports());

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled);

        if (!enabled) {
            FirebaseCrashlytics.getInstance().deleteUnsentReports();
        }
    }
}
