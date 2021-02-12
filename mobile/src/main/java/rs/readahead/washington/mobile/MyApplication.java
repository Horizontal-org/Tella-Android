package rs.readahead.washington.mobile;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.StrictMode;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.evernote.android.job.JobManager;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_locking_ui.TellaKeysUI;
import com.hzontal.tella_locking_ui.common.CredentialsCallback;
import com.hzontal.tella_locking_ui.ui.AppCompatActivityUnlocker;
import com.hzontal.tella_locking_ui.ui.DeviceCredentialsUnlockActivity;
import com.hzontal.tella_locking_ui.ui.PasswordUnlockActivity;
import com.hzontal.tella_locking_ui.ui.PinUnlockActivity;
import com.hzontal.tella_locking_ui.ui.pattern.PatternUnlockActivity;

import org.hzontal.tella.keys.MainKeyStore;
import org.hzontal.tella.keys.TellaKeys;
import org.hzontal.tella.keys.config.IUnlockRegistryHolder;
import org.hzontal.tella.keys.config.UnencryptedUnlocker;
import org.hzontal.tella.keys.config.UnlockConfig;
import org.hzontal.tella.keys.config.UnlockRegistry;
import org.hzontal.tella.keys.key.LifecycleMainKey;
import org.hzontal.tella.keys.key.MainKey;
import org.hzontal.tella.keys.wrapper.AndroidKeyStoreWrapper;
import org.hzontal.tella.keys.wrapper.PBEKeyWrapper;
import org.hzontal.tella.keys.wrapper.UnencryptedKeyWrapper;

import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import rs.readahead.washington.mobile.bus.TellaBus;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.data.rest.BaseApi;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.javarosa.JavaRosa;
import rs.readahead.washington.mobile.javarosa.PropertyManager;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.util.LocaleManager;
import rs.readahead.washington.mobile.util.jobs.TellaJobCreator;
import rs.readahead.washington.mobile.views.activity.ExitActivity;
import rs.readahead.washington.mobile.views.activity.MainActivity;
import rs.readahead.washington.mobile.views.activity.TellaIntroActivity;
import rs.readahead.washington.mobile.views.activity.onboarding.OnBoardingActivity;
import timber.log.Timber;


public class MyApplication extends Application implements IUnlockRegistryHolder, CredentialsCallback {
    private static TellaBus bus;
    private static LifecycleMainKey mainKeyHolder;
    @SuppressLint("StaticFieldLeak")
    private static MainKeyStore mainKeyStore;
    private static UnlockRegistry unlockRegistry;
    private static KeyDataSource keyDataSource;

    public static void startMainActivity(@NonNull Context context) {
        Intent intent;
        if (Preferences.isFirstStart()) {
            intent = new Intent(context, OnBoardingActivity.class);
        } else {
            intent = new Intent(context, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        maybeExcludeIntentFromRecents(intent);
        context.startActivity(intent);
    }


    @NonNull
    public static TellaBus bus() {
        return bus;
    }

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

    private static void maybeExcludeIntentFromRecents(Intent intent) {
        if (Preferences.isSecretModeActive()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS/* |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK*/);
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPrefs.getInstance().init(newBase);
        super.attachBaseContext(LocaleManager.getInstance().getLocalizedContext(newBase));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate() {
        super.onCreate();

        //ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        BaseApi.Builder apiBuilder = new BaseApi.Builder();

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll().penaltyLog()/*.penaltyDeath()*/.build()); // todo: catch those..
            Timber.plant(new Timber.DebugTree());
            apiBuilder.setLogLevelFull();
        }
        // todo: implement dagger2
        SharedPrefs.getInstance().init(this);
        //configureCrashlytics();

        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                Timber.d(throwable, getClass().getName());
                FirebaseCrashlytics.getInstance().recordException(throwable);
            }
        });
        bus = TellaBus.create();

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

        //Tella keys
        TellaKeys.initialize();
        initializeLockConfigRegistry();
        mainKeyStore = new MainKeyStore(getApplicationContext());
        mainKeyHolder = new LifecycleMainKey(ProcessLifecycleOwner.get().getLifecycle(), LifecycleMainKey.NO_TIMEOUT);
        keyDataSource = new KeyDataSource(getApplicationContext());
        TellaKeysUI.initialize(mainKeyStore, mainKeyHolder, unlockRegistry, this);
    }

    private void configureCrashlytics() {
        boolean enabled = (!BuildConfig.DEBUG && Preferences.isSubmittingCrashReports());

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled);

        if (!enabled) {
            FirebaseCrashlytics.getInstance().deleteUnsentReports();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initializeLockConfigRegistry() {
        unlockRegistry = new UnlockRegistry();

        PBEKeyWrapper pbeKeyWrapper = new PBEKeyWrapper();
        AndroidKeyStoreWrapper androidKeyStoreWrapper = new AndroidKeyStoreWrapper();

        unlockRegistry.registerConfig(UnlockRegistry.Method.DISABLED,
                new UnlockConfig(new UnencryptedUnlocker(), new UnencryptedKeyWrapper()));

        unlockRegistry.registerConfig(UnlockRegistry.Method.TELLA_PIN,
                new UnlockConfig(new AppCompatActivityUnlocker(unlockRegistry, PinUnlockActivity.class), pbeKeyWrapper));

        unlockRegistry.registerConfig(UnlockRegistry.Method.TELLA_PATTERN,
                new UnlockConfig(new AppCompatActivityUnlocker(unlockRegistry, PatternUnlockActivity.class), pbeKeyWrapper));

        unlockRegistry.registerConfig(UnlockRegistry.Method.TELLA_PASSWORD,
                new UnlockConfig(new AppCompatActivityUnlocker(unlockRegistry, PasswordUnlockActivity.class), pbeKeyWrapper));

        unlockRegistry.registerConfig(UnlockRegistry.Method.DEVICE_CREDENTIALS,
                new UnlockConfig(new AppCompatActivityUnlocker(unlockRegistry, DeviceCredentialsUnlockActivity.class), androidKeyStoreWrapper));

        unlockRegistry.registerConfig(UnlockRegistry.Method.DEVICE_CREDENTIALS_BIOMETRICS,
                new UnlockConfig(new AppCompatActivityUnlocker(unlockRegistry, DeviceCredentialsUnlockActivity.class), androidKeyStoreWrapper));

        // we need this to set one active unlocking method
        // in Tella1 this can be here and fixed, but in Tella2 we need to read saved active method
        // when starting the app and set it (that functionality is missing from active registry, with changing method support)
        unlockRegistry.setActiveMethod(getApplicationContext(), UnlockRegistry.Method.TELLA_PATTERN);
    }

    @Override
    public void onSuccessfulUnlock(Context context) {
        mainKeyHolder = TellaKeysUI.getMainKeyHolder();
        mainKeyStore = TellaKeysUI.getMainKeyStore();
        unlockRegistry = TellaKeysUI.getUnlockRegistry();
        keyDataSource.initKeyDataSource();
        startMainActivity(context);
    }

    @Override
    public void onUnSuccessfulUnlock() {

    }

    @Override
    public UnlockRegistry getUnlockRegistry() {
        return unlockRegistry;
    }

    public static LifecycleMainKey getMainKeyHolder() {
        return mainKeyHolder;
    }

    public static KeyDataSource getKeyDataSource() {
        return keyDataSource;
    }

    public static MainKeyStore getMainKeyStore() {
        return mainKeyStore;
    }

    public static void resetKeys() {
        getMainKeyHolder().clear();
        TellaKeysUI.getMainKeyHolder().clear();
    }
    public static void initKeys(MainKey mainKey){
        getMainKeyHolder().set(mainKey);
        TellaKeysUI.getMainKeyHolder().set(mainKey);
    }
}
