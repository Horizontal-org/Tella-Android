package org.horizontal.tella.mobile.data.database.modules.nextcloud;

import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.cddl;
import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.sq;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.horizontal.tella.mobile.data.database.D;
import org.horizontal.tella.mobile.data.database.modules.DatabaseModule;

/**
 * Database module for NextCloud functionality.
 * Manages tables for:
 * - NextCloud servers
 * - NextCloud form instances
 * - NextCloud instance vault files
 * 
 * Note: NextCloud is F-Droid compatible and should always be included.
 */
public class NextCloudDatabaseModule implements DatabaseModule {
    private static final int MIN_DB_VERSION = 15;

    @Override
    public String getModuleName() {
        return "NextCloud";
    }

    @Override
    public int getMinDatabaseVersion() {
        return MIN_DB_VERSION;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createTableNextCloud());
        db.execSQL(createTableNextCloudFormInstance());
        db.execSQL(createTableNextCloudInstanceVaultFile());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 15) {
            db.execSQL(createTableNextCloud());
            db.execSQL(createTableNextCloudFormInstance());
            db.execSQL(createTableNextCloudInstanceVaultFile());
        }
    }

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




