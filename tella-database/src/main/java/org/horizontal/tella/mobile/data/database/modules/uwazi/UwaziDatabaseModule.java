package org.horizontal.tella.mobile.data.database.modules.uwazi;

import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.cddl;
import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.sq;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.horizontal.tella.mobile.data.database.D;
import org.horizontal.tella.mobile.data.database.modules.DatabaseModule;

/**
 * Database module for Uwazi functionality.
 * Manages tables for:
 * - Uwazi servers
 * - Uwazi blank templates
 * - Uwazi entity instances
 * - Uwazi entity instance vault files
 */
public class UwaziDatabaseModule implements DatabaseModule {
    private static final int MIN_DB_VERSION = 8;

    @Override
    public String getModuleName() {
        return "Uwazi";
    }

    @Override
    public int getMinDatabaseVersion() {
        return MIN_DB_VERSION;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // DBv8
        db.execSQL(createTableUwaziServer());
        db.execSQL(createTableCollectEntityUwazi());
        db.execSQL(createTableCollectBlankTemplateUwazi());
        db.execSQL(createTableUwaziEntityInstanceVaultFile());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 8) {
            db.execSQL(createTableUwaziServer());
            db.execSQL(createTableCollectEntityUwazi());
            db.execSQL(createTableCollectBlankTemplateUwazi());
            db.execSQL(createTableUwaziEntityInstanceVaultFile());
        }
    }

    private String createTableUwaziServer() {
        return "CREATE TABLE " + sq(D.T_UWAZI_SERVER) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_NAME, D.TEXT) + " , " +
                cddl(D.C_URL, D.TEXT) + " , " +
                cddl(D.C_CONNECT_COOKIES, D.TEXT) + " , " +
                cddl(D.C_LOCALE_COOKIES, D.TEXT) + " , " +
                cddl(D.C_USERNAME, D.TEXT) + " , " +
                cddl(D.C_PASSWORD, D.TEXT) +
                ");";
    }

    private String createTableCollectBlankTemplateUwazi() {
        return "CREATE TABLE " + sq(D.T_UWAZI_BLANK_TEMPLATES) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_UWAZI_SERVER_ID, D.INTEGER, true) + " , " +
                cddl(D.C_DOWNLOAD_URL, D.TEXT) + " , " +
                cddl(D.C_TEMPLATE_ENTITY, D.TEXT, true) + " , " +
                cddl(D.C_DOWNLOADED, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_FAVORITE, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_UPDATED, D.INTEGER, true) + " DEFAULT 0 , " +
                "FOREIGN KEY(" + sq(D.C_UWAZI_SERVER_ID) + ") REFERENCES " +
                sq(D.T_UWAZI_SERVER) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE, " +
                "UNIQUE(" + sq(D.C_ID) + ") ON CONFLICT REPLACE" +
                ");";
    }

    private String createTableCollectEntityUwazi() {
        return "CREATE TABLE " + sq(D.T_UWAZI_ENTITY_INSTANCES) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_UWAZI_SERVER_ID, D.INTEGER, true) + " , " +
                cddl(D.C_TEMPLATE_ENTITY, D.TEXT, true) + " , " +
                cddl(D.C_METADATA, D.TEXT, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_UPDATED, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_TEMPLATE, D.TEXT, true) + " , " +
                cddl(D.C_TITLE, D.TEXT, true) + " , " +
                cddl(D.C_TYPE, D.TEXT, true) + " , " +
                cddl(D.C_FORM_PART_STATUS, D.INTEGER, true) + " DEFAULT 0 , " +
                "FOREIGN KEY(" + sq(D.C_UWAZI_SERVER_ID) + ") REFERENCES " +
                sq(D.T_UWAZI_SERVER) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE, " +
                "UNIQUE(" + sq(D.C_ID) + ") ON CONFLICT REPLACE" +
                ");";
    }

    private String createTableUwaziEntityInstanceVaultFile() {
        return "CREATE TABLE " + sq(D.T_UWAZI_ENTITY_INSTANCE_VAULT_FILE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_UWAZI_ENTITY_INSTANCE_ID, D.INTEGER, true) + " , " +
                cddl(D.C_VAULT_FILE_ID, D.TEXT, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0," +
                "FOREIGN KEY(" + sq(D.C_UWAZI_ENTITY_INSTANCE_ID) + ") REFERENCES " +
                sq(D.T_UWAZI_ENTITY_INSTANCES) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "UNIQUE(" + sq(D.C_UWAZI_ENTITY_INSTANCE_ID) + ", " + sq(D.C_VAULT_FILE_ID) + ") ON CONFLICT IGNORE" +
                ");";
    }
}

