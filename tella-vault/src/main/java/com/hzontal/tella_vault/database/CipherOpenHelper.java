package com.hzontal.tella_vault.database;

import android.content.Context;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabase.CursorFactory;
import net.sqlcipher.database.SQLiteException;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.lang.reflect.Method;
import java.nio.CharBuffer;


abstract class CipherOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "CipherOpenHelper";

    CipherOpenHelper(Context context, String name,
                     CursorFactory factory, int version) {
        super(context, name, factory, version/*, new CipherV3MigrationHook()*/);
    }

    /**
     * Create and/or open a database that will be used for reading and writing.
     * Once opened successfully, the database is cached, so you can call this
     * method every time you need to write to the database. Make sure to call
     * {@link #close} when you no longer need it.
     * <p>
     * Errors such as bad permissions or a full disk may cause this operation to
     * fail, but future attempts may succeed if the problem is fixed.
     * </p>
     *
     * @return a read/write database object valid until {@link #close} is called
     * @throws SQLiteException if the database cannot be opened for writing
     */
    synchronized SQLiteDatabase getWritableDatabase(byte[] key) {
        return super.getWritableDatabase(encodeRawKey(key));
    }

    /**
     * Create and/or open a database. This will be the same object returned by
     * {@link #getWritableDatabase} unless some problem, such as a full disk,
     * requires the database to be opened read-only. In that case, a read-only
     * database object will be returned. If the problem is fixed, a future call
     * to {@link #getWritableDatabase} may succeed, in which case the read-only
     * database object will be closed and the read/write object will be returned
     * in the future.
     *
     * @return a database object valid until {@link #getWritableDatabase} or
     * {@link #close} is called.
     * @throws SQLiteException if the database cannot be opened
     */
    public synchronized SQLiteDatabase getReadableDatabase(byte[] key) {
        return super.getReadableDatabase(encodeRawKey(key));
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
