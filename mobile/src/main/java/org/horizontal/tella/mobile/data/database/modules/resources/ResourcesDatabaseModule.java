package org.horizontal.tella.mobile.data.database.modules.resources;

import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.cddl;
import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.sq;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.horizontal.tella.mobile.data.database.D;
import org.horizontal.tella.mobile.data.database.modules.DatabaseModule;


/**
 * Database module for Resources functionality.
 * Manages tables for:
 * - Resources
 */
public class ResourcesDatabaseModule implements DatabaseModule {
    private static final int MIN_DB_VERSION = 12;

    @Override
    public String getModuleName() {
        return "Resources";
    }

    @Override
    public int getMinDatabaseVersion() {
        return MIN_DB_VERSION;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // DBv12
        db.execSQL(createTableResources());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 12) {
            db.execSQL(createTableResources());
        }
    }

    private String createTableResources() {
        return "CREATE TABLE " + sq(D.T_RESOURCES) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_SERVER_ID, D.INTEGER) + " , " +
                cddl(D.C_RESOURCES_ID, D.TEXT, false) + " UNIQUE, " +
                cddl(D.C_RESOURCES_TITLE, D.TEXT, false) + " , " +
                cddl(D.C_RESOURCES_FILE_NAME, D.TEXT, true) + " UNIQUE, " +
                cddl(D.C_RESOURCES_SIZE, D.INTEGER) + " , " +
                cddl(D.C_RESOURCES_CREATED, D.TEXT, false) + " , " +
                cddl(D.C_RESOURCES_SAVED, D.INTEGER, false) + " , " +
                cddl(D.C_RESOURCES_PROJECT, D.TEXT, false) + " , " +
                cddl(D.C_RESOURCES_FILE_ID, D.TEXT, false) +
                ");";
    }
}

