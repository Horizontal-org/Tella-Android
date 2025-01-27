package org.horizontal.tella.mobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.StrictMode;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;
import androidx.work.Configuration;

import com.bumptech.glide.Glide;
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
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import org.hzontal.shared_ui.data.CommonPrefs;

import org.conscrypt.Conscrypt;
import org.hzontal.tella.keys.MainKeyStore;
import org.hzontal.tella.keys.TellaKeys;
import org.hzontal.tella.keys.config.IUnlockRegistryHolder;
import org.hzontal.tella.keys.config.UnencryptedUnlocker;
import org.hzontal.tella.keys.config.UnlockConfig;
import org.hzontal.tella.keys.config.UnlockRegistry;
import org.hzontal.tella.keys.key.LifecycleMainKey;
import org.hzontal.tella.keys.wrapper.AndroidKeyStoreWrapper;
import org.hzontal.tella.keys.wrapper.PBEKeyWrapper;
import org.hzontal.tella.keys.wrapper.UnencryptedKeyWrapper;

import java.io.File;
import java.lang.ref.WeakReference;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import dagger.hilt.android.HiltAndroidApp;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import org.horizontal.tella.mobile.bus.TellaBus;
import org.horizontal.tella.mobile.data.database.KeyDataSource;
import org.horizontal.tella.mobile.data.nextcloud.TempFileManager;
import org.horizontal.tella.mobile.data.rest.BaseApi;
import org.horizontal.tella.mobile.data.sharedpref.Preferences;
import org.horizontal.tella.mobile.data.sharedpref.SharedPrefs;
import org.horizontal.tella.mobile.javarosa.JavaRosa;
import org.horizontal.tella.mobile.javarosa.PropertyManager;
import org.horizontal.tella.mobile.media.MediaFileHandler;
import org.horizontal.tella.mobile.util.C;
import org.horizontal.tella.mobile.util.LocaleManager;
import org.horizontal.tella.mobile.util.TellaUpgrader;
import org.horizontal.tella.mobile.util.divviup.DivviupUtils;
import org.horizontal.tella.mobile.views.activity.ExitActivity;
import org.horizontal.tella.mobile.views.activity.MainActivity;
import org.horizontal.tella.mobile.views.activity.onboarding.OnBoardingActivity;

import timber.log.Timber;

@HiltAndroidApp
public class MyApplication extends MultiDexApplication implements IUnlockRegistryHolder, CredentialsCallback, Configuration.Provider, Application.ActivityLifecycleCallbacks {
    public static Vault vault;
    public static RxVault rxVault;
    private static TellaBus bus;
    private static LifecycleMainKey mainKeyHolder;
    @SuppressLint("StaticFieldLeak")
    private static MainKeyStore mainKeyStore;
    private static UnlockRegistry unlockRegistry;
    private static KeyDataSource keyDataSource;
    @Inject
    public HiltWorkerFactory workerFactory;
    @Inject
    DivviupUtils divviupUtils;
    Vault.Config vaultConfig;
    private static final String TAG = MyApplication.class.getSimpleName();
    public static final String DOT = ".";
    public static final OwnCloudVersion MINIMUM_SUPPORTED_SERVER_VERSION = OwnCloudVersion.nextcloud_17;
    private static WeakReference<Context> appContext;
    private long startTime;
    private long totalTimeSpent = 0; // Store total time spent in the app
    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;


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
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static void exit(Context context) {
        Intent intent = new Intent(context, ExitActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        context.startActivity(intent);
    }

    private static void maybeExcludeIntentFromRecents(Intent intent) {
        if (Preferences.isSecretModeActive()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS/* |
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

    @Override
    protected void attachBaseContext(Context newBase) {
        CommonPrefs.getInstance().init(newBase);
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
        CommonPrefs.getInstance().init(this);
        SharedPrefs.getInstance().init(this);
        configureCrashlytics();
        System.loadLibrary("sqlcipher");

        registerActivityLifecycleCallbacks(this);

        // provide custom configuration
     /*   Configuration myConfig = new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();*/

        //initialize WorkManager
        //  WorkManager.initialize(this, myConfig);

        System.setProperty("javax.net.debug", "ssl,handshake");


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
        TellaKeysUI.initialize(mainKeyStore, mainKeyHolder, unlockRegistry, this, Preferences.getFailedUnlockOption(), Preferences.getUnlockRemainingAttempts(), Preferences.isShowUnlockRemainingAttempts());
        insertConscrypt();
        enableStrictMode();
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

        unlockRegistry.registerConfig(UnlockRegistry.Method.DISABLED, new UnlockConfig(new UnencryptedUnlocker(), new UnencryptedKeyWrapper()));

        unlockRegistry.registerConfig(UnlockRegistry.Method.TELLA_PIN, new UnlockConfig(new AppCompatActivityUnlocker(unlockRegistry, PinUnlockActivity.class), pbeKeyWrapper));

        unlockRegistry.registerConfig(UnlockRegistry.Method.TELLA_PATTERN, new UnlockConfig(new AppCompatActivityUnlocker(unlockRegistry, PatternUnlockActivity.class), pbeKeyWrapper));

        unlockRegistry.registerConfig(UnlockRegistry.Method.TELLA_PASSWORD, new UnlockConfig(new AppCompatActivityUnlocker(unlockRegistry, PasswordUnlockActivity.class), pbeKeyWrapper));

        unlockRegistry.registerConfig(UnlockRegistry.Method.DEVICE_CREDENTIALS, new UnlockConfig(new AppCompatActivityUnlocker(unlockRegistry, DeviceCredentialsUnlockActivity.class), androidKeyStoreWrapper));

        unlockRegistry.registerConfig(UnlockRegistry.Method.DEVICE_CREDENTIALS_BIOMETRICS, new UnlockConfig(new AppCompatActivityUnlocker(unlockRegistry, DeviceCredentialsUnlockActivity.class), androidKeyStoreWrapper));

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
        divviupUtils.runUnlockEvent();
        divviupUtils.runInstallEvent();

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
    public void onFailedAttempts(long num) {
        ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
    }

    @Override
    public void saveRemainingAttempts(long num) {
        Preferences.setUnlockRemainingAttempts(num);
    }

    @Override
    public UnlockRegistry getUnlockRegistry() {
        return unlockRegistry;
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder().setMinimumLoggingLevel(android.util.Log.DEBUG).setWorkerFactory(workerFactory).build();
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            // App enters foreground
            startTime = System.currentTimeMillis(); // Start tracking time
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            // App enters background
            long spentTime = System.currentTimeMillis() - startTime;
            totalTimeSpent += spentTime; // Add to total time spent
            Preferences.setTimeSpent(totalTimeSpent); // Save the time to shared preferences
            divviupUtils.runTimeSpentEvent(totalTimeSpent); // Send analytics if needed
            TempFileManager.INSTANCE.deleteAllFiles();
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }

    private void insertConscrypt() {
        Security.insertProviderAt(Conscrypt.newProvider(), 1);

        try {
            Conscrypt.Version version = Conscrypt.version();
            Log_OC.i(TAG, "Using Conscrypt/"
                    + version.major()
                    + DOT
                    + version.minor()
                    + DOT + version.patch()
                    + " for TLS");
            SSLEngine engine = SSLContext.getDefault().createSSLEngine();
            Log_OC.i(TAG, "Enabled protocols: " + Arrays.toString(engine.getEnabledProtocols()) + " }");
            Log_OC.i(TAG, "Enabled ciphers: " + Arrays.toString(engine.getEnabledCipherSuites()) + " }");
        } catch (NoSuchAlgorithmException e) {
            Log_OC.e(TAG, e.getMessage());
        }
    }


    private void enableStrictMode() {
        if (BuildConfig.DEBUG) {
            Log_OC.d(TAG, "Enabling StrictMode");
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build());
        }
    }


}
