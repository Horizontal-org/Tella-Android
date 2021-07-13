package rs.readahead.washington.mobile.data.sharedpref;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Preferences {
    private static SharedPrefs sharedPrefs = SharedPrefs.getInstance();

    // cache
    private static Map<String, Boolean> bCache = new ConcurrentHashMap<>();


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

    public static boolean isDomainFronting() {
        return getBoolean(SharedPrefs.DOMAIN_FRONTING, false);
    }

    public static void setDomainFronting(boolean value) {
        setBoolean(SharedPrefs.DOMAIN_FRONTING, value);
    }

    public static boolean isUninstallOnPanic() {
        return getBoolean(SharedPrefs.UNINSTALL_ON_PANIC, false);
    }

    public static void setUninstallOnPanic(boolean value) {
        setBoolean(SharedPrefs.UNINSTALL_ON_PANIC, value);
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
        return getLong(SharedPrefs.LAST_COLLECT_REFRESH, 0);
    }

    public static void setLockTimeout(@Nullable long value) {
        setLong(SharedPrefs.LAST_COLLECT_REFRESH, value);
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

    private Preferences() {
    }
}
