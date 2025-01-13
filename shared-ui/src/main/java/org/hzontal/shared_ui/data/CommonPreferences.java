package org.hzontal.shared_ui.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.joda.time.DateTime;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class CommonPreferences {
    private static final CommonPrefs sharedPrefs = CommonPrefs.getInstance();

    // cache
    private static final Map<String, Boolean> cbCache = new ConcurrentHashMap<>();

    private static boolean getBoolean(String name, boolean def) {
        Boolean value = cbCache.get(name);

        if (value == null) {
            value = sharedPrefs.getBoolean(name, def);
            cbCache.put(name, value);
        }

        return value;
    }

    private static void setBoolean(String name, boolean value) {
        cbCache.put(name, sharedPrefs.setBoolean(name, value));
    }

    @Nullable
    private static String getString(@NonNull String name, String def) {
        String value = sharedPrefs.getString(name, def);
        return (!value.equals(CommonPrefs.NONE) ? value : def);
    }

    private static void setString(@NonNull String name, String value) {
        sharedPrefs.setString(name, value);
    }

    private static float getFloat(@NonNull String name, float def) {
        return sharedPrefs.getFloat(name, def);
    }

    private static void setFloat(@NonNull String name, float value) {
        sharedPrefs.setFloat(name, value);
    }

    private static long getLong(@NonNull String name, long def) {
        return sharedPrefs.getLong(name, def);
    }

    private static void setLong(@NonNull String name, long value) {
        sharedPrefs.setLong(name, value);
    }
}
