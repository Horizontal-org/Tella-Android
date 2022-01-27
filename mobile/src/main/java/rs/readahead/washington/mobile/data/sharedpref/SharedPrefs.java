package rs.readahead.washington.mobile.data.sharedpref;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;


public class SharedPrefs {
    public static final String NONE = "";

    private static final String SHARED_PREFS_NAME = "washington_shared_prefs";

    static final String SECRET_PASSWORD = "secret_password";
    //private static final String TOR_MODE_ENABLED = "tor_password_enabled";
    //private static final String ASK_FOR_TOR = "ask_for_tor";
    private static final String PANIC_PASSWORD = "panic_password";
    static final String PANIC_MESSAGE = "panic_message";
    static final String PANIC_GEOLOCATION = "panic_geolocation";
    static final String DELETE_SERVER_SETTINGS = "erase_everything";
    static final String ERASE_GALLERY = "erase_gallery";
    static final String ERASE_FORMS = "erase_forms";
    //private static final String AUTO_SAVE_DRAFT_FORM = "auto_save_draft_form";
    private static final String LANGUAGE = "language";
    static final String SECRET_MODE_ENABLED = "secret_password_enabled";
    static final String BYPASS_CENSORSHIP = "bypass_censorship";
    static final String ANONYMOUS_MODE = "anonymous_mode";
    static final String UNINSTALL_ON_PANIC = "uninstall_on_panic";
    static final String APP_FIRST_START = "app_first_start";
    static final String APP_ALIAS_NAME = "app_alias_name";
    static final String SUBMIT_CRASH_REPORTS = "submit_crash_reports";
    static final String ENABLE_CAMERA_PREVIEW = "enable_camera_preview";
    static final String LOCATION_ACCURACY_THRESHOLD = "location_threshold";
    static final String OFFLINE_MODE = "offline_mode";
    static final String QUICK_EXIT_BUTTON = "quick_exit_button";
    static final String COLLECT_OPTION = "collect_option";
    static final String INSTALLATION_ID = "installation_id";
    static final String LAST_COLLECT_REFRESH = "last_collect_refresh";
    static final String VIDEO_RESOLUTION = "video_resolution";
    static final String AUTO_UPLOAD_SERVER = "auto_upload_server";
    static final String AUTO_UPLOAD = "auto_upload";
    static final String AUTO_DELETE = "auto_delete";
    static final String METADATA_AUTO_UPLOAD = "metadata_auto_upload";
    static final String AUTO_UPLOAD_PAUSED = "auto_upload_paused";
    static final String LOCK_TIMEOUT = "lock_timeout";
    static final String MUTE_CAMERA_SHUTTER = "mute_camera_shutter";
    static final String SHOW_FAVORITE_FORMS = "show_favorite_forms";
    static final String SHOW_FAVORITE_TEMPLATES = "show_favorite_Templates";
    static final String SHOW_RECENT_FILES = "show_recent_files";
    static final String UPGRADE_TELLA_2 = "update_tella_2";

    private static SharedPrefs instance;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;


    public static SharedPrefs getInstance() {
        synchronized (SharedPrefs.class) {
            if (instance == null) {
                instance = new SharedPrefs();
            }

            return instance;
        }
    }

    public SharedPreferences getPref() {
        return pref;
    }

    @SuppressLint("CommitPrefEdits")
    public void init(Context context) {
        pref = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void setPanicPassword(String password) {
        editor.putString(PANIC_PASSWORD, password);
        editor.apply();
    }

    public String getPanicPassword() {
        return pref.getString(PANIC_PASSWORD, "");
    }

    /*public boolean isTorModeActive() {
        return pref.getBoolean(TOR_MODE_ENABLED, false);
    }

    public void setToreModeActive(boolean activated) {
        editor.putBoolean(TOR_MODE_ENABLED, activated);
        editor.apply();
    }*/

    /*public boolean askForTorOnStart() {
        return pref.getBoolean(ASK_FOR_TOR, true);
    }

    public void setAskForTorOnStart(boolean activated) {
        editor.putBoolean(ASK_FOR_TOR, activated);
        editor.apply();
    }*/

    public boolean isEraseGalleryActive() {
        return pref.getBoolean(ERASE_GALLERY, true);
    }

    public void setEraseGalleryActive(boolean activated) {
        editor.putBoolean(ERASE_GALLERY, activated);
        editor.apply();
    }

    /*public boolean isAutoSaveDrafts() {
        //return pref.getBoolean(AUTO_SAVE_DRAFT_FORM, false);
        return true; // for now this is default, no settings..
    }

    public void setAutoSaveDrafts(boolean autoSaveDrafts) {
        //editor.putBoolean(AUTO_SAVE_DRAFT_FORM, autoSaveDrafts);
        //editor.apply();
    }*/

    public void setAppLanguage(String language) {
        editor.putString(LANGUAGE, language);
        editor.apply();
    }

    public String getAppLanguage() {
        return pref.getString(LANGUAGE, null);
    }

    boolean getBoolean(final String name, final boolean def) {
        return Single.fromCallable(() -> pref.getBoolean(name, def))
                .subscribeOn(Schedulers.io()).blockingGet();
    }

    boolean setBoolean(final String name, final boolean value) {
        return Single.fromCallable(() -> {
            editor.putBoolean(name, value);
            editor.apply();
            return value;
        }).subscribeOn(Schedulers.io()).blockingGet();
    }

    @NonNull
    String getString(@NonNull final String name, final String def) {
        return Single.fromCallable(() -> {
            String str = pref.getString(name, def);
            return str != null ? str : NONE;
        }).subscribeOn(Schedulers.io()).blockingGet();
    }

    void setString(@NonNull final String name, final String value) {
        Completable.fromCallable((Callable<Void>) () -> {
            editor.putString(name, value);
            editor.apply();
            return null;
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    float getFloat(final String name, final float def) {
        return Single.fromCallable(() -> pref.getFloat(name, def))
                .subscribeOn(Schedulers.io()).blockingGet();
    }

    float setFloat(final String name, final float value) {
        return Single.fromCallable(() -> {
            editor.putFloat(name, value);
            editor.apply();
            return value;
        }).subscribeOn(Schedulers.io()).blockingGet();
    }

    long getLong(final String name, final long def) {
        return Single.fromCallable(() -> pref.getLong(name, def))
                .subscribeOn(Schedulers.io()).blockingGet();
    }

    long setLong(final String name, final long value) {
        return Single.fromCallable(() -> {
            editor.putLong(name, value);
            editor.apply();
            return value;
        }).subscribeOn(Schedulers.io()).blockingGet();
    }

    private SharedPrefs() {
    }
}
