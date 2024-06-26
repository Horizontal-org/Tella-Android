package com.hzontal.tella_vault.database;

import static com.hzontal.tella_vault.database.D.CIPHER3_DATABASE_NAME;
import static com.hzontal.tella_vault.database.D.DATABASE_NAME;
import static com.hzontal.tella_vault.database.D.DATABASE_VERSION;
import static com.hzontal.tella_vault.database.D.MIN_DATABASE_VERSION;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import androidx.annotation.NonNull;


import net.zetetic.database.sqlcipher.SQLiteConnection;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


abstract class CipherOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "CipherOpenHelper";

    final Context context;
    //final DatabaseSecret databaseSecret;

    CipherOpenHelper(@NonNull Context context, byte[] password) {
        super(
                context,
                DATABASE_NAME,
                password,
                null,
                DATABASE_VERSION,
                MIN_DATABASE_VERSION,
                null,
                new SQLiteDatabaseHook() {
                    @Override
                    public void preKey(SQLiteConnection connection) {
                        CipherOpenHelper.applySQLCipherPragmas(connection, true);
                    }

                    @Override
                    public void postKey(SQLiteConnection connection) {
                        CipherOpenHelper.applySQLCipherPragmas(connection, true);

                        // if not vacuumed in a while, perform that operation
                        long currentTime = System.currentTimeMillis();
                        // 7 days
                      //  if (currentTime - com.hzontal.tella_vault.database.Preferences.getLastVacuumTime() > 604_800_000) {
                       //     connection.execute("VACUUM;", null, null);
                       //     com.hzontal.tella_vault.database.Preferences.setLastVacuumNow();
                      //  }
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
    public static boolean execute(SQLiteDatabase database, Context context) {
        database.close();
        try {
            int rows = 0;
            byte[] password = {-79, 56, -62, -6, -110, -19, 27, -28, -77, -64, 2, -42, 1, 40, 46, 45, 86, -24, -67, 37, -50, -49, 37, -47, -90, -61, 108, 73, 13, 78, -98, -114};
            String oldDbPath = context.getDatabasePath(CIPHER3_DATABASE_NAME).getAbsolutePath();
            database = SQLiteDatabase.openDatabase(oldDbPath,password ,null,  SQLiteDatabase.OPEN_READWRITE,null);
            if(database != null){
                Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM sqlite_master;", new Object[]{});
                if(cursor != null){
                    cursor.moveToFirst();
                    rows = cursor.getInt(0);
                    cursor.close();
                }
            }
            return rows > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static void shouldOpenSQLCipher3Database(Context context,  byte[] key) {
        int count = 0;
        String oldDbPath = context.getDatabasePath(CIPHER3_DATABASE_NAME).getAbsolutePath();

        SQLiteDatabaseHook hook = new SQLiteDatabaseHook() {
            public void preKey(SQLiteConnection connection) {}
            public void postKey(SQLiteConnection connection) {
                connection.executeForString("PRAGMA cipher_compatibility = 3;", null, null);
            }
        };
        SQLiteDatabase database;
        try {
            File oldDbFile = new File(oldDbPath);
            database = SQLiteDatabase.openDatabase(oldDbFile.getAbsolutePath(), encodeRawKeyToStr(key), null, SQLiteDatabase.OPEN_READWRITE, hook);
            Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM sqlite_master;", null);
            if(cursor != null && cursor.moveToFirst()){
                count = cursor.getInt(0);
                cursor.close();
            }
           // assertThat(count, greaterThan(0));
        } finally {
           // delete(oldDbFile);
        }



}
    public static void migrateSqlCipher3To4IfNeeded(@NonNull Context context,byte[] key) throws UnsupportedEncodingException, InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] password = {-79, 56, -62, -6, -110, -19, 27, -28, -77, -64, 2, -42, 1, 40, 46, 45, 86, -24, -67, 37, -50, -49, 37, -47, -90, -61, 108, 73, 13, 78, -98, -114};

        String oldDbPath = context.getDatabasePath(CIPHER3_DATABASE_NAME).getAbsolutePath();
        File oldDbFile = new File(oldDbPath);

        if (!oldDbFile.exists()) {
            Log.d(TAG, "Old database file does not exist at: " + oldDbPath);
            return;
        }

        Log.d(TAG, "Old database file path: " + oldDbPath);

        String newDbPath = context.getDatabasePath(DATABASE_NAME).getPath();
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

        SQLiteDatabase oldDb = null;
        SQLiteDatabase newDb = null;
        Cursor cursor = null;
        byte[] salt = oldDbFile.getName().getBytes(Charset.forName("UTF-8"));
        char[] pass = new String(key, "UTF-8").toCharArray();

        KeySpec keySpec = new PBEKeySpec(pass, salt, 64000, 1024);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        SecretKey sk = factory.generateSecret(keySpec);
        byte[] derivedKey = sk.getEncoded();
        try {
            oldDb = SQLiteDatabase.openDatabase(oldDbPath, encodeRawKeyToStr(key), null,  SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY, null, null) ;
//                @Override
//                public void preKey(SQLiteConnection sqLiteConnection) {
////                    sqLiteConnection.executeForString("PRAGMA key = '" + key + "';",null,null);
////                    sqLiteConnection.executeForString("PRAGMA cipher_hmac_algorithm = HMAC_SHA1;",null,null);
////                    sqLiteConnection.executeForString("PRAGMA cipher_kdf_algorithm = PBKDF2_HMAC_SHA1;",null,null);
////                    sqLiteConnection.executeForString("PRAGMA cipher_page_size = 4096;",null,null);
////                    sqLiteConnection.executeForString("ATTACH DATABASE " + newDbPath + "AS sqlcipher4 KEY " + password + "';",null,null);
////                    sqLiteConnection.executeForString("SELECT sqlcipher_export('sqlcipher4');",null,null);
////                    sqLiteConnection.executeForString("DETACH DATABASE sqlcipher4;",null,null);
////                    sqLiteConnection.executeForString("PRAGMA kdf_iter = 64000;",null,null);
//
//
//                    //  sqLiteConnection.execute("PRAGMA cipher_compatibility = 3;",null,null);
//                  //  sqLiteConnection.execute("PRAGMA kdf_iter = 64000;",null,null);
//                 //   sqLiteConnection.execute("PRAGMA cipher_page_size = 1024;",null,null);
//
//                  //  sqLiteConnection.execute("PRAGMA cipher_page_size = 1024;",null,null);
//                  //  sqLiteConnection.execute("PRAGMA cipher_hmac_algorithm = HMAC_SHA1;",null,null);
//                 //   sqLiteConnection.execute("PRAGMA cipher_kdf_algorithm = PBKDF2_HMAC_SHA1",null,null);
//                }
//
//                @Override
//                public void postKey(SQLiteConnection sqLiteConnection) {
//                   // sqLiteConnection.executeForString("PRAGMA cipher_migrate;",null,null);
////                    PRAGMA key = '<key material>';
////                    PRAGMA cipher_page_size = 1024;
////                    PRAGMA kdf_iter = 64000;
////                    PRAGMA cipher_hmac_algorithm = HMAC_SHA1;
////                    PRAGMA cipher_kdf_algorithm = PBKDF2_HMAC_SHA1;
////                    ATTACH DATABASE 'sqlcipher-4.db' AS sqlcipher4 KEY '<key material>';
////                    SELECT sqlcipher_export('sqlcipher4');
////                    DETACH DATABASE sqlcipher4;
////                    public static final String CIPHER3_DATABASE_NAME    = "tella-vault.db";
////                    public static final String  DATABASE_NAME            = "tella-vault-v4.db";
////                    PRAGMA key = '<key material>';
////                    PRAGMA cipher_page_size = 1024;
////                    PRAGMA kdf_iter = 64000;
////                    PRAGMA cipher_hmac_algorithm = HMAC_SHA1;
////                    PRAGMA cipher_kdf_algorithm = PBKDF2_HMAC_SHA1;
////                    ATTACH DATABASE 'sqlcipher-4.db' AS sqlcipher4 KEY '<key material>';
////                    SELECT sqlcipher_export('sqlcipher4');
////                    DETACH DATABASE sqlcipher4;
////                    sqlcipher-4.db.
//                    sqLiteConnection.executeForLong("PRAGMA key = '" + encodeRawKeyToStr(key) + "';",null,null);
//                    sqLiteConnection.executeForLong("PRAGMA cipher_page_size = 1024;",null,null);
//                    sqLiteConnection.executeForLong("PRAGMA kdf_iter = 64000;",null,null);
//                    sqLiteConnection.executeForLong("PRAGMA cipher_hmac_algorithm = HMAC_SHA1;",null,null);
//                    sqLiteConnection.executeForLong("PRAGMA cipher_kdf_algorithm = PBKDF2_HMAC_SHA1;",null,null);
//                    sqLiteConnection.executeForLong("ATTACH DATABASE 'tella-vault-v4' AS tella-vault KEY  " + encodeRawKeyToStr(key)  + "';",null,null);
//                    sqLiteConnection.executeForLong("SELECT sqlcipher_export('tella-vault');",null,null);
//                    sqLiteConnection.executeForLong("DETACH DATABASE tella-vault;",null,null);
//
//
//                }
//            });

            if (oldDb == null) {
                Log.e(TAG, "Failed to open old database. Database object is null.");
                return;
            }

            Log.d(TAG, "Old database opened successfully");

            if (!isDatabaseValid(oldDb)) {
                Log.e(TAG, "Old database is not a valid SQLite database");
                oldDb.close();
                return;
            }

            int oldDbVersion = oldDb.getVersion();
            oldDb.rawExecSQL(String.format("ATTACH DATABASE '%s' AS sqlcipher4 KEY ?", newDbPath), password);
            cursor = oldDb.rawQuery("SELECT sqlcipher_export('sqlcipher4');", null);
            if (cursor.moveToLast()) {
                Log.d(TAG, "Database export completed successfully");
            }
            cursor.close();
            oldDb.rawExecSQL("DETACH DATABASE sqlcipher4");
            oldDb.close();

            newDb = SQLiteDatabase.openDatabase(newDbPath, password, null, SQLiteDatabase.OPEN_READWRITE, new SQLiteDatabaseHook() {
                @Override
                public void preKey(SQLiteConnection sqLiteConnection) {
                    sqLiteConnection.execute("PRAGMA cipher_default_kdf_iter = 256000;",null,null);
                    sqLiteConnection.execute("PRAGMA cipher_default_page_size = 4096;",null,null);
                }

                @Override
                public void postKey(SQLiteConnection sqLiteConnection) {
                    // no-op
                }
            });

            newDb.setVersion(oldDbVersion);
            newDb.close();

            Log.d(TAG, "Database migration completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Exception during database migration", e);

            if (newDbFile.exists() && !newDbFile.delete()) {
                Log.e(TAG, "Failed to delete new database file after migration failure");
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (oldDb != null && oldDb.isOpen()) {
                oldDb.close();
            }
            if (newDb != null && newDb.isOpen()) {
                newDb.close();
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
