package org.horizontal.tella.core_preferences

import org.horizontal.tella.core_preferences.SharedPrefs.FAILED_UNLOCK_OPTION
import org.horizontal.tella.core_preferences.SharedPrefs.HAS_IMPROVEMENT_ACCEPTED
import org.horizontal.tella.core_preferences.SharedPrefs.INSTALL_METRIC_SENT
import org.horizontal.tella.core_preferences.SharedPrefs.IS_FRESH_INSTALL
import org.horizontal.tella.core_preferences.SharedPrefs.IS_MIGRATED_MAIN_DB
import org.horizontal.tella.core_preferences.SharedPrefs.REMAINING_UNLOCK_ATTEMPTS
import org.horizontal.tella.core_preferences.SharedPrefs.SHOW_IMPROVEMENT_SECTION
import org.horizontal.tella.core_preferences.SharedPrefs.SHOW_REMAINING_UNLOCK_ATTEMPTS
import org.horizontal.tella.core_preferences.SharedPrefs.TIME_IMPROVEMENT_ACCEPTED
import org.horizontal.tella.core_preferences.SharedPrefs.TIME_SPENT
import org.horizontal.tella.core_preferences.SharedPrefs.UNLOCK_TIME
import org.joda.time.DateTime
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

object Preferences {
    private val sharedPrefs: SharedPrefs = SharedPrefs.getInstance()

    // cache
    private val bCache: MutableMap<String, Boolean> = ConcurrentHashMap()

    fun isSecretModeActive(): Boolean =
        getBoolean(SharedPrefs.SECRET_MODE_ENABLED, false)

    fun setSecretModeActive(value: Boolean) {
        setBoolean(SharedPrefs.SECRET_MODE_ENABLED, value)
    }

    fun isAnonymousMode(): Boolean =
        getBoolean(SharedPrefs.ANONYMOUS_MODE, true)

    fun setAnonymousMode(value: Boolean) {
        setBoolean(SharedPrefs.ANONYMOUS_MODE, value)
    }

    fun isBypassCensorship(): Boolean =
        getBoolean(SharedPrefs.BYPASS_CENSORSHIP, false)

    fun setBypassCensorship(value: Boolean) {
        setBoolean(SharedPrefs.BYPASS_CENSORSHIP, value)
    }

    fun isUninstallOnPanic(): Boolean =
        getBoolean(SharedPrefs.UNINSTALL_ON_PANIC, false)

    fun setUninstallOnPanic(value: Boolean) {
        setBoolean(SharedPrefs.UNINSTALL_ON_PANIC, value)
    }

    fun isSecurityScreenEnabled(): Boolean =
        getBoolean(SharedPrefs.SET_SECURITY_SCREEN, true)

    fun setSecurityScreenEnabled(value: Boolean) {
        setBoolean(SharedPrefs.SET_SECURITY_SCREEN, value)
    }

    fun isFirstStart(): Boolean =
        getBoolean(SharedPrefs.APP_FIRST_START, true)

    fun setFirstStart(value: Boolean) {
        setBoolean(SharedPrefs.APP_FIRST_START, value)
    }

    fun isQuickExit(): Boolean =
        getBoolean(SharedPrefs.QUICK_EXIT_BUTTON, false)

    fun setQuickExit(value: Boolean) {
        setBoolean(SharedPrefs.QUICK_EXIT_BUTTON, value)
    }

    fun isCollectServersLayout(): Boolean =
        getBoolean(SharedPrefs.COLLECT_OPTION, false)

    fun setCollectServersLayout(value: Boolean) {
        setBoolean(SharedPrefs.COLLECT_OPTION, value)
    }

    fun isShutterMute(): Boolean =
        getBoolean(SharedPrefs.MUTE_CAMERA_SHUTTER, true)

    fun setShutterMute(value: Boolean) {
        setBoolean(SharedPrefs.MUTE_CAMERA_SHUTTER, value)
    }

    fun isKeepExif(): Boolean =
        getBoolean(SharedPrefs.KEEP_EXIF, false)

    fun setKeepExif(value: Boolean) {
        setBoolean(SharedPrefs.KEEP_EXIF, value)
    }

    fun isDeleteServerSettingsActive(): Boolean =
        getBoolean(SharedPrefs.DELETE_SERVER_SETTINGS, true)

    fun setDeleteServerSettingsActive(value: Boolean) {
        setBoolean(SharedPrefs.DELETE_SERVER_SETTINGS, value)
    }

    fun isEraseForms(): Boolean =
        getBoolean(SharedPrefs.ERASE_FORMS, true)

    fun setEraseForms(value: Boolean) {
        setBoolean(SharedPrefs.ERASE_FORMS, value)
    }

    fun isPanicGeolocationActive(): Boolean =
        getBoolean(SharedPrefs.PANIC_GEOLOCATION, true)

    fun setPanicGeolocationActive(value: Boolean) {
        setBoolean(SharedPrefs.PANIC_GEOLOCATION, value)
    }

    fun getAppAlias(): String? =
        getString(SharedPrefs.APP_ALIAS_NAME, null)

    fun setAppAlias(value: String) {
        setString(SharedPrefs.APP_ALIAS_NAME, value)
    }

    fun getCalculatorTheme(): String =
        getString(SharedPrefs.CALCULATOR_THEME, CalculatorTheme.GREEN_SKIN.name) ?: CalculatorTheme.GREEN_SKIN.name

    fun setCalculatorTheme(value: String) {
        setString(SharedPrefs.CALCULATOR_THEME, value)
    }

    fun getVideoResolution(): String? =
        getString(SharedPrefs.VIDEO_RESOLUTION, null)

    fun setVideoResolution(value: String) {
        setString(SharedPrefs.VIDEO_RESOLUTION, value)
    }

    fun getSecretPassword(): String =
        getString(SharedPrefs.SECRET_PASSWORD, "") ?: ""

    fun setSecretPassword(value: String) {
        setString(SharedPrefs.SECRET_PASSWORD, value)
    }

    fun isSubmittingCrashReports(): Boolean =
        getBoolean(SharedPrefs.SUBMIT_CRASH_REPORTS, true)

    fun setSubmittingCrashReports(value: Boolean) {
        setBoolean(SharedPrefs.SUBMIT_CRASH_REPORTS, value)
    }

    fun isShowFavoriteForms(): Boolean =
        getBoolean(SharedPrefs.SHOW_FAVORITE_FORMS, false)

    fun setShowFavoriteForms(value: Boolean) {
        setBoolean(SharedPrefs.SHOW_FAVORITE_FORMS, value)
    }

    fun isShowFavoriteTemplates(): Boolean =
        getBoolean(SharedPrefs.SHOW_FAVORITE_TEMPLATES, false)

    fun setShowFavoriteTemplates(value: Boolean) {
        setBoolean(SharedPrefs.SHOW_FAVORITE_TEMPLATES, value)
    }

    fun isShowRecentFiles(): Boolean =
        getBoolean(SharedPrefs.SHOW_RECENT_FILES, false)

    fun setShowRecentFiles(value: Boolean) {
        setBoolean(SharedPrefs.SHOW_RECENT_FILES, value)
    }

    fun isTextJustification(): Boolean =
        getBoolean(SharedPrefs.TEXT_JUSTIFICATION, false)

    fun setTextJustification(value: Boolean) {
        setBoolean(SharedPrefs.TEXT_JUSTIFICATION, value)
    }

    fun isTextSpacing(): Boolean =
        getBoolean(SharedPrefs.TEXT_SPACING, false)

    fun setTextSpacing(value: Boolean) {
        setBoolean(SharedPrefs.TEXT_SPACING, value)
    }

    fun isUpgradeTella2(): Boolean =
        getBoolean(SharedPrefs.UPGRADE_TELLA_2, true)

    fun setUpgradeTella2(value: Boolean) {
        setBoolean(SharedPrefs.UPGRADE_TELLA_2, value)
    }

    fun isCameraPreviewEnabled(): Boolean =
        getBoolean(SharedPrefs.ENABLE_CAMERA_PREVIEW, false)

    fun setCameraPreviewEnabled(value: Boolean) {
        setBoolean(SharedPrefs.ENABLE_CAMERA_PREVIEW, value)
    }

    fun isDeleteGalleryEnabled(): Boolean =
        getBoolean(SharedPrefs.ERASE_GALLERY, true)

    fun setDeleteGallery(value: Boolean) {
        setBoolean(SharedPrefs.ERASE_GALLERY, value)
    }

    fun getLocationAccuracyThreshold(): Float =
        getFloat(SharedPrefs.LOCATION_ACCURACY_THRESHOLD, 5f)

    fun isOfflineMode(): Boolean =
        getBoolean(SharedPrefs.OFFLINE_MODE, false)

    fun setOfflineMode(value: Boolean) {
        setBoolean(SharedPrefs.OFFLINE_MODE, value)
    }

    fun getPanicMessage(): String? =
        getString(SharedPrefs.PANIC_MESSAGE, null)

    fun setPanicMessage(value: String?) {
        setString(SharedPrefs.PANIC_MESSAGE, value)
    }

    fun getInstallationId(): String? =
        getString(SharedPrefs.INSTALLATION_ID, null)

    fun setInstallationId(value: String?) {
        setString(SharedPrefs.INSTALLATION_ID, value)
    }

    fun getLastCollectRefresh(): Long =
        getLong(SharedPrefs.LAST_COLLECT_REFRESH, 0L)

    fun setLastCollectRefresh(value: Long) {
        setLong(SharedPrefs.LAST_COLLECT_REFRESH, value)
    }

    fun getAutoUploadServerId(): Long =
        getLong(SharedPrefs.AUTO_UPLOAD_SERVER, -1L)

    fun setAutoUploadServerId(value: Long) {
        setLong(SharedPrefs.AUTO_UPLOAD_SERVER, value)
    }

    fun isAutoUploadEnabled(): Boolean =
        getBoolean(SharedPrefs.AUTO_UPLOAD, false)

    fun isAutoUploadPaused(): Boolean =
        getBoolean(SharedPrefs.AUTO_UPLOAD_PAUSED, false)

    fun getLockTimeout(): Long =
        getLong(SharedPrefs.LOCK_TIMEOUT, 0L)

    fun setLockTimeout(value: Long) {
        setLong(SharedPrefs.LOCK_TIMEOUT, value)
    }

    fun getFailedUnlockOption(): Long =
        getLong(FAILED_UNLOCK_OPTION, 0L)

    fun setFailedUnlockOption(option: Long) {
        setLong(FAILED_UNLOCK_OPTION, option)
    }

    fun isShowUnlockRemainingAttempts(): Boolean =
        getBoolean(SHOW_REMAINING_UNLOCK_ATTEMPTS, true)

    fun setShowUnlockRemainingAttempts(option: Boolean) {
        setBoolean(SHOW_REMAINING_UNLOCK_ATTEMPTS, option)
    }

    fun getUnlockRemainingAttempts(): Long =
        getLong(REMAINING_UNLOCK_ATTEMPTS, 0L)

    fun setUnlockRemainingAttempts(option: Long) {
        setLong(REMAINING_UNLOCK_ATTEMPTS, option)
    }

    fun isTempTimeout(): Boolean =
        getBoolean(SharedPrefs.TEMP_TIMEOUT, false)

    fun setTempTimeout(value: Boolean) {
        setBoolean(SharedPrefs.TEMP_TIMEOUT, value)
    }

    fun isExitTimeout(): Boolean =
        getBoolean(SharedPrefs.EXIT_TIMEOUT, false)

    fun setExitTimeout(value: Boolean) {
        setBoolean(SharedPrefs.EXIT_TIMEOUT, value)
    }

    fun isJavarosa3Upgraded(): Boolean =
        getBoolean(SharedPrefs.JAVAROSA_3_UPGRADE, false)

    fun setJavarosa3Upgraded(value: Boolean) {
        setBoolean(SharedPrefs.JAVAROSA_3_UPGRADE, value)
    }

    fun setAutoUpload(value: Boolean) {
        setBoolean(SharedPrefs.AUTO_UPLOAD, value)
    }

    fun setAutoUploadPased(value: Boolean) { // keep typo for compatibility
        setBoolean(SharedPrefs.AUTO_UPLOAD_PAUSED, value)
    }

    fun isAutoDeleteEnabled(): Boolean =
        getBoolean(SharedPrefs.AUTO_DELETE, false)

    fun setAutoDelete(value: Boolean) {
        setBoolean(SharedPrefs.AUTO_DELETE, value)
    }

    fun isMetadataAutoUpload(): Boolean =
        getBoolean(SharedPrefs.METADATA_AUTO_UPLOAD, false)

    fun setMetadataAutoUpload(value: Boolean) {
        setBoolean(SharedPrefs.METADATA_AUTO_UPLOAD, value)
    }

    private fun getBoolean(name: String, def: Boolean): Boolean {
        val cached = bCache[name]
        if (cached != null) return cached

        val value = sharedPrefs.getBoolean(name, def)
        bCache[name] = value
        return value
    }

    private fun setBoolean(name: String, value: Boolean) {
        bCache[name] = sharedPrefs.setBoolean(name, value)
    }

    // preserve reference equality check with SharedPrefs.NONE
    private fun getString(name: String, def: String): String? {
        val value = sharedPrefs.getString(name, def)
        return if (value !== SharedPrefs.NONE) value else def
    }

    private fun setString(name: String, value: String?) {
        sharedPrefs.setString(name, value)
    }

    private fun getFloat(name: String, def: Float): Float =
        sharedPrefs.getFloat(name, def)

    private fun setFloat(name: String, value: Float) {
        sharedPrefs.setFloat(name, value)
    }

    private fun getLong(name: String, def: Long): Long =
        sharedPrefs.getLong(name, def)

    private fun setLong(name: String, value: Long) {
        sharedPrefs.setLong(name, value)
    }

    fun isShowVaultImprovementSection(): Boolean =
        getBoolean(SHOW_IMPROVEMENT_SECTION, true)

    fun setShowVaultImprovementSection(value: Boolean) {
        setBoolean(SHOW_IMPROVEMENT_SECTION, value)
    }

    fun isShowUpdateMigrationSheet(): Boolean =
        getBoolean(SharedPrefs.SHOW_UPDATE_MIGRATION_BOTTOM_SHEET, true)

    fun setShowUpdateMigrationSheet(value: Boolean) {
        setBoolean(SharedPrefs.SHOW_UPDATE_MIGRATION_BOTTOM_SHEET, value)
    }

    fun isShowFailedMigrationSheet(): Boolean =
        getBoolean(SharedPrefs.SHOW_MIGRATION_FAILED_BOTTOM_SHEET, true)

    fun setShowFailedMigrationSheet(value: Boolean) {
        setBoolean(SharedPrefs.SHOW_MIGRATION_FAILED_BOTTOM_SHEET, value)
    }

    fun hasAcceptedImprovements(): Boolean =
        getBoolean(HAS_IMPROVEMENT_ACCEPTED, false)

    fun setIsAcceptedImprovements(value: Boolean) {
        setBoolean(HAS_IMPROVEMENT_ACCEPTED, value)
        setTimeAcceptedImprovements(Date().time)
    }

    fun getUnlockTime(): Long =
        getLong(UNLOCK_TIME, 0L)

    fun setUnlockTime(value: Long) {
        setLong(UNLOCK_TIME, value)
    }

    fun getTimeSpent(): Long =
        getLong(TIME_SPENT, 0L)

    fun setTimeSpent(value: Long) {
        setLong(TIME_SPENT, value)
    }

    fun isShowVaultAnalyticsSection(): Boolean =
        getBoolean(SHOW_IMPROVEMENT_SECTION, true)

    fun setShowVaultAnalyticsSection(value: Boolean) {
        setBoolean(SHOW_IMPROVEMENT_SECTION, value)
    }

    fun isInstallMetricSent(): Boolean =
        getBoolean(INSTALL_METRIC_SENT, false)

    fun setInstallMetricSent(value: Boolean) {
        setBoolean(INSTALL_METRIC_SENT, value)
    }

    fun hasAcceptedAnalytics(): Boolean =
        getBoolean(HAS_IMPROVEMENT_ACCEPTED, false)

    fun setIsAcceptedAnalytics(value: Boolean) {
        setBoolean(HAS_IMPROVEMENT_ACCEPTED, value)
        setTimeAcceptedAnalytics(Date().time)
    }

    fun getTimeAcceptedAnalytics(): Long =
        getLong(TIME_IMPROVEMENT_ACCEPTED, 0L)

    private fun setTimeAcceptedAnalytics(value: Long) {
        setLong(TIME_IMPROVEMENT_ACCEPTED, value)
    }

    fun isTimeToShowReminderAnalytics(): Boolean {
        if (getTimeAcceptedAnalytics() == 0L || !hasAcceptedAnalytics()) return false
        val currentDate = Date()
        val acceptedDatePlusSixMonths =
            DateTime(Date(getTimeAcceptedAnalytics())).plusMonths(6).toDate()
        return currentDate.time > acceptedDatePlusSixMonths.time
    }

    fun getTimeAcceptedImprovements(): Long =
        getLong(TIME_IMPROVEMENT_ACCEPTED, 0L)

    private fun setTimeAcceptedImprovements(value: Long) {
        setLong(TIME_IMPROVEMENT_ACCEPTED, value)
    }

    fun isTimeToShowReminderImprovements(): Boolean {
        if (getTimeAcceptedImprovements() == 0L || !hasAcceptedImprovements()) return false
        val currentDate = Date()
        val acceptedDatePlusSixMonths =
            DateTime(Date(getTimeAcceptedImprovements())).plusMonths(6).toDate()
        return currentDate.time > acceptedDatePlusSixMonths.time
    }

    fun isFeedbackSharingEnabled(): Boolean =
        getBoolean(SharedPrefs.FEEDBACK_SHARING_ENBALED, false)

    fun setFeedbackSharingEnabled(value: Boolean) {
        setBoolean(SharedPrefs.FEEDBACK_SHARING_ENBALED, value)
    }

    fun isAlreadyMigratedMainDB(): Boolean =
        getBoolean(IS_MIGRATED_MAIN_DB, false)

    fun setAlreadyMigratedMainDB(value: Boolean) {
        setBoolean(IS_MIGRATED_MAIN_DB, value)
    }

    fun isFreshInstall(): Boolean =
        getBoolean(IS_FRESH_INSTALL, false)

    fun setFreshInstall(value: Boolean) {
        setBoolean(IS_FRESH_INSTALL, value)
    }
}
