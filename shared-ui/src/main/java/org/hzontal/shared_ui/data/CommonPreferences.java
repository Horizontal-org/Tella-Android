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

    public static Long getUnlockTime() {
        return getLong(CommonPrefs.UNLOCK_TIME, 0L);
    }

    public static void setUnlockTime(Long value) {
        setLong(CommonPrefs.UNLOCK_TIME, value);
    }


    public static Long getTimeSpent() {
        return getLong(CommonPrefs.TIME_SPENT, 0L);
    }

    public static void setTimeSpent(Long value) {
        setLong(CommonPrefs.TIME_SPENT, value);
    }

    public static boolean isShowVaultAnalyticsSection() {
        return getBoolean(CommonPrefs.SHOW_IMPROVEMENT_SECTION, true);
    }

    public static void setShowVaultAnalyticsSection(boolean value) {
        setBoolean(CommonPrefs.SHOW_IMPROVEMENT_SECTION, value);
    }

    public static boolean isInstallMetricSent() {
        return getBoolean(CommonPrefs.INSTALL_METRIC_SENT, false);
    }

    public static void setInstallMetricSent(boolean value) {
        setBoolean(CommonPrefs.INSTALL_METRIC_SENT, value);
    }

    public static boolean hasAcceptedAnalytics() {
        return getBoolean(CommonPrefs.HAS_IMPROVEMENT_ACCEPTED, false);
    }

    public static void setIsAcceptedAnalytics(boolean value) {
        setBoolean(CommonPrefs.HAS_IMPROVEMENT_ACCEPTED, value);
        setTimeAcceptedAnalytics(new Date().getTime());
    }

    public static Long getTimeAcceptedAnalytics() {
        return getLong(CommonPrefs.TIME_IMPROVEMENT_ACCEPTED, 0L);
    }

    private static void setTimeAcceptedAnalytics(Long value) {
        setLong(CommonPrefs.TIME_IMPROVEMENT_ACCEPTED, value);
    }

    public static boolean isTimeToShowReminderAnalytics() {
        if (getTimeAcceptedAnalytics() == 0L || !hasAcceptedAnalytics()) return false;
        Date currentDate = new Date();
        Date acceptedDatePlusSixMonths = new DateTime(new Date(getTimeAcceptedAnalytics())).plusMonths(6).toDate();
        return currentDate.getTime() > acceptedDatePlusSixMonths.getTime();
    }
}
