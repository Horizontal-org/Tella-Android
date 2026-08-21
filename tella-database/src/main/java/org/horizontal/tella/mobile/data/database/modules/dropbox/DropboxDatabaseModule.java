package org.horizontal.tella.mobile.data.database.modules.dropbox;

import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.cddl;
import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.sq;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.horizontal.tella.mobile.data.database.D;
import org.horizontal.tella.mobile.data.database.modules.DatabaseModule;

/**
 * Database module for Dropbox functionality.
 * Manages tables for:
 * - Dropbox servers
 * - Dropbox form instances
 * - Dropbox instance vault files
 * 
 * This module can be excluded from F-Droid builds.
 */
public class DropboxDatabaseModule implements DatabaseModule {
    private static final int MIN_DB_VERSION = 14;

    @Override
    public String getModuleName() {
        return "Dropbox";
    }

    @Override
    public int getMinDatabaseVersion() {
        return MIN_DB_VERSION;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createTableDropBox());
        db.execSQL(createTableDropBoxFormInstance());
        db.execSQL(createTableDropBoxInstanceVaultFile());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 14) {
            db.execSQL(createTableDropBox());
            db.execSQL(createTableDropBoxFormInstance());
            db.execSQL(createTableDropBoxInstanceVaultFile());
        }
    }

    private String createTableDropBox() {
        return "CREATE TABLE " + sq(D.T_DROPBOX) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_DROPBOX_ACCESS_TOKEN, D.TEXT, true) + " UNIQUE, " +
                cddl(D.C_NAME, D.TEXT) + " , " +
                cddl(D.C_USERNAME, D.TEXT, false) + " , " +
                cddl(D.C_DROPBOX_SERVER_NAME, D.TEXT, true) +
                ");";
    }

    private String createTableDropBoxFormInstance() {
        return "CREATE TABLE " + sq(D.T_DROPBOX_FORM_INSTANCE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_REPORT_SERVER_ID, D.INTEGER, true) + " , " +
                cddl(D.C_REPORT_API_ID, D.TEXT, true) + " , " +
                cddl(D.C_METADATA, D.TEXT, false) + " , " +
                cddl(D.C_CURRENT_UPLOAD, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_UPDATED, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_TITLE, D.TEXT, true) + " , " +
                cddl(D.C_DESCRIPTION_TEXT, D.TEXT, true) + " , " +
                cddl(D.C_FORM_PART_STATUS, D.INTEGER, true) + " DEFAULT 0 , " +
                "FOREIGN KEY(" + sq(D.C_REPORT_SERVER_ID) + ") REFERENCES " +
                sq(D.T_DROPBOX) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE" +
                ");";
    }

    private String createTableDropBoxInstanceVaultFile() {
        return "CREATE TABLE " + sq(D.T_DROPBOX_INSTANCE_VAULT_FILE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_REPORT_INSTANCE_ID, D.INTEGER, true) + " , " +
                cddl(D.C_VAULT_FILE_ID, D.TEXT, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0," +
                cddl(D.C_UPLOADED_SIZE, D.INTEGER, true) + " DEFAULT 0," +
                "FOREIGN KEY(" + sq(D.C_REPORT_INSTANCE_ID) + ") REFERENCES " +
                sq(D.T_DROPBOX_FORM_INSTANCE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "UNIQUE(" + sq(D.C_REPORT_INSTANCE_ID) + ", " + sq(D.C_VAULT_FILE_ID) + ") ON CONFLICT IGNORE" +
                ");";
    }
}




