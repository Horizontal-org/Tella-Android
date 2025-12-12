package org.horizontal.tella.mobile.data.database;

import android.content.Context;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.horizontal.tella.mobile.data.database.modules.DatabaseModule;
import org.horizontal.tella.mobile.data.database.modules.cloud.CloudDatabaseModule;
import org.horizontal.tella.mobile.data.database.modules.feedback.FeedbackDatabaseModule;
import org.horizontal.tella.mobile.data.database.modules.forms.FormsDatabaseModule;
import org.horizontal.tella.mobile.data.database.modules.media.MediaDatabaseModule;
import org.horizontal.tella.mobile.data.database.modules.reports.ReportsDatabaseModule;
import org.horizontal.tella.mobile.data.database.modules.resources.ResourcesDatabaseModule;
import org.horizontal.tella.mobile.data.database.modules.settings.SettingsDatabaseModule;
import org.horizontal.tella.mobile.data.database.modules.uwazi.UwaziDatabaseModule;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class HorizontalSQLiteOpenHelper extends CipherOpenHelper {
    private static final String OBJ_QUOTE = "`";
    
    private final List<DatabaseModule> databaseModules;

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);

        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    HorizontalSQLiteOpenHelper(Context context, byte[] password, DatabasePreferences preferences, List<DatabaseModule> modules) {
        super(context, password, preferences);
        if (!preferences.isAlreadyMigratedMainDB()) {
            migrateSqlCipher3To4IfNeeded(context, password, preferences);
        }
        
        // Use provided modules or default modules
        this.databaseModules = modules != null ? new ArrayList<>(modules) : getDefaultModules();
    }
    
    /**
     * Factory method to create with default modules.
     */
    public static HorizontalSQLiteOpenHelper create(Context context, byte[] password, DatabasePreferences preferences) {
        return new HorizontalSQLiteOpenHelper(context, password, preferences, null);
    }
    
    /**
     * Factory method to create with custom modules.
     */
    public static HorizontalSQLiteOpenHelper create(Context context, byte[] password, DatabasePreferences preferences, List<DatabaseModule> modules) {
        return new HorizontalSQLiteOpenHelper(context, password, preferences, modules);
    }
    
    private List<DatabaseModule> getDefaultModules() {
        List<DatabaseModule> modules = new ArrayList<>();
        modules.add(new SettingsDatabaseModule());
        modules.add(new FormsDatabaseModule());
        modules.add(new MediaDatabaseModule());
        modules.add(new ReportsDatabaseModule());
        modules.add(new UwaziDatabaseModule());
        modules.add(new FeedbackDatabaseModule());
        modules.add(new ResourcesDatabaseModule());
        modules.add(new CloudDatabaseModule());
        return modules;
    }

    public static String objQuote(String str) {
        return OBJ_QUOTE + str + OBJ_QUOTE;
    }

    public static String sq(String unQuotedText) {
        return " " + objQuote(unQuotedText) + " ";
    }

    public static String cddl(String columnName, String columnType) {
        return objQuote(columnName) + " " + columnType;
    }

    public static String cddl(String columnName, String columnType, boolean notNull) {
        return objQuote(columnName) + " " + columnType + (notNull ? " NOT NULL" : "");
    }


    @Override
    public void onCreate(net.zetetic.database.sqlcipher.SQLiteDatabase db) {
        Timber.d("Creating database with modular architecture");
        
        // Delegate table creation to modules
        for (DatabaseModule module : databaseModules) {
            try {
                Timber.d("Creating tables for module: %s", module.getModuleName());
                module.onCreate(db);
            } catch (Exception e) {
                Timber.e(e, "Error creating tables for module: %s", module.getModuleName());
                throw new RuntimeException("Failed to create database module: " + module.getModuleName(), e);
            }
        }
        
        Timber.d("Database creation completed");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Timber.d("Upgrading database from version %d to %d", oldVersion, newVersion);
        
        // Delegate upgrade to modules
        for (DatabaseModule module : databaseModules) {
            try {
                if (oldVersion < module.getMinDatabaseVersion() || 
                    oldVersion < newVersion) {
                    Timber.d("Upgrading module: %s (min version: %d)", 
                            module.getModuleName(), module.getMinDatabaseVersion());
                    module.onUpgrade(db, oldVersion, newVersion);
                }
            } catch (Exception e) {
                Timber.e(e, "Error upgrading module: %s", module.getModuleName());
                throw new RuntimeException("Failed to upgrade database module: " + module.getModuleName(), e);
            }
        }
        
        Timber.d("Database upgrade completed");
    }

}
