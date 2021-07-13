package rs.readahead.washington.mobile.util;


import java.util.HashMap;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;


public class LockTimeoutManager {
    private final HashMap<Long, Integer> options;

    public LockTimeoutManager() {
        options = new HashMap<> ();
        options.put(0L,R.string.settings_sec_lock_timeout_immediately);
        options.put(60000L, R.string.settings_sec_lock_timeout_1min);
        options.put(300000L, R.string.settings_sec_lock_timeout_5min);
        options.put(1800000L, R.string.settings_sec_lock_timeout_30min);
        options.put(3600000L, R.string.settings_sec_lock_timeout_1hour);
    }

    public HashMap<Long, Integer> getOptionsList() {
        return options;
    }

    public long getLockTimeout() {
        return Preferences.getLockTimeout();
    }

    public int getSelectedStringRes() {
        int res = R.string.settings_sec_lock_timeout_immediately;
        for (long option : this.options.keySet()) {
            if (option == getLockTimeout() && options.get(option)!= null) {
                res = options.get(option);
            }
        }
        return res;
    }

    public void setLockTimeout(long timeout) {
        Preferences.setLockTimeout(timeout);
    }
}
