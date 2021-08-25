package rs.readahead.washington.mobile.util;

import static rs.readahead.washington.mobile.MyApplication.getMainKeyHolder;

import java.util.LinkedHashMap;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;


public class LockTimeoutManager {
    private final LinkedHashMap<Long, Integer> options;

    public LockTimeoutManager() {
        options = new LinkedHashMap<>();
        options.put(0L,R.string.settings_sec_lock_timeout_immediately);
        options.put(60000L, R.string.settings_sec_lock_timeout_1min);
        options.put(300000L, R.string.settings_sec_lock_timeout_5min);
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