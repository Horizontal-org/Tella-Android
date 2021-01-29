package org.hzontal.tella.keys.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

public class Preferences {
    public static final String PREFERENCES = "org.hzontal.tella.keys";

    public static final String WRAPPED_MAIN_KEY = "WRAPPED_MAIN_KEY";


    public static boolean contains(Context context, String key) {
        return getPreferences(context).contains(key);
    }

    @SuppressLint("ApplySharedPref")
    public static void store(Context context, String key, int value) {
        // todo: move away from main thread
        getPreferences(context)
                .edit()
                .putInt(key, value)
                .commit();
    }

    public static void store(Context context, String key, byte[] value) {
        store(context, key, Base64.encodeToString(value, Base64.DEFAULT));
    }

    @SuppressLint("ApplySharedPref")
    public static void store(Context context, String key, String value) {
        // todo: move away from main thread
        getPreferences(context)
                .edit()
                .putString(key, value)
                .commit();
    }

    public static int load(Context context, String key, int defaultValue) {
        // todo: move away from main thread
        return getPreferences(context).getInt(key, defaultValue);
    }

    public static byte[] load(Context context, String key) {
        // todo: move away from main thread
        String encoded = getPreferences(context).getString(key, "");

        if (TextUtils.isEmpty(encoded)) {
            return null;
        }

        return Base64.decode(encoded, Base64.DEFAULT);
    }

    public static String load(Context context, String key, String defaultValue) {
        // todo: move away from main thread
        return getPreferences(context).getString(key, defaultValue);
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
}
