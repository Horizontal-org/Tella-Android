package org.horizontal.tella.mobile.data.database.modules.cloud;

import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.cddl;
import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.sq;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.horizontal.tella.mobile.data.database.D;
import org.horizontal.tella.mobile.data.database.modules.DatabaseModule;

/**
 * Database module for Cloud Services functionality.
 * Manages tables for:
 * - Google Drive
 * - Dropbox
 * - NextCloud
 * - Their respective form instances and vault files
 */
public class CloudDatabaseModule implements DatabaseModule {
    private static final int MIN_DB_VERSION = 13;

    @Override
    public String getModuleName() {
        return "Cloud";
    }

    @Override
    public int getMinDatabaseVersion() {
        return MIN_DB_VERSION;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // DBv13 - Google Drive
        db.execSQL(createTableGoogleDrive());
        db.execSQL(createTableGoogleDriveFormInstance());
        db.execSQL(createTableGoogleDriveInstanceVaultFile());

        // DBv14 - Dropbox
        db.execSQL(createTableDropBox());
        db.execSQL(createTableDropBoxFormInstance());
        db.execSQL(createTableDropBoxInstanceVaultFile());

        // DBv15 - NextCloud
        db.execSQL(createTableNextCloud());
        db.execSQL(createTableNextCloudFormInstance());
        db.execSQL(createTableNextCloudInstanceVaultFile());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 13) {
            db.execSQL(createTableGoogleDrive());
            db.execSQL(createTableGoogleDriveFormInstance());
            db.execSQL(createTableGoogleDriveInstanceVaultFile());
        }
        if (oldVersion < 14) {
            db.execSQL(createTableDropBox());
            db.execSQL(createTableDropBoxFormInstance());
            db.execSQL(createTableDropBoxInstanceVaultFile());
        }
        if (oldVersion < 15) {
            db.execSQL(createTableNextCloud());
            db.execSQL(createTableNextCloudFormInstance());
            db.execSQL(createTableNextCloudInstanceVaultFile());
        }
    }

    // Google Drive
    private String createTableGoogleDrive() {
        return "CREATE TABLE " + sq(D.T_GOOGLE_DRIVE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_GOOGLE_DRIVE_FOLDER_ID, D.TEXT, true) + " UNIQUE, " +
                cddl(D.C_GOOGLE_DRIVE_FOLDER_NAME, D.TEXT, true) + " , " +
                cddl(D.C_NAME, D.TEXT) + " , " +
                cddl(D.C_USERNAME, D.TEXT, true) + " , " +
                cddl(D.C_GOOGLE_DRIVE_SERVER_NAME, D.TEXT, true) +
                ");";
    }

    private String createTableGoogleDriveFormInstance() {
        return "CREATE TABLE " + sq(D.T_GOOGLE_DRIVE_FORM_INSTANCE) + " (" +
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
                sq(D.T_GOOGLE_DRIVE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE" +
                ");";
    }

    private String createTableGoogleDriveInstanceVaultFile() {
        return "CREATE TABLE " + sq(D.T_GOOGLE_DRIVE_INSTANCE_VAULT_FILE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_REPORT_INSTANCE_ID, D.INTEGER, true) + " , " +
                cddl(D.C_VAULT_FILE_ID, D.TEXT, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0," +
                cddl(D.C_UPLOADED_SIZE, D.INTEGER, true) + " DEFAULT 0," +
                "FOREIGN KEY(" + sq(D.C_REPORT_INSTANCE_ID) + ") REFERENCES " +
                sq(D.T_GOOGLE_DRIVE_FORM_INSTANCE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "UNIQUE(" + sq(D.C_REPORT_INSTANCE_ID) + ", " + sq(D.C_VAULT_FILE_ID) + ") ON CONFLICT IGNORE" +
                ");";
    }

    // Dropbox
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

    // NextCloud
    private String createTableNextCloud() {
        return "CREATE TABLE " + sq(D.T_NEXT_CLOUD) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_NEXT_CLOUD_FOLDER_NAME, D.TEXT, true) + " UNIQUE, " +
                cddl(D.C_URL, D.TEXT) + " , " +
                cddl(D.C_NEXT_CLOUD_USER_ID, D.TEXT, true) + " , " +
                cddl(D.C_PASSWORD, D.TEXT, true) + " , " +
                cddl(D.C_NAME, D.TEXT) + " , " +
                cddl(D.C_USERNAME, D.TEXT, true) + " , " +
                cddl(D.C_NEXT_CLOUD_SERVER_NAME, D.TEXT, true) +
                ");";
    }

    private String createTableNextCloudFormInstance() {
        return "CREATE TABLE " + sq(D.T_NEXT_CLOUD_FORM_INSTANCE) + " (" +
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
                sq(D.T_NEXT_CLOUD) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE" +
                ");";
    }

    private String createTableNextCloudInstanceVaultFile() {
        return "CREATE TABLE " + sq(D.T_NEXT_CLOUD_INSTANCE_VAULT_FILE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_REPORT_INSTANCE_ID, D.INTEGER, true) + " , " +
                cddl(D.C_VAULT_FILE_ID, D.TEXT, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0," +
                cddl(D.C_UPLOADED_SIZE, D.INTEGER, true) + " DEFAULT 0," +
                "FOREIGN KEY(" + sq(D.C_REPORT_INSTANCE_ID) + ") REFERENCES " +
                sq(D.T_NEXT_CLOUD_FORM_INSTANCE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "UNIQUE(" + sq(D.C_REPORT_INSTANCE_ID) + ", " + sq(D.C_VAULT_FILE_ID) + ") ON CONFLICT IGNORE" +
                ");";
    }
}

