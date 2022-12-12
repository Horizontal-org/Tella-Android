package rs.readahead.washington.mobile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.evernote.android.job.JobManager;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_locking_ui.TellaKeysUI;
import com.hzontal.tella_locking_ui.common.CredentialsCallback;
import com.hzontal.tella_locking_ui.ui.AppCompatActivityUnlocker;
import com.hzontal.tella_locking_ui.ui.DeviceCredentialsUnlockActivity;
import com.hzontal.tella_locking_ui.ui.password.PasswordUnlockActivity;
import com.hzontal.tella_locking_ui.ui.pattern.PatternUnlockActivity;
import com.hzontal.tella_locking_ui.ui.pin.PinUnlockActivity;
import com.hzontal.tella_vault.Vault;
import com.hzontal.tella_vault.rx.RxVault;

import org.cleaninsights.sdk.CleanInsights;
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

import java.io.File;

import dagger.hilt.android.HiltAndroidApp;
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
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.CleanInsightUtils;
import rs.readahead.washington.mobile.util.LocaleManager;
import rs.readahead.washington.mobile.util.TellaUpgrader;
import rs.readahead.washington.mobile.util.jobs.TellaJobCreator;
import rs.readahead.washington.mobile.views.activity.ExitActivity;
import rs.readahead.washington.mobile.views.activity.MainActivity;
import rs.readahead.washington.mobile.views.activity.onboarding.OnBoardingActivity;
import timber.log.Timber;

@HiltAndroidApp
public class MyApplication extends MultiDexApplication implements IUnlockRegistryHolder, CredentialsCallback, Configuration.Provider  {
    public static Vault vault;
    public static RxVault rxVault;
    private static TellaBus bus;
    private static LifecycleMainKey mainKeyHolder;
    @SuppressLint("StaticFieldLeak")
    private static MainKeyStore mainKeyStore;
    private static UnlockRegistry unlockRegistry;
    private static KeyDataSource keyDataSource;
    private static CleanInsights cleanInsights;
    private final Long start = System.currentTimeMillis();
    Vault.Config vaultConfig;

    public static void startMainActivity(@NonNull Context context) {
        Intent intent;
        if (Preferences.isFirstStart()) {
            intent = new Intent(context, OnBoardingActivity.class);
            Preferences.setUpgradeTella2(false);
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

    public static void initKeys(MainKey mainKey) {
        getMainKeyHolder().set(mainKey);
        TellaKeysUI.getMainKeyHolder().set(mainKey);
    }

    public static CleanInsights getCleanInsights() {
        return cleanInsights;
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

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        //ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        BaseApi.Builder apiBuilder = new BaseApi.Builder();

        if (BuildConfig.DEBUG) {
            //  StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            ///        .detectAll().penaltyLog()/*.penaltyDeath()*/.build()); // todo: catch those..
            Timber.plant(new Timber.DebugTree());
            apiBuilder.setLogLevelFull();
        }
        // todo: implement dagger2
        SharedPrefs.getInstance().init(this);
        configureCrashlytics();

        // provide custom configuration
        Configuration myConfig = new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();

       //initialize WorkManager
      //  WorkManager.initialize(this, myConfig);

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
        vaultConfig = new Vault.Config();
        vaultConfig.root = new File(this.getFilesDir(), C.MEDIA_DIR);
        //Tella keys
        TellaKeys.initialize();
        initializeLockConfigRegistry();
        mainKeyStore = new MainKeyStore(getApplicationContext());
        //mainKeyHolder = new LifecycleMainKey(ProcessLifecycleOwner.get().getLifecycle(), LifecycleMainKey.NO_TIMEOUT);
        mainKeyHolder = new LifecycleMainKey(ProcessLifecycleOwner.get().getLifecycle(), Preferences.getLockTimeout());
        keyDataSource = new KeyDataSource(getApplicationContext());
        TellaKeysUI.initialize(mainKeyStore, mainKeyHolder, unlockRegistry, this);
        //initCleanInsights();
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
        // unlockRegistry.setActiveMethod(getApplicationContext(), UnlockRegistry.Method.TELLA_PATTERN);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        super.onLowMemory();
        Glide.get(this).clearMemory();
        persistCleanInsights();
    }

    @Override
    public void onSuccessfulUnlock(Context context) {
        mainKeyHolder = TellaKeysUI.getMainKeyHolder();
        mainKeyStore = TellaKeysUI.getMainKeyStore();
        unlockRegistry = TellaKeysUI.getUnlockRegistry();
        keyDataSource.initKeyDataSource();

        try {
            vault = new Vault(this, mainKeyHolder, vaultConfig);
            rxVault = new RxVault(this, vault);
            if (Preferences.isUpgradeTella2()) {
                Toast.makeText(context, "Hold tight while we transferring your files to the Vault!", Toast.LENGTH_LONG).show();
                upgradeTella2(context);
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        startMainActivity(context);
    }

    private void upgradeTella2(Context context) {
        try {
            byte[] key;
            if ((key = getMainKeyHolder().get().getKey().getEncoded()) != null) {
                TellaUpgrader.upgradeV2(context, key);
            }
        } catch (LifecycleMainKey.MainKeyUnavailableException e) {
            Timber.d(e);
        }
    }

    @Override
    public void onUnSuccessfulUnlock(String tag, Throwable throwable) {
        // FirebaseCrashlytics.getInstance().recordException(throwable);
    }

    @Override
    public void onLockConfirmed(Context context) {
        Preferences.setFirstStart(false);
        onSuccessfulUnlock(context);
    }

    @Override
    public void onUpdateUnlocking() {
        mainKeyHolder = TellaKeysUI.getMainKeyHolder();
        mainKeyStore = TellaKeysUI.getMainKeyStore();
        unlockRegistry = TellaKeysUI.getUnlockRegistry();
        keyDataSource.initKeyDataSource();
    }

    @Override
    public UnlockRegistry getUnlockRegistry() {
        return unlockRegistry;
    }

  /*  private void initCleanInsights() {
        if (Preferences.hasAcceptedImprovements()) {
            try {
                cleanInsights = createCleanInsightsInstance(getApplicationContext(), Preferences.getTimeAcceptedImprovements());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
*/
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        persistCleanInsights();
    }

    private void persistCleanInsights() {
        if (Preferences.hasAcceptedImprovements() && cleanInsights != null)
            CleanInsightUtils.INSTANCE.measureTimeSpentEvent(start);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        persistCleanInsights();
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }
}
