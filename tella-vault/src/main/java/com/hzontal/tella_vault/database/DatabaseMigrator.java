package com.hzontal.tella_vault.database;

import android.content.Context;
import android.database.Cursor;


import net.sqlcipher.DatabaseErrorHandler;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

import timber.log.Timber;


public class DatabaseMigrator {

    static public void checkAndMigrateDatabase(Context context, String databaseName, String password) {

        String path = context.getDatabasePath(databaseName).getPath();

        SQLiteDatabaseHook hook = new SQLiteDatabaseHook() {
            @Override
            public void preKey(SQLiteDatabase sqLiteDatabase) {
                sqLiteDatabase.rawQuery("PRAGMA cipher_migrate", null);

            }

            @Override
            public void postKey(SQLiteDatabase sqLiteDatabase) {
                Cursor c = sqLiteDatabase.rawQuery("PRAGMA cipher_migrate", null);

                boolean migrationOccurred = false;

                if (c.getCount() == 1) {
                    c.moveToFirst();
                    String selection = c.getString(0);

                    migrationOccurred = selection.equals("0");

                    Timber.i("selection: %s", selection);
                }

                c.close();

                Timber.i("migrationOccurred: %s", migrationOccurred);
            }};

        SQLiteDatabase database = null;
        DatabaseErrorHandler errorHandler = new DatabaseErrorHandler() {
            @Override
            public void onCorruption(SQLiteDatabase sqLiteDatabase) {
                Timber.i("Error: %s", sqLiteDatabase);
        }
        };
        try {
            database = SQLiteDatabase.openOrCreateDatabase(path, password, null, hook,errorHandler);
        }
        catch (Exception e) {
            Timber.i("Exception while trying to open db: " + e);
        }

        if (database != null) {

            database.close();
        }
    }
}
