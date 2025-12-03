package org.horizontal.tella.core_preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Callable

object SharedPrefs {
    const val NONE = ""
    const val FEEDBACK_SHARING_ENBALED = "feedback_sharing_enabled"
    private const val SHARED_PREFS_NAME = "washington_shared_prefs"

    const val SECRET_PASSWORD = "secret_password"
    private const val PANIC_PASSWORD = "panic_password"
    const val PANIC_MESSAGE = "panic_message"
    const val PANIC_GEOLOCATION = "panic_geolocation"
    const val DELETE_SERVER_SETTINGS = "erase_everything"
    const val ERASE_GALLERY = "erase_gallery"
    const val FAILED_UNLOCK_OPTION = "failed_unlock_option"

    const val SHOW_REMAINING_UNLOCK_ATTEMPTS = "show_remaining_unlock_attempts"
    const val REMAINING_UNLOCK_ATTEMPTS = "remaining_unlock_attempts"
    const val ERASE_FORMS = "erase_forms"
    private const val LANGUAGE = "language"
    const val SECRET_MODE_ENABLED = "secret_password_enabled"
    const val BYPASS_CENSORSHIP = "bypass_censorship"
    const val ANONYMOUS_MODE = "anonymous_mode"
    const val UNINSTALL_ON_PANIC = "uninstall_on_panic"
    const val APP_FIRST_START = "app_first_start"
    const val APP_ALIAS_NAME = "app_alias_name"
    const val CALCULATOR_THEME = "app_calculator_theme"
    const val SUBMIT_CRASH_REPORTS = "submit_crash_reports"
    const val ENABLE_CAMERA_PREVIEW = "enable_camera_preview"
    const val LOCATION_ACCURACY_THRESHOLD = "location_threshold"
    const val OFFLINE_MODE = "offline_mode"
    const val QUICK_EXIT_BUTTON = "quick_exit_button"
    const val COLLECT_OPTION = "collect_option"
    const val INSTALLATION_ID = "installation_id"
    const val LAST_COLLECT_REFRESH = "last_collect_refresh"
    const val VIDEO_RESOLUTION = "video_resolution"
    const val AUTO_UPLOAD_SERVER = "auto_upload_server"
    const val AUTO_UPLOAD = "auto_upload"
    const val AUTO_DELETE = "auto_delete"
    const val METADATA_AUTO_UPLOAD = "metadata_auto_upload"
    const val AUTO_UPLOAD_PAUSED = "auto_upload_paused"
    const val LOCK_TIMEOUT = "lock_timeout"
    const val MUTE_CAMERA_SHUTTER = "mute_camera_shutter"
    const val KEEP_EXIF = "keep_exif"
    const val SET_SECURITY_SCREEN = "set_security_screen"
    const val SHOW_FAVORITE_FORMS = "show_favorite_forms"
    const val SHOW_FAVORITE_TEMPLATES = "show_favorite_Templates"
    const val SHOW_RECENT_FILES = "show_recent_files"
    const val UPGRADE_TELLA_2 = "update_tella_2"
    const val SHOW_IMPROVEMENT_SECTION = "show_improvement_section"
    const val HAS_IMPROVEMENT_ACCEPTED = "has_improvement_accepted"
    const val TIME_IMPROVEMENT_ACCEPTED = "time_improvement_accepted"
    const val TEMP_TIMEOUT = "temp_timeout"
    const val EXIT_TIMEOUT = "exit_timeout"
    const val JAVAROSA_3_UPGRADE = "javarosa_3_upgrade"
    const val TEXT_JUSTIFICATION = "text_justification"
    const val TEXT_SPACING = "text_spacing"
    const val SHOW_UPDATE_MIGRATION_BOTTOM_SHEET = "SHOW_UPDATE_MIGRATION_BOTTOM_SHEET"
    const val SHOW_MIGRATION_FAILED_BOTTOM_SHEET = "show_migration_failed_bottom_sheet"
    const val IS_MIGRATED_MAIN_DB = "is_migrated_main_db"
    const val IS_FRESH_INSTALL = "is_fresh_install"
    const val INSTALL_METRIC_SENT = "install_metric_sent"
    const val UNLOCK_TIME = "unlock_time"
    const val TIME_SPENT = "time_spent"

    // For Java code that still calls SharedPrefs.getInstance()
    @JvmStatic
    fun getInstance(): SharedPrefs = this

    private lateinit var pref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    fun getPref(): SharedPreferences = pref

    @SuppressLint("CommitPrefEdits")
    fun init(context: Context) {
        pref = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        editor = pref.edit()
    }

    fun setPanicPassword(password: String) {
        editor.putString(PANIC_PASSWORD, password)
        editor.apply()
    }

    fun getPanicPassword(): String =
        pref.getString(PANIC_PASSWORD, "") ?: ""

    fun isEraseGalleryActive(): Boolean =
        pref.getBoolean(ERASE_GALLERY, false)

    fun setEraseGalleryActive(activated: Boolean) {
        editor.putBoolean(ERASE_GALLERY, activated)
        editor.apply()
    }

    fun setAppLanguage(language: String) {
        editor.putString(LANGUAGE, language)
        editor.apply()
    }

    fun getAppLanguage(): String? =
        pref.getString(LANGUAGE, null)

    // --- generic getters / setters used by Preferences ---

    internal fun getBoolean(name: String, def: Boolean): Boolean =
        Single.fromCallable { pref.getBoolean(name, def) }
            .subscribeOn(Schedulers.io())
            .blockingGet()

    internal fun setBoolean(name: String, value: Boolean): Boolean =
        Single.fromCallable {
            editor.putBoolean(name, value)
            editor.apply()
            value
        }.subscribeOn(Schedulers.io())
            .blockingGet()

    @JvmStatic
    internal fun getString(name: String, def: String): String =
        Single.fromCallable<String> {
            val str = pref.getString(name, def)
            str ?: NONE
        }.subscribeOn(Schedulers.io())
            .blockingGet()

    internal fun setString(name: String, value: String?) {
        Completable.fromCallable(Callable<Void> {
            editor.putString(name, value)
            editor.apply()
            null
        }).subscribeOn(Schedulers.io())
            .subscribe()
    }

    internal fun getFloat(name: String, def: Float): Float =
        Single.fromCallable { pref.getFloat(name, def) }
            .subscribeOn(Schedulers.io())
            .blockingGet()

    internal fun setFloat(name: String, value: Float): Float =
        Single.fromCallable {
            editor.putFloat(name, value)
            editor.apply()
            value
        }.subscribeOn(Schedulers.io())
            .blockingGet()

    internal fun getLong(name: String, def: Long): Long =
        Single.fromCallable { pref.getLong(name, def) }
            .subscribeOn(Schedulers.io())
            .blockingGet()

    internal fun setLong(name: String, value: Long): Long =
        Single.fromCallable {
            editor.putLong(name, value)
            editor.apply()
            value
        }.subscribeOn(Schedulers.io())
            .blockingGet()
}
