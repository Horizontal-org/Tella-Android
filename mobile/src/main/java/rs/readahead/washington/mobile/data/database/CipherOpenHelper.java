package rs.readahead.washington.mobile.data.database;

import static rs.readahead.washington.mobile.data.database.D.CIPHER3_DATABASE_NAME;
import static rs.readahead.washington.mobile.data.database.D.DATABASE_NAME;
import static rs.readahead.washington.mobile.data.database.D.DATABASE_VERSION;
import static rs.readahead.washington.mobile.data.database.D.MIN_DATABASE_VERSION;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import androidx.annotation.NonNull;

import com.hzontal.tella_vault.database.DatabaseSecret;
import com.hzontal.tella_vault.database.Preferences;

import net.zetetic.database.sqlcipher.SQLiteConnection;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.CharBuffer;


abstract class CipherOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "CipherOpenHelper";

    final Context context;
    //final DatabaseSecret databaseSecret;

    CipherOpenHelper(@NonNull Context context, byte[] password) {
        super(
                context,
                com.hzontal.tella_vault.database.D.DATABASE_NAME,
                password,
                null,
                DATABASE_VERSION,
                MIN_DATABASE_VERSION,
                null,
                new SQLiteDatabaseHook() {
                    @Override
                    public void preKey(SQLiteConnection connection) {
                      applySQLCipherPragmas(connection, true);
                    }

                    @Override
                    public void postKey(SQLiteConnection connection) {
                        applySQLCipherPragmas(connection, true);

                        // if not vacuumed in a while, perform that operation
                        long currentTime = System.currentTimeMillis();
                        // 7 days
                      // if (currentTime - getLastVacuumTime() > 604_800_000) {
                        //   connection.execute("VACUUM;", null, null);
                          // Preferences.setLastVacuumNow();
                          //}
                    }
                },
                // Note: Now that we support concurrent database reads the migrations are actually non-blocking
                // because of this we need to initially open the database with writeAheadLogging (WAL mode) disabled
                // and enable it once the database officially opens it's connection (which will cause it to re-connect
                // in WAL mode) - this is a little inefficient but will prevent SQL-related errors/crashes due to
                // incomplete migrations
                false
        );

        this.context        = context.getApplicationContext();
        //this.databaseSecret = databaseSecret;
    }

    private static void applySQLCipherPragmas(SQLiteConnection connection, boolean useSQLCipher4) {
        if (useSQLCipher4) {
            connection.execute("PRAGMA cipher_compatibility = '4';",null, null);
            connection.execute("PRAGMA kdf_iter = '256000';", null, null);
        }
        else {
            connection.execute("PRAGMA cipher_compatibility = 3;", null, null);
            connection.execute("PRAGMA kdf_iter = '1';", null, null);
        }

        connection.execute("PRAGMA cipher_page_size = 4096;", null, null);
    }


    /**
     * Copy the table contents from source to target with only 1 transaction.
     * Before calling this function, ensure below conditions:
     * 1) target database file exists
     * 2) target database SQLiteOpenHelper#onCreate has been called
     * 3) target database table structure is exactly the same as source database
     *
     * @param context             The context to access resources
     * @param srcWritableDatabase The legacy SQLiteDatabase, i.e. old.db
     * @param targetDbFileName    The target database file name, i.e. new.db
     * @param key                 The encryption key for the new database
     * @param tableNames          The tables to be copied
     * @return boolean value whether migration succeeded
     */
    public static boolean migrateTables(Context context, SQLiteDatabase srcWritableDatabase, String targetDbFileName, String key, String... tableNames) {
        if (context == null) {
            Log.d(TAG, "migrateTables: invalid context");
            return false;
        }
        if (srcWritableDatabase == null) {
            Log.d(TAG, "migrateTables: invalid srcWritableDatabase");
            return false;
        }
        if (targetDbFileName == null || targetDbFileName.isEmpty()) {
            Log.d(TAG, "migrateTables: invalid targetDbFileName");
            return false;
        }
        if (tableNames == null || tableNames.length == 0) {
            Log.d(TAG, "migrateTables: invalid tableNames");
            return false;
        }

        File targetDbFile = context.getDatabasePath(targetDbFileName);
        if (targetDbFile == null || !targetDbFile.exists()) {
            Log.d(TAG, "migrateTables: target db file doesn't exist: " + targetDbFileName);
            return false;
        }

        Log.d(TAG, "migrateTables: targetDbFileName=" + targetDbFileName);

        boolean success = true;
        try {
            Log.d(TAG, "migrateTables: attach database");
            srcWritableDatabase.execSQL("ATTACH DATABASE '" + targetDbFile.getPath() + "' AS target KEY '" + key + "'");
        } catch (SQLException e) {
            success = false;
            Log.e(TAG, "migrateTables: exception=" + e);
        }

        srcWritableDatabase.beginTransaction();
        try {
            Log.d(TAG, "migrateTables: start copy tables");
            for (String tableName : tableNames) {
                Log.d(TAG, "migrateTables: start copy table: " + tableName);
                srcWritableDatabase.execSQL("INSERT INTO target." + tableName + " SELECT * FROM " + tableName);
            }
            srcWritableDatabase.setTransactionSuccessful();
            Log.d(TAG, "migrateTables: end copy tables");
        } catch (Exception e) {
            Log.e(TAG, "migrateTables: exception=" + e);
            success = false;
        } finally {
            srcWritableDatabase.endTransaction();
            srcWritableDatabase.execSQL("DETACH DATABASE target");
            Log.d(TAG, "migrateTables: detach database");
        }
        Log.d(TAG, "migrateTables: success=" + success);
        return success;
    }

    public static void migrateSqlCipher3To4IfNeeded(@NonNull Context context, byte[] key) {
        String oldDbPath = context.getDatabasePath(com.hzontal.tella_vault.database.D.CIPHER3_DATABASE_NAME).getAbsolutePath();
        File oldDbFile = new File(oldDbPath);

        // If the old SQLCipher3 database file doesn't exist then just return early
        if (!oldDbFile.exists()) {
            Log.d(TAG, "Old database file does not exist at: " + oldDbPath);
            return;
        }

        // Log the path to ensure it's correct
        Log.d(TAG, "Old database file path: " + oldDbPath);

        // If the new database file already exists then we probably had a failed migration and it's likely in
        // an invalid state so should delete it
        String newDbPath = context.getDatabasePath(com.hzontal.tella_vault.database.D.DATABASE_NAME).getPath();
        File newDbFile = new File(newDbPath);

        if (newDbFile.exists()) {
            if (!newDbFile.delete()) {
                Log.e(TAG, "Failed to delete existing new database file at: " + newDbPath);
                return;
            }
        }

        try {
            if (!newDbFile.createNewFile()) {
                Log.e(TAG, "Failed to create new database file at: " + newDbPath);
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while creating new database file", e);
            return;
        }

        try {
            // Open the old database
            SQLiteDatabase oldDb = SQLiteDatabase.openDatabase(oldDbPath, key, null, SQLiteDatabase.OPEN_READWRITE, new SQLiteDatabaseHook() {
                @Override
                public void preKey(SQLiteConnection connection) {
                    connection.execute("PRAGMA cipher_compatibility = 3;", null, null);
                    connection.execute("PRAGMA kdf_iter = 64000;", null, null);
                    connection.execute("PRAGMA cipher_page_size = 1024;", null, null);
                }

                @Override
                public void postKey(SQLiteConnection connection) {
                    connection.execute("PRAGMA cipher_compatibility = 3;", null, null);
                    connection.execute("PRAGMA kdf_iter = 64000;", null, null);
                    connection.execute("PRAGMA cipher_page_size = 1024;", null, null);
                }
            });

            // Verify if old database is opened successfully
            if (oldDb == null) {
                Log.e(TAG, "Failed to open old database. Database object is null.");
                return;
            }

            Log.d(TAG, "Old database opened successfully");

            // Check if the database is actually a valid SQLite database
            if (!isDatabaseValid(oldDb)) {
                Log.e(TAG, "Old database is not a valid SQLite database");
                oldDb.close();
                return;
            }

            // Export the old database to the new one (will have the default 'kdf_iter' and 'page_size' settings)
            int oldDbVersion = oldDb.getVersion();
            oldDb.rawExecSQL(String.format("ATTACH DATABASE '%s' AS sqlcipher4 KEY '%s'", newDbPath, key));
            Cursor cursor = oldDb.rawQuery("SELECT sqlcipher_export('sqlcipher4')");
            cursor.moveToLast();
            cursor.close();
            oldDb.rawExecSQL("DETACH DATABASE sqlcipher4");
            oldDb.close();

            // Open the new database
            SQLiteDatabase newDb = SQLiteDatabase.openDatabase(newDbPath, key, null, SQLiteDatabase.OPEN_READWRITE, new SQLiteDatabaseHook() {
                @Override
                public void preKey(SQLiteConnection connection) {
                    connection.execute("PRAGMA cipher_default_kdf_iter = 256000;", null, null);
                    connection.execute("PRAGMA cipher_default_page_size = 4096;", null, null);
                }

                @Override
                public void postKey(SQLiteConnection connection) {
                    connection.execute("PRAGMA cipher_default_kdf_iter = 256000;", null, null);
                    connection.execute("PRAGMA cipher_default_page_size = 4096;", null, null);
                }
            });

            // Set the version of the new database to match the old database
            newDb.setVersion(oldDbVersion);
            newDb.close();

            // Optional: Delete the old database file if migration is successful
            // oldDbFile.delete();
        } catch (Exception e) {
            Log.e(TAG, "Exception during database migration", e);

            // Clean up: Delete the new database file if migration failed
            if (newDbFile.exists() && !newDbFile.delete()) {
                Log.e(TAG, "Failed to delete new database file after migration failure");
            }
        }
    }

    private static boolean isDatabaseValid(SQLiteDatabase db) {
        try {
            // Try to execute a simple query to determine if the database is valid
            db.rawQuery("SELECT COUNT(*) FROM sqlite_schema", null).close();
            return true;
        } catch (Exception e) {
            // Log any exceptions, indicating the database is invalid
            Log.e(TAG, "Error validating database", e);
            return false;
        }
    }


    private static SQLiteDatabase open(String path, DatabaseSecret databaseSecret, boolean useSQLCipher4) {
        return SQLiteDatabase.openDatabase(path, databaseSecret.asString(), null, SQLiteDatabase.OPEN_READWRITE, new SQLiteDatabaseHook() {
            @Override
            public void preKey(SQLiteConnection connection) { CipherOpenHelper.applySQLCipherPragmas(connection, useSQLCipher4); }

            @Override
            public void postKey(SQLiteConnection connection) { CipherOpenHelper.applySQLCipherPragmas(connection, useSQLCipher4); }
        });
    }


    /**
     * Formats a byte sequence into the literal string format expected by
     * SQLCipher: hex'HEX ENCODED BYTES' The key data must be 256 bits (32
     * bytes) wide. The key data will be formatted into a 64 character hex
     * string with a special prefix and suffix SQLCipher uses to distinguish raw
     * key data from a password.
     *
     * @param raw_key a 32 byte array
     * @return the encoded key
     * @link http://sqlcipher.net/sqlcipher-api/#key
     */
    private static char[] encodeRawKey(byte[] raw_key) {
        if (raw_key.length != 32)
            throw new IllegalArgumentException("provided key not 32 bytes (256 bits) wide");

        final String kPrefix;
        final String kSuffix;

        if (sqlcipher_uses_native_key) {
            Log.d(TAG, "sqlcipher uses native method to set key");
            kPrefix = "x'";
            kSuffix = "'";
        } else {
            Log.d(TAG, "sqlcipher uses PRAGMA to set key - SPECIAL HACK IN PROGRESS");
            kPrefix = "x''";
            kSuffix = "''";
        }
        final char[] key_chars = encodeHex(raw_key, HEX_DIGITS_LOWER);
        if (key_chars.length != 64)
            throw new IllegalStateException("encoded key is not 64 bytes wide");

        char[] kPrefix_c = kPrefix.toCharArray();
        char[] kSuffix_c = kSuffix.toCharArray();
        CharBuffer cb = CharBuffer.allocate(kPrefix_c.length + kSuffix_c.length + key_chars.length);
        cb.put(kPrefix_c);
        cb.put(key_chars);
        cb.put(kSuffix_c);

        return cb.array();
    }

    /**
     * @see #encodeRawKey(byte[])
     */
    public static String encodeRawKeyToStr(byte[] raw_key) {
        return new String(encodeRawKey(raw_key));
    }

    /*
     * Special hack for detecting whether or not we're using a new SQLCipher for
     * Android library The old version uses the PRAGMA to set the key, which
     * requires escaping of the single quote characters. The new version calls a
     * native method to set the key instead.
     * @see https://github.com/sqlcipher/android-database-sqlcipher/pull/95
     */
    private static final boolean sqlcipher_uses_native_key = check_sqlcipher_uses_native_key();

    private static boolean check_sqlcipher_uses_native_key() {

        for (Method method : SQLiteDatabase.class.getDeclaredMethods()) {
            if (method.getName().equals("native_key"))
                return true;
        }
        return false;
    }

    private static final char[] HEX_DIGITS_LOWER = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private static char[] encodeHex(final byte[] data, final char[] toDigits) {
        final int l = data.length;
        final char[] out = new char[l << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
            out[j++] = toDigits[0x0F & data[i]];
        }
        return out;
    }
}
