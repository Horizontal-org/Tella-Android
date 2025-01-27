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
import io.reactivex.schedulers.Schedulers;
import org.horizontal.tella.mobile.data.sharedpref.SharedPrefs;


/**
 * Managing "forced" (non-system) locale in the app.
 */
public class LocaleManager {
    private static LocaleManager instance;
    private Locale appLocale;

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
        if (appLocale == null) { // nothing to do..
            return context;
        }

        // todo: ContextWrapper here?
        return getLocalizedContext(context, appLocale);
    }

    public void setLocale(@Nullable final Locale newLocale) {
        if (newLocale != null) {
            appLocale = newLocale;
        } else {
            appLocale = getSystemLocale();
        }

        setLanguageSetting(newLocale != null ? newLocale.getLanguage() : null);
    }

    @Nullable
    public String getLanguageSetting() {
        String language = Single.fromCallable(() -> {
            String language1 = SharedPrefs.getInstance().getAppLanguage();
            return language1 != null ? language1 : NONE_LANGUAGE;
        }).subscribeOn(Schedulers.io()).blockingGet();

        return NONE_LANGUAGE.equals(language) ? null : language;
    }

    private void setLanguageSetting(@Nullable final String language) {
        Completable.fromCallable((Callable<Void>) () -> {
            SharedPrefs.getInstance().setAppLanguage(language);
            return null;
        }).subscribeOn(Schedulers.io()).subscribe(); // leaks?
    }

    private Locale getSystemLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Resources.getSystem().getConfiguration().getLocales().get(0);
        } else {
            //noinspection deprecation
            return Resources.getSystem().getConfiguration().locale;
        }
    }

    @Nullable
    private Locale loadSavedLocale() {
        String language = getLanguageSetting();
        return language != null ? new Locale(language) : null;
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
        //LocaleList localeList = new LocaleList(locale);
        //LocaleList.setDefault(localeList);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        //configuration.setLocales(localeList);

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
}
