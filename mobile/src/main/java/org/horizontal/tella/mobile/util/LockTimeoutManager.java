package org.horizontal.tella.mobile.util;

import static org.horizontal.tella.mobile.MyApplication.getMainKeyHolder;

import java.util.LinkedHashMap;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.data.sharedpref.Preferences;


public class LockTimeoutManager {
    public static long IMMEDIATE_SHUTDOWN = 0L;
    public static long FIVE_MINUTES_SHUTDOWN = 300000L;
    public static long THREE_MINUTES_SHUTDOWN = 180000L;
    public static long ONE_MINUTES_SHUTDOWN = 60000L;
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
        getMainKeyHolder().setTimeout(timeout);
    }
}