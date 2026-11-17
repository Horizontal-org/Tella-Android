package org.horizontal.tella.mobile.util;

import static org.horizontal.tella.mobile.MyApplication.getMainKeyHolder;

import java.util.LinkedHashMap;

import androidx.annotation.MainThread;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.data.sharedpref.Preferences;
import org.hzontal.tella.keys.key.LifecycleMainKey;


public class LockTimeoutManager {
    public static long IMMEDIATE_SHUTDOWN = 0L;
    public static long FIVE_MINUTES_SHUTDOWN = 300000L;
    public static long THREE_MINUTES_SHUTDOWN = 180000L;
    public static long ONE_MINUTES_SHUTDOWN = 60000L;
    private static int activeBackgroundWorkers;
    private static boolean pendingReportsHold;
    private static boolean temporaryTimeoutUsedWhilePaused;
    private final LinkedHashMap<Long, Integer> options;

    public LockTimeoutManager() {
        options = new LinkedHashMap<>();
        options.put(IMMEDIATE_SHUTDOWN,R.string.settings_sec_lock_timeout_immediately);
        options.put(60000L, R.string.settings_sec_lock_timeout_1min);
        options.put(FIVE_MINUTES_SHUTDOWN, R.string.settings_sec_lock_timeout_5min);
        options.put(1800000L, R.string.settings_sec_lock_timeout_30min);
        options.put(3600000L, R.string.settings_sec_lock_timeout_1hour);
    }

    public LinkedHashMap<Long, Integer> getOptionsList() {
        return options;
    }

    public long getLockTimeout() {
        return Preferences.getLockTimeout();
    }

    public int getSelectedStringRes() {
        int res = R.string.settings_sec_lock_timeout_immediately;
        for (long option : this.options.keySet()) {
            if (option == getLockTimeout()) {
                res = options.get(option) != null ? options.get(option) : R.string.settings_sec_lock_timeout_immediately;
            }
        }
        return res;
    }

    public void setLockTimeout(long timeout) {
        Preferences.setLockTimeout(timeout);
        setCurrentTimeout(timeout);
    }

    public static void setCurrentTimeout(long timeout) {
        LifecycleMainKey mainKeyHolder = getMainKeyHolder();
        synchronized (mainKeyHolder) {
            if (!isHeld() || mainKeyHolder.getTimeout() != LifecycleMainKey.NO_TIMEOUT) {
                mainKeyHolder.setTimeout(timeout);
            }
        }
    }

    @MainThread
    public static void setTemporaryTimeout() {
        setCurrentTimeout(THREE_MINUTES_SHUTDOWN);
        Preferences.setTempTimeout(true);
        temporaryTimeoutUsedWhilePaused = false;
    }

    @MainThread
    public static void onAppPaused() {
        temporaryTimeoutUsedWhilePaused = Preferences.isTempTimeout();
    }

    /** Restore only an allowance used during the absence that has just been checked. */
    @MainThread
    public static void restoreTemporaryTimeout() {
        if (temporaryTimeoutUsedWhilePaused && Preferences.isTempTimeout()) {
            setCurrentTimeout(Preferences.getLockTimeout());
            Preferences.setTempTimeout(false);
        }
        temporaryTimeoutUsedWhilePaused = false;
    }

    @MainThread
    public static void onBackgroundWorkStarted() throws LifecycleMainKey.MainKeyUnavailableException {
        LifecycleMainKey mainKeyHolder = getMainKeyHolder();
        synchronized (mainKeyHolder) {
            mainKeyHolder.get();
            activeBackgroundWorkers++;
            applyHold(mainKeyHolder);
        }
    }

    @MainThread
    public static void onBackgroundWorkFinished() {
        LifecycleMainKey mainKeyHolder = getMainKeyHolder();
        synchronized (mainKeyHolder) {
            if (activeBackgroundWorkers == 0) return;
            activeBackgroundWorkers--;
            applyHold(mainKeyHolder);
        }
    }

    @MainThread
    public static void holdForPendingReports() throws LifecycleMainKey.MainKeyUnavailableException {
        LifecycleMainKey mainKeyHolder = getMainKeyHolder();
        synchronized (mainKeyHolder) {
            mainKeyHolder.get();
            pendingReportsHold = true;
            applyHold(mainKeyHolder);
        }
    }

    @MainThread
    public static void releasePendingReportsHold() {
        LifecycleMainKey mainKeyHolder = getMainKeyHolder();
        synchronized (mainKeyHolder) {
            if (!pendingReportsHold) return;
            pendingReportsHold = false;
            applyHold(mainKeyHolder);
        }
    }

    public static void onMainKeyCleared() {
        LifecycleMainKey mainKeyHolder = getMainKeyHolder();
        synchronized (mainKeyHolder) {
            pendingReportsHold = false;
            if (activeBackgroundWorkers == 0 && mainKeyHolder.getTimeout() == LifecycleMainKey.NO_TIMEOUT) {
                mainKeyHolder.setTimeout(Preferences.getLockTimeout());
            }
        }
    }

    private static boolean isHeld() {
        return activeBackgroundWorkers > 0 || pendingReportsHold;
    }

    private static void applyHold(LifecycleMainKey mainKeyHolder) {
        boolean noTimeout = mainKeyHolder.getTimeout() == LifecycleMainKey.NO_TIMEOUT;
        if (isHeld() && !noTimeout) {
            refreshTimeout(mainKeyHolder, LifecycleMainKey.NO_TIMEOUT);
        } else if (!isHeld() && noTimeout) {
            refreshTimeout(mainKeyHolder, Preferences.getLockTimeout());
        }
    }

    private static void refreshTimeout(LifecycleMainKey mainKeyHolder, long timeout) {
        mainKeyHolder.setTimeout(LifecycleMainKey.NO_TIMEOUT);
        mainKeyHolder.activate();
        mainKeyHolder.setTimeout(timeout);
        if (!ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            mainKeyHolder.deactivate();
        }
    }
}
