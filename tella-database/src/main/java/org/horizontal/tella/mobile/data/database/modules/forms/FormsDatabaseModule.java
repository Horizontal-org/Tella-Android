package org.horizontal.tella.mobile.data.database.modules.forms;

import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.cddl;
import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.sq;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.horizontal.tella.mobile.data.database.D;
import org.horizontal.tella.mobile.data.database.modules.DatabaseModule;

/**
 * Database module for Collect Forms functionality.
 * Manages tables for:
 * - Collect servers
 * - Blank forms
 * - Form instances
 * - Form instance media files
 * - Form instance vault files
 */
public class FormsDatabaseModule implements DatabaseModule {
    private static final int MIN_DB_VERSION = 1;

    @Override
    public String getModuleName() {
        return "Forms";
    }

    @Override
    public int getMinDatabaseVersion() {
        return MIN_DB_VERSION;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // DBv1
        db.execSQL(createTableCollectServer());
        db.execSQL(createTableCollectBlankForm());
        db.execSQL(createTableCollectFormInstance());
        db.execSQL(createTableCollectFormInstanceMediaFile());
        db.execSQL(createTableCollectFormInstanceVaultFile());

        // DBv2
        db.execSQL(alterTableCollectFormInstanceMediaFileAddStatus());
        db.execSQL(alterTableCollectServerAddChecked());

        // DBv3
        db.execSQL(alterTableCollectBlankFormAddUpdated());

        // DBv4
        db.execSQL(alterTableCollectFormInstanceAddFormPartStatus());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(alterTableCollectFormInstanceMediaFileAddStatus());
            db.execSQL(alterTableCollectServerAddChecked());
        }
        if (oldVersion < 3) {
            db.execSQL(alterTableCollectBlankFormAddUpdated());
        }
        if (oldVersion < 4) {
            db.execSQL(alterTableCollectFormInstanceAddFormPartStatus());
        }
    }

    private String createTableCollectServer() {
        return "CREATE TABLE " + sq(D.T_COLLECT_SERVER) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_NAME, D.TEXT) + " , " +
                cddl(D.C_URL, D.TEXT) + " , " +
                cddl(D.C_USERNAME, D.TEXT) + " , " +
                cddl(D.C_PASSWORD, D.TEXT) +
                ");";
    }

    private String createTableCollectBlankForm() {
        return "CREATE TABLE " + sq(D.T_COLLECT_BLANK_FORM) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_COLLECT_SERVER_ID, D.INTEGER, true) + " , " +
                cddl(D.C_FORM_ID, D.TEXT, true) + " , " +
                cddl(D.C_VERSION, D.TEXT, true) + " , " +
                cddl(D.C_HASH, D.TEXT, true) + " , " +
                cddl(D.C_NAME, D.TEXT, true) + " , " +
                cddl(D.C_DOWNLOAD_URL, D.TEXT) + " , " +
                cddl(D.C_FORM_DEF, D.BLOB) + " , " +
                cddl(D.C_DOWNLOADED, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_FAVORITE, D.INTEGER, true) + " DEFAULT 0 , " +
                "FOREIGN KEY(" + sq(D.C_COLLECT_SERVER_ID) + ") REFERENCES " +
                sq(D.T_COLLECT_SERVER) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE, " +
                "UNIQUE(" + sq(D.C_FORM_ID) + ") ON CONFLICT REPLACE" +
                ");";
    }

    private String createTableCollectFormInstance() {
        return "CREATE TABLE " + sq(D.T_COLLECT_FORM_INSTANCE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_COLLECT_SERVER_ID, D.INTEGER, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " , " +
                cddl(D.C_UPDATED, D.INTEGER, true) + " , " +
                cddl(D.C_FORM_ID, D.TEXT, true) + " , " +
                cddl(D.C_VERSION, D.TEXT, true) + " , " +
                cddl(D.C_FORM_NAME, D.TEXT, true) + " , " +
                cddl(D.C_INSTANCE_NAME, D.TEXT, true) + " , " +
                cddl(D.C_FORM_DEF, D.BLOB) + " , " +
                "FOREIGN KEY(" + sq(D.C_COLLECT_SERVER_ID) + ") REFERENCES " +
                sq(D.T_COLLECT_SERVER) + "(" + sq(D.C_ID) + ") ON DELETE RESTRICT" +
                ");";
    }

    private String createTableCollectFormInstanceMediaFile() {
        return "CREATE TABLE " + sq(D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_COLLECT_FORM_INSTANCE_ID, D.INTEGER, true) + " , " +
                cddl(D.C_MEDIA_FILE_ID, D.INTEGER, true) + " , " +
                "FOREIGN KEY(" + sq(D.C_COLLECT_FORM_INSTANCE_ID) + ") REFERENCES " +
                sq(D.T_COLLECT_FORM_INSTANCE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "FOREIGN KEY(" + sq(D.C_MEDIA_FILE_ID) + ") REFERENCES " +
                sq(D.T_MEDIA_FILE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "UNIQUE(" + sq(D.C_COLLECT_FORM_INSTANCE_ID) + ", " + sq(D.C_MEDIA_FILE_ID) + ") ON CONFLICT IGNORE" +
                ");";
    }

    private String createTableCollectFormInstanceVaultFile() {
        return "CREATE TABLE " + sq(D.T_COLLECT_FORM_INSTANCE_VAULT_FILE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_COLLECT_FORM_INSTANCE_ID, D.INTEGER, true) + " , " +
                cddl(D.C_VAULT_FILE_ID, D.TEXT, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0," +
                "FOREIGN KEY(" + sq(D.C_COLLECT_FORM_INSTANCE_ID) + ") REFERENCES " +
                sq(D.T_COLLECT_FORM_INSTANCE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "UNIQUE(" + sq(D.C_COLLECT_FORM_INSTANCE_ID) + ", " + sq(D.C_VAULT_FILE_ID) + ") ON CONFLICT IGNORE" +
                ");";
    }

    private String alterTableCollectFormInstanceMediaFileAddStatus() {
        return "ALTER TABLE " + sq(D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE) + " ADD COLUMN " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0";
    }

    private String alterTableCollectServerAddChecked() {
        return "ALTER TABLE " + sq(D.T_COLLECT_SERVER) + " ADD COLUMN " +
                cddl(D.C_CHECKED, D.INTEGER, true) + " DEFAULT 0";
    }

    private String alterTableCollectBlankFormAddUpdated() {
        return "ALTER TABLE " + sq(D.T_COLLECT_BLANK_FORM) + " ADD COLUMN " +
                cddl(D.C_UPDATED, D.INTEGER, true) + " DEFAULT 0";
    }

    private String alterTableCollectFormInstanceAddFormPartStatus() {
        return "ALTER TABLE " + sq(D.T_COLLECT_FORM_INSTANCE) + " ADD COLUMN " +
                cddl(D.C_FORM_PART_STATUS, D.INTEGER, true) + " DEFAULT 0";
    }
}

