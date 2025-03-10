package org.horizontal.tella.mobile.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import org.horizontal.tella.mobile.data.sharedpref.SharedPrefs;

/**
 * Managing "forced" (non-system) locale in the app.
 */
public class LocaleManager {
    private static LocaleManager instance;
    private Locale appLocale;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private static final String NONE_LANGUAGE = "";

    public synchronized static LocaleManager getInstance() {
        if (instance == null) {
            instance = new LocaleManager();
        }
        return instance;
    }

    private LocaleManager() {
        appLocale = loadSavedLocale();
    }

    public Context getLocalizedContext(Context context) {
        if (appLocale == null) { // If no custom locale is set, return default context
            return context;
        }
        return getLocalizedContext(context, appLocale);
    }

    /**
     * Sets the app locale and saves it in SharedPreferences.
     */
    public void setLocale(@Nullable final Locale newLocale) {
        if (newLocale != null) {
            appLocale = newLocale;
        } else {
            appLocale = getSystemLocale();
        }

        // Store the FULL locale (language + region) instead of just the language.
        String localeString = (newLocale != null) ? newLocale.toLanguageTag() : null;
        setLanguageSetting(localeString);
    }

    /**
     * Gets the currently saved language setting.
     */
    @Nullable
    public String getLanguageSetting() {
        String localeTag = Single.fromCallable(() -> {
            String savedLocale = SharedPrefs.getInstance().getAppLanguage();
            return savedLocale != null ? savedLocale : NONE_LANGUAGE;
        }).subscribeOn(Schedulers.io()).blockingGet();

        return NONE_LANGUAGE.equals(localeTag) ? null : localeTag;
    }

    /**
     * Saves the full language setting (including region).
     */
    private void setLanguageSetting(@Nullable final String localeTag) {
        disposables.add(Completable.fromCallable((Callable<Void>) () -> {
            SharedPrefs.getInstance().setAppLanguage(localeTag);
            return null;
        }).subscribeOn(Schedulers.io()).subscribeWith(new io.reactivex.observers.DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                // Successfully saved
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }
        }));
    }

    /**
     * Gets the system default locale.
     */
    private Locale getSystemLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Resources.getSystem().getConfiguration().getLocales().get(0);
        } else {
            return Resources.getSystem().getConfiguration().locale;
        }
    }

    /**
     * Loads the saved locale from SharedPreferences.
     */
    @Nullable
    private Locale loadSavedLocale() {
        String localeTag = getLanguageSetting();
        return (localeTag != null) ? Locale.forLanguageTag(localeTag) : null;
    }

    private Context getLocalizedContext(Context context, Locale locale) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return updateResources(context, locale);
            }
            return updateResourcesLegacy(context, locale);
        } catch (Throwable ignored) {
            return context;
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Context updateResources(Context context, Locale locale) {
        Locale.setDefault(locale);
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        return context.createConfigurationContext(configuration);
    }

    @SuppressWarnings("deprecation")
    private static Context updateResourcesLegacy(Context context, Locale locale) {
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        configuration.setLayoutDirection(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }

    /**
     * Clean up resources.
     */
    public void dispose() {
        disposables.clear();
    }
}
