package com.hzontal.tella_vault.database;

import static com.hzontal.tella_vault.database.D.CIPHER3_DATABASE_NAME;
import static com.hzontal.tella_vault.database.D.DATABASE_NAME;
import static com.hzontal.tella_vault.database.D.DATABASE_VERSION;
import static com.hzontal.tella_vault.database.D.MIN_DATABASE_VERSION;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;

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
    final  byte[] password;


    CipherOpenHelper(@NonNull Context context, byte[] password) {
        super(
                context,
                DATABASE_NAME,
                encodeRawKeyToStr(password),
                null,
                DATABASE_VERSION,
                MIN_DATABASE_VERSION,
                null,
                null,
                false
        );

        this.context = context.getApplicationContext();
        this.password = password;

        migrateSqlCipher3To4IfNeeded(context, password);
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

    public static void migrateSqlCipher3To4IfNeeded(@NonNull Context context, byte[] key) {
        String oldDbPath = context.getDatabasePath(CIPHER3_DATABASE_NAME).getAbsolutePath();
        File oldDbFile = new File(oldDbPath);

        if (oldDbFile.exists()) {
            long newSize = oldDbFile.length();
            Log.d("Migration", "old database file size: " + newSize + " bytes");
            Log.d("Migration", "Old DB Path: " + oldDbPath);

        }

        // If the old SQLCipher3 database file doesn't exist then just return early
        if (!oldDbFile.exists()) { return; }

        // If the new database file already exists then we probably had a failed migration and it's likely in
        // an invalid state so should delete it
        String newDbPath = context.getDatabasePath(DATABASE_NAME).getPath();
        File newDbFile = new File(newDbPath);

        if (newDbFile.exists()) { newDbFile.delete(); }

        try {
            newDbFile.createNewFile();
        }
        catch (Exception e) {
            // TODO: Communicate the error somehow???
            return;
        }

        try {
            System.loadLibrary("sqlcipher");

            // Open the old database
            SQLiteDatabase oldDb = SQLiteDatabase.openOrCreateDatabase(oldDbPath, encodeRawKeyToStr(key), null, null, new SQLiteDatabaseHook() {
                @Override
                public void preKey(SQLiteConnection connection) {
                    connection.executeForString("PRAGMA key = '" + encodeRawKeyToStr(key) + "';",null,null);
                    connection.execute("PRAGMA cipher_page_size = 1024",null,null);
                    connection.execute("PRAGMA kdf_iter = 64000",null,null);
                    connection.execute("PRAGMA cipher_hmac_algorithm = HMAC_SHA1",null,null);
                    connection.execute("PRAGMA cipher_kdf_algorithm = PBKDF2_HMAC_SHA1",null,null);
                }

                @Override
                public void postKey(SQLiteConnection connection) {
                    connection.executeForString("PRAGMA key = '" + encodeRawKeyToStr(key) + "';",null,null);
                    connection.execute("PRAGMA cipher_page_size = 1024",null,null);
                    connection.execute("PRAGMA kdf_iter = 64000",null,null);
                    connection.execute("PRAGMA cipher_hmac_algorithm = HMAC_SHA1",null,null);
                    connection.execute("PRAGMA cipher_kdf_algorithm = PBKDF2_HMAC_SHA1",null,null);                 }
            });

            // Export the old database to the new one (will have the default 'kdf_iter' and 'page_size' settings)
            int oldDbVersion = oldDb.getVersion();
            oldDb.rawExecSQL(
                    String.format("ATTACH DATABASE '%s' AS sqlcipher4 KEY '%s'", newDbPath, encodeRawKeyToStr(key))
            );
            Cursor cursor = oldDb.rawQuery("SELECT sqlcipher_export('sqlcipher4')");
            cursor.moveToLast();
            cursor.close();
            oldDb.rawExecSQL("DETACH DATABASE sqlcipher4");
            oldDb.close();

            // TODO: Performance testing

            if (newDbFile.exists()) {
                long newSize = newDbFile.length();
                Log.d("TAG", "New database file size: " + newSize + " bytes");
            }
            SQLiteDatabase newDb = SQLiteDatabase.openOrCreateDatabase(newDbPath, encodeRawKeyToStr(key), null, null, new SQLiteDatabaseHook() {
                @Override
                public void preKey(SQLiteConnection connection) {
                   // connection.executeForString("PRAGMA key = '" + encodeRawKeyToStr(key) + "';",null,null);
                    connection.execute("PRAGMA kdf_iter = '256000';", null, null);
                    connection.execute("PRAGMA cipher_page_size = 1024",null,null);
                }

                @Override
                public void postKey(SQLiteConnection connection) {
                   // connection.executeForString("PRAGMA key = '" + encodeRawKeyToStr(key) + "';",null,null);
                    connection.execute("PRAGMA kdf_iter = '256000';", null, null);
                    connection.execute("PRAGMA cipher_page_size = 1024",null,null);

                }
            });



            newDb.setVersion(oldDbVersion);
            newDb.close();

            // TODO: Delete 'CIPHER3_DATABASE_NAME'
            // TODO: What do we do if the deletion fails??? (The current logic will end up re-migrating...)
//      oldDbFile.delete();
        }
        catch (Exception e) {
            Log.d("Migration Exception", e.getLocalizedMessage());
            // TODO: Communicate the error somehow???
        }
    }
}
