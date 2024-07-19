package org.hzontal.shared_ui.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.concurrent.Callable;
import timber.log.Timber;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;


public class CommonPrefs {
    public static final String NONE = "";
    private static final String SHARED_PREFERENCES_NAME = "tella_shared_preferences";

    static final String SHOW_IMPROVEMENT_SECTION = "show_improvement_section";
    static final String HAS_IMPROVEMENT_ACCEPTED = "has_improvement_accepted";
    static final String TIME_IMPROVEMENT_ACCEPTED = "time_improvement_accepted";

    private static CommonPrefs instance;
    private SharedPreferences commonPref;
    private SharedPreferences.Editor commonEditor;


    public static CommonPrefs getInstance() {
        synchronized (CommonPrefs.class) {
            if (instance == null) {
                instance = new CommonPrefs();
            }

            return instance;
        }
    }

    public SharedPreferences getPref() {
        return commonPref;
    }

    @SuppressLint("CommitPrefEdits")
    public void init(Context context) {
        commonPref = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        commonEditor = commonPref.edit();
    }

    boolean getBoolean(final String name, final boolean def) {
        Timber.d("++++ name, def %s %s", name, def);
        return Single.fromCallable(() -> commonPref.getBoolean(name, def))
                .subscribeOn(Schedulers.io()).blockingGet();
    }

    boolean setBoolean(final String name, final boolean value) {
        return Single.fromCallable(() -> {
            commonEditor.putBoolean(name, value);
            commonEditor.apply();
            return value;
        }).subscribeOn(Schedulers.io()).blockingGet();
    }

    @NonNull
    String getString(@NonNull final String name, final String def) {
        return Single.fromCallable(() -> {
            String str = commonPref.getString(name, def);
            return str != null ? str : NONE;
        }).subscribeOn(Schedulers.io()).blockingGet();
    }

    void setString(@NonNull final String name, final String value) {
        Completable.fromCallable((Callable<Void>) () -> {
            commonEditor.putString(name, value);
            commonEditor.apply();
            return null;
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    float getFloat(final String name, final float def) {
        return Single.fromCallable(() -> commonPref.getFloat(name, def))
                .subscribeOn(Schedulers.io()).blockingGet();
    }

    float setFloat(final String name, final float value) {
        return Single.fromCallable(() -> {
            commonEditor.putFloat(name, value);
            commonEditor.apply();
            return value;
        }).subscribeOn(Schedulers.io()).blockingGet();
    }

    long getLong(final String name, final long def) {
        return Single.fromCallable(() -> commonPref.getLong(name, def))
                .subscribeOn(Schedulers.io()).blockingGet();
    }

    long setLong(final String name, final long value) {
        return Single.fromCallable(() -> {
            commonEditor.putLong(name, value);
            commonEditor.apply();
            return value;
        }).subscribeOn(Schedulers.io()).blockingGet();
    }

    private CommonPrefs() {
    }
}
