package org.horizontal.tella.mobile.data.database.modules;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

/**
 * Interface for database modules that define their own tables and schema.
 * Each module is responsible for creating and upgrading its own tables.
 */
public interface DatabaseModule {
    /**
     * Gets the module name for logging and identification.
     */
    String getModuleName();

    /**
     * Creates all tables for this module.
     * Called during database creation.
     */
    void onCreate(SQLiteDatabase db);

    /**
     * Upgrades tables for this module.
     * Called during database upgrade.
     *
     * @param db The database
     * @param oldVersion The old database version
     * @param newVersion The new database version
     */
    void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);

    /**
     * Gets the minimum database version required for this module.
     */
    int getMinDatabaseVersion();
}

