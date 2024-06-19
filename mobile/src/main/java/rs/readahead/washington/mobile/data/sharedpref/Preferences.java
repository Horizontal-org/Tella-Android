package rs.readahead.washington.mobile.data.sharedpref;


import static rs.readahead.washington.mobile.data.sharedpref.SharedPrefs.FAILED_UNLOCK_OPTION;
import static rs.readahead.washington.mobile.data.sharedpref.SharedPrefs.REMAINING_UNLOCK_ATTEMPTS;
import static rs.readahead.washington.mobile.data.sharedpref.SharedPrefs.SHOW_REMAINING_UNLOCK_ATTEMPTS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.hzontal.shared_ui.utils.CalculatorTheme;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Preferences {
    private static final SharedPrefs sharedPrefs = SharedPrefs.getInstance();

    // cache
    private static final Map<String, Boolean> bCache = new ConcurrentHashMap<>();

    public static boolean isSecretModeActive() {
        return getBoolean(SharedPrefs.SECRET_MODE_ENABLED, false);
    }

    public static void setSecretModeActive(boolean value) {
        setBoolean(SharedPrefs.SECRET_MODE_ENABLED, value);
    }

    public static boolean isAnonymousMode() {
        return getBoolean(SharedPrefs.ANONYMOUS_MODE, true);
    }

    public static void setAnonymousMode(boolean value) {
        setBoolean(SharedPrefs.ANONYMOUS_MODE, value);
    }

    public static boolean isBypassCensorship() {
        return getBoolean(SharedPrefs.BYPASS_CENSORSHIP, false);
    }

    public static void setBypassCensorship(boolean value) {
        setBoolean(SharedPrefs.BYPASS_CENSORSHIP, value);
    }

    public static boolean isUninstallOnPanic() {
        return getBoolean(SharedPrefs.UNINSTALL_ON_PANIC, false);
    }

    public static void setUninstallOnPanic(boolean value) {
        setBoolean(SharedPrefs.UNINSTALL_ON_PANIC, value);
    }

    public static boolean isSecurityScreenEnabled() {
        return getBoolean(SharedPrefs.SET_SECURITY_SCREEN, true);
    }

    public static void setSecurityScreenEnabled(boolean value) {
        setBoolean(SharedPrefs.SET_SECURITY_SCREEN, value);
    }

    public static boolean isFirstStart() {
        return getBoolean(SharedPrefs.APP_FIRST_START, true);
    }

    public static void setFirstStart(boolean value) {
        setBoolean(SharedPrefs.APP_FIRST_START, value);
    }

    public static boolean isQuickExit() {
        return getBoolean(SharedPrefs.QUICK_EXIT_BUTTON, false);
    }

    public static void setQuickExit(boolean value) {
        setBoolean(SharedPrefs.QUICK_EXIT_BUTTON, value);
    }

    public static boolean isCollectServersLayout() {
        return getBoolean(SharedPrefs.COLLECT_OPTION, false);
    }

    public static void setCollectServersLayout(boolean value) {
        setBoolean(SharedPrefs.COLLECT_OPTION, value);
    }

    public static boolean isShutterMute() {
        return getBoolean(SharedPrefs.MUTE_CAMERA_SHUTTER, true);
    }

    public static void setShutterMute(boolean value) {
        setBoolean(SharedPrefs.MUTE_CAMERA_SHUTTER, value);
    }

    public static boolean isKeepExif() {
        return getBoolean(SharedPrefs.KEEP_EXIF, false);
    }

    public static void setKeepExif(boolean value) {
        setBoolean(SharedPrefs.KEEP_EXIF, value);
    }

    public static boolean isDeleteServerSettingsActive() {
        return getBoolean(SharedPrefs.DELETE_SERVER_SETTINGS, true);
    }

    public static void setDeleteServerSettingsActive(boolean value) {
        setBoolean(SharedPrefs.DELETE_SERVER_SETTINGS, value);
    }

    public static boolean isEraseForms() {
        return getBoolean(SharedPrefs.ERASE_FORMS, true);
    }

    public static void setEraseForms(boolean value) {
        setBoolean(SharedPrefs.ERASE_FORMS, value);
    }

    public static boolean isPanicGeolocationActive() {
        return getBoolean(SharedPrefs.PANIC_GEOLOCATION, true);
    }

    public static void setPanicGeolocationActive(boolean value) {
        setBoolean(SharedPrefs.PANIC_GEOLOCATION, value);
    }

    @Nullable
    public static String getAppAlias() {
        return getString(SharedPrefs.APP_ALIAS_NAME, null);
    }

    public static void setAppAlias(@NonNull String value) {
        setString(SharedPrefs.APP_ALIAS_NAME, value);
    }

    public static String getCalculatorTheme() {
        return getString(SharedPrefs.CALCULATOR_THEME, CalculatorTheme.GREEN_SKIN.name());
    }

    public static void setCalculatorTheme(@NonNull String value) {
        setString(SharedPrefs.CALCULATOR_THEME, value);
    }

    @Nullable
    public static String getVideoResolution() {
        return getString(SharedPrefs.VIDEO_RESOLUTION, null);
    }

    public static void setVideoResolution(@NonNull String value) {
        setString(SharedPrefs.VIDEO_RESOLUTION, value);
    }

    public static String getSecretPassword() {
        return getString(SharedPrefs.SECRET_PASSWORD, "");
    }

    public static void setSecretPassword(@NonNull String value) {
        setString(SharedPrefs.SECRET_PASSWORD, value);
    }

    public static boolean isSubmittingCrashReports() {
        return getBoolean(SharedPrefs.SUBMIT_CRASH_REPORTS, true);
    }

    public static void setSubmittingCrashReports(boolean value) {
        setBoolean(SharedPrefs.SUBMIT_CRASH_REPORTS, value);
    }

    public static boolean isShowFavoriteForms() {
        return getBoolean(SharedPrefs.SHOW_FAVORITE_FORMS, false);
    }

    public static void setShowFavoriteForms(boolean value) {
        setBoolean(SharedPrefs.SHOW_FAVORITE_FORMS, value);
    }

    public static boolean isShowFavoriteTemplates() {
        return getBoolean(SharedPrefs.SHOW_FAVORITE_TEMPLATES, false);
    }

    public static void setShowFavoriteTemplates(boolean value) {
        setBoolean(SharedPrefs.SHOW_FAVORITE_TEMPLATES, value);
    }

    public static boolean isShowRecentFiles() {
        return getBoolean(SharedPrefs.SHOW_RECENT_FILES, false);
    }

    public static void setShowRecentFiles(boolean value) {
        setBoolean(SharedPrefs.SHOW_RECENT_FILES, value);
    }

    public static boolean isTextJustification() {
        return getBoolean(SharedPrefs.TEXT_JUSTIFICATION, false);
    }

    public static void setTextJustification(boolean value) {
        setBoolean(SharedPrefs.TEXT_JUSTIFICATION, value);
    }

    public static boolean isTextSpacing() {
        return getBoolean(SharedPrefs.TEXT_SPACING, false);
    }

    public static void setTextSpacing(boolean value) {
        setBoolean(SharedPrefs.TEXT_SPACING, value);
    }

    public static boolean isUpgradeTella2() {
        return getBoolean(SharedPrefs.UPGRADE_TELLA_2, true);
    }

    public static void setUpgradeTella2(boolean value) {
        setBoolean(SharedPrefs.UPGRADE_TELLA_2, value);
    }

    public static boolean isCameraPreviewEnabled() {
        return getBoolean(SharedPrefs.ENABLE_CAMERA_PREVIEW, false);
    }

    public static void setCameraPreviewEnabled(boolean value) {
        setBoolean(SharedPrefs.ENABLE_CAMERA_PREVIEW, value);
    }

    public static boolean isDeleteGalleryEnabled() {
        return getBoolean(SharedPrefs.ERASE_GALLERY, false);
    }

    public static void setDeleteGallery(boolean value) {
        setBoolean(SharedPrefs.ERASE_GALLERY, value);
    }

    public static float getLocationAccuracyThreshold() {
        return getFloat(SharedPrefs.LOCATION_ACCURACY_THRESHOLD, 5f);
    }

    public static boolean isOfflineMode() {
        return getBoolean(SharedPrefs.OFFLINE_MODE, false);
    }

    public static void setOfflineMode(boolean value) {
        setBoolean(SharedPrefs.OFFLINE_MODE, value);
    }

    @Nullable
    public static String getPanicMessage() {
        return getString(SharedPrefs.PANIC_MESSAGE, null);
    }

    public static void setPanicMessage(@Nullable String value) {
        setString(SharedPrefs.PANIC_MESSAGE, value);
    }

    @Nullable
    public static String getInstallationId() {
        return getString(SharedPrefs.INSTALLATION_ID, null);
    }

    public static void setInstallationId(@Nullable String value) {
        setString(SharedPrefs.INSTALLATION_ID, value);
    }

    @Nullable
    public static long getLastCollectRefresh() {
        return getLong(SharedPrefs.LAST_COLLECT_REFRESH, 0);
    }

    public static void setLastCollectRefresh(@Nullable long value) {
        setLong(SharedPrefs.LAST_COLLECT_REFRESH, value);
    }

    @Nullable
    public static long getAutoUploadServerId() {
        return getLong(SharedPrefs.AUTO_UPLOAD_SERVER, -1);
    }

    public static void setAutoUploadServerId(@Nullable long value) {
        setLong(SharedPrefs.AUTO_UPLOAD_SERVER, value);
    }

    public static boolean isAutoUploadEnabled() {
        return getBoolean(SharedPrefs.AUTO_UPLOAD, false);
    }

    public static boolean isAutoUploadPaused() {
        return getBoolean(SharedPrefs.AUTO_UPLOAD_PAUSED, false);
    }

    @Nullable
    public static long getLockTimeout() {
        return getLong(SharedPrefs.LOCK_TIMEOUT, 0);
    }

    public static void setLockTimeout(@Nullable long value) {
        setLong(SharedPrefs.LOCK_TIMEOUT, value);
    }

    public static Long getFailedUnlockOption() {
        return getLong(FAILED_UNLOCK_OPTION, 0);
    }

    public static void setFailedUnlockOption(Long option) {
        setLong(FAILED_UNLOCK_OPTION, option);
    }

    public static boolean isShowUnlockRemainingAttempts() {
        return getBoolean(SHOW_REMAINING_UNLOCK_ATTEMPTS, true);
    }

    public static void setShowUnlockRemainingAttempts(boolean option) {
        setBoolean(SHOW_REMAINING_UNLOCK_ATTEMPTS, option);
    }

    public static long getUnlockRemainingAttempts() {
        return getLong(REMAINING_UNLOCK_ATTEMPTS, 0);
    }

    public static void setUnlockRemainingAttempts(long option) {
        setLong(REMAINING_UNLOCK_ATTEMPTS, option);
    }

    public static boolean isTempTimeout() {
        return getBoolean(SharedPrefs.TEMP_TIMEOUT, false);
    }

    public static void setTempTimeout(boolean value) {
        setBoolean(SharedPrefs.TEMP_TIMEOUT, value);
    }

    public static boolean isExitTimeout() {
        return getBoolean(SharedPrefs.EXIT_TIMEOUT, false);
    }

    public static void setExitTimeout(boolean value) {
        setBoolean(SharedPrefs.EXIT_TIMEOUT, value);
    }

    public static boolean isJavarosa3Upgraded() {
        return getBoolean(SharedPrefs.JAVAROSA_3_UPGRADE, false);
    }

    public static void setJavarosa3Upgraded(boolean value) {
        setBoolean(SharedPrefs.JAVAROSA_3_UPGRADE, value);
    }

    public static void setAutoUpload(boolean value) {
        setBoolean(SharedPrefs.AUTO_UPLOAD, value);
    }

    public static void setAutoUploadPased(boolean value) {
        setBoolean(SharedPrefs.AUTO_UPLOAD_PAUSED, value);
    }

    public static boolean isAutoDeleteEnabled() {
        return getBoolean(SharedPrefs.AUTO_DELETE, false);
    }

    public static void setAutoDelete(boolean value) {
        setBoolean(SharedPrefs.AUTO_DELETE, value);
    }

    public static boolean isMetadataAutoUpload() {
        return getBoolean(SharedPrefs.METADATA_AUTO_UPLOAD, false);
    }

    public static void setMetadataAutoUpload(boolean value) {
        setBoolean(SharedPrefs.METADATA_AUTO_UPLOAD, value);
    }

    private static boolean getBoolean(String name, boolean def) {
        Boolean value = bCache.get(name);

        if (value == null) {
            value = sharedPrefs.getBoolean(name, def);
            bCache.put(name, value);
        }

        return value;
    }

    private static void setBoolean(String name, boolean value) {
        bCache.put(name, sharedPrefs.setBoolean(name, value));
    }

    @Nullable
    private static String getString(@NonNull String name, String def) {
        String value = sharedPrefs.getString(name, def);
        //noinspection StringEquality
        return (value != SharedPrefs.NONE ? value : def);
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

    public static boolean isShowVaultImprovementSection() {
        return getBoolean(SharedPrefs.SHOW_IMPROVEMENT_SECTION, true);
    }

    public static void setShowVaultImprovementSection(boolean value) {
        setBoolean(SharedPrefs.SHOW_IMPROVEMENT_SECTION, value);
    }

    public static boolean isShowUpdateMigrationSheet() {
        return getBoolean(SharedPrefs.SHOW_UPDATE_MIGRATION_BOTTOM_SHEET, true);
    }

    public static void setShowUpdateMigrationSheet(boolean value) {
        setBoolean(SharedPrefs.SHOW_UPDATE_MIGRATION_BOTTOM_SHEET, value);
    }

    public static boolean hasAcceptedImprovements() {
        return getBoolean(SharedPrefs.HAS_IMPROVEMENT_ACCEPTED, false);
    }

    public static void setIsAcceptedImprovements(boolean value) {
        setBoolean(SharedPrefs.HAS_IMPROVEMENT_ACCEPTED, value);
        setTimeAcceptedImprovements(new Date().getTime());
    }

    public static Long getTimeAcceptedImprovements() {
        return getLong(SharedPrefs.TIME_IMPROVEMENT_ACCEPTED, 0L);
    }

    private static void setTimeAcceptedImprovements(Long value) {
        setLong(SharedPrefs.TIME_IMPROVEMENT_ACCEPTED, value);
    }

    public static boolean isTimeToShowReminderImprovements() {
        if (getTimeAcceptedImprovements() == 0L || !hasAcceptedImprovements()) return false;
        Date currentDate = new Date();
        Date acceptedDatePlusSixMonths = new DateTime(new Date(getTimeAcceptedImprovements())).plusMonths(6).toDate();
        return currentDate.getTime() > acceptedDatePlusSixMonths.getTime();
    }

    public static boolean isFeedbackSharingEnabled() {
        return getBoolean(SharedPrefs.FEEDBACK_SHARING_ENBALED, false);
    }

    public static void setFeedbackSharingEnabled(boolean value) {
        setBoolean(SharedPrefs.FEEDBACK_SHARING_ENBALED, value);
    }
}
