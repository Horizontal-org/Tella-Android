package com.hzontal.tella_vault.database;

import static com.hzontal.tella_vault.database.D.CIPHER3_DATABASE_NAME;
import static com.hzontal.tella_vault.database.D.DATABASE_NAME;
import static com.hzontal.tella_vault.database.D.MIN_DATABASE_VERSION;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import com.hzontal.tella_vault.VaultFile;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.io.File;


public class VaultSQLiteOpenHelper extends CipherOpenHelper {
    private static final String PREFS_NAME = "VaultSQLiteOpenHelperPrefs";
    private static final String KEY_ALREADY_MIGRATED = "alreadyMigrated";
    private final byte[] password;
    private final SharedPreferences sharedPreferences;

    public VaultSQLiteOpenHelper(Context context, byte[] password) {
        super(context, password);
        this.password = password;
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean alreadyMigrated = sharedPreferences.getBoolean(KEY_ALREADY_MIGRATED, false);
       if (!alreadyMigrated) {
            migrateDatabase();
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);

        if (!db.isReadOnly()) {
           db.execSQL("PRAGMA foreign_keys=ON;");
        }
        db.enableWriteAheadLogging();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // we have started from version 1

        // DBv1
        createVaultFileTable(db);
        insertRootVaultFile(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < newVersion) {
            migrateDatabase();
        }
    }

    private void migrateDatabase() {
        SQLiteDatabase oldDb = null;
        SQLiteDatabase newDb = null;

        try {
            String databaseDir = context.getApplicationInfo().dataDir; // Path to your app's data directory
            String databasePathOld = databaseDir + File.separator + CIPHER3_DATABASE_NAME;
            String databasePathNew = context.getDatabasePath(DATABASE_NAME).getPath();

            // Log paths for debugging
            Log.d("Migration", "Old DB Path: " + databasePathOld);
            Log.d("Migration", "New DB Path: " + databasePathNew);

            // Open the old database with the existing 3.x settings
            oldDb =SQLiteDatabase.openOrCreateDatabase(
                    databasePathNew,
                    encodeRawKeyToStr(password),
                    null,
                    null
            );

            // Open the new database with SQLCipher 4 settings
            newDb = SQLiteDatabase.openOrCreateDatabase(
                    databasePathNew,
                    encodeRawKeyToStr(password),
                    null,
                    null
            );

            // Step 1: Attach new database with updated settings
            oldDb.execSQL("ATTACH DATABASE '" + databasePathNew + "' AS newdb KEY '" + encodeRawKeyToStr(password) + "'");

            // Set PRAGMA statements for new database settings on 'newdb'
            oldDb.execSQL("PRAGMA newdb.cipher_page_size = 4096");
            oldDb.execSQL("PRAGMA newdb.kdf_iter = 10000");
            oldDb.execSQL("PRAGMA newdb.cipher_hmac_algorithm = HMAC_SHA256");
            oldDb.execSQL("PRAGMA newdb.cipher_kdf_algorithm = PBKDF2_HMAC_SHA256");

            // Step 2: Copy schema from old database to new database
            Cursor cursor = oldDb.rawQuery("SELECT sql FROM sqlite_master WHERE type='table'", null);
            while (cursor.moveToNext()) {
                String sql = cursor.getString(0);
                newDb.execSQL(sql);
            }
            cursor.close();

            // Step 3: Copy data from old tables to corresponding new tables
            cursor = oldDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
            while (cursor.moveToNext()) {
                String tableName = cursor.getString(0);
                if (!tableName.startsWith("sqlite_")) { // Exclude SQLite system tables
                    String copyTable = String.format("INSERT INTO newdb.%s SELECT * FROM %s", tableName, tableName);
                    oldDb.execSQL(copyTable);
                }
            }
            cursor.close();

            // Step 4: Detach the new database
            oldDb.execSQL("DETACH DATABASE 'newdb'");

            // Close databases
            oldDb.close();
            newDb.close();

            // Rename the new database file to replace the old one
            File oldFile = context.getDatabasePath(CIPHER3_DATABASE_NAME);
            File newFile = context.getDatabasePath(DATABASE_NAME);
            if (oldFile.exists()) {
               // oldFile.delete();
            }
            if (newFile.exists()) {
                newFile.renameTo(oldFile);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to migrate from SQLCipher 3 to 4", e);
        } finally {
            if (oldDb != null && oldDb.isOpen()) {
                oldDb.close();
            }
            if (newDb != null && newDb.isOpen()) {
                newDb.close();
            }
        }
    }
    private void createVaultFileTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + sq(D.T_VAULT_FILE) + " (" +
                cddl(D.C_ID, D.TEXT) + " PRIMARY KEY , " +
                cddl(D.C_PARENT_ID, D.TEXT) + ", " +
                cddl(D.C_NAME, D.TEXT, true) + ", " +
                cddl(D.C_TYPE, D.INTEGER, true) + ", " +
                cddl(D.C_HASH, D.TEXT) + ", " +
                cddl(D.C_METADATA, D.TEXT) + ", " +
                cddl(D.C_PATH, D.TEXT) + ", " +
                cddl(D.C_MIME_TYPE, D.TEXT) + ", " +
                cddl(D.C_THUMBNAIL, D.BLOB) + " , " +
                cddl(D.C_CREATED, D.INTEGER, true) + " , " +
                cddl(D.C_DURATION, D.INTEGER, true, 0) + ", " +
                cddl(D.C_ANONYMOUS, D.INTEGER, true, 0) + ", " +
                cddl(D.C_SIZE, D.INTEGER, true, 0) + ", " +
                "UNIQUE(" + sq(D.C_PARENT_ID) + ", " + sq(D.C_NAME) + ")" +
                ");");
    }

    private void insertRootVaultFile(SQLiteDatabase db) {
        ContentValues values = new ContentValues();

        values.put(D.C_ID, VaultDataSource.ROOT_UID);
        values.put(D.C_NAME, "");
        values.put(D.C_TYPE, VaultFile.Type.DIRECTORY.getValue());
        values.put(D.C_CREATED, System.currentTimeMillis());

        db.insert(D.T_VAULT_FILE, null, values);
    }
    private SQLiteDatabase getOldDatabase() {
        String oldDbPath = context.getDatabasePath(CIPHER3_DATABASE_NAME).getPath();
        return SQLiteDatabase.openDatabase(oldDbPath, null, SQLiteDatabase.OPEN_READWRITE);
    }


    private static String objQuote(String str) {
        return "`" + str + "`";
    }

    private static String sq(String unQuotedText) {
        return " " + objQuote(unQuotedText) + " ";
    }

    private static String cddl(String columnName, String columnType) {
        return objQuote(columnName) + " " + columnType;
    }

    private static String cddl(String columnName, String columnType, boolean notNull) {
        return objQuote(columnName) + " " + columnType + (notNull ? " NOT NULL" : "");
    }

    private static String cddl(String columnName, String columnType, boolean notNull, int defaultValue) {
        return objQuote(columnName) + " " + columnType + (notNull ? " NOT NULL " : "") + "DEFAULT " + defaultValue;
    }
}
