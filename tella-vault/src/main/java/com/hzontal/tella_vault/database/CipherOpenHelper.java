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
import java.util.ArrayList;
import java.util.List;

abstract class CipherOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "CipherOpenHelper";

    final Context context;
    final byte[] password;

    CipherOpenHelper(@NonNull Context context, byte[] password) {
        super(
                context,
                DATABASE_NAME,
                encodeRawKeyToStr(password),
                null,
                DATABASE_VERSION,
                MIN_DATABASE_VERSION,
                null, null,
                false
        );

        this.context = context.getApplicationContext();
        this.password = password;
    }

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

    public static String encodeRawKeyToStr(byte[] raw_key) {
        return new String(encodeRawKey(raw_key));
    }

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
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
            out[j++] = toDigits[0x0F & data[i]];
        }
        return out;
    }

    public static void migrateSqlCipher3To4IfNeeded(@NonNull Context context, byte[] key) {
        String oldDbPath = context.getDatabasePath(CIPHER3_DATABASE_NAME).getAbsolutePath();
        File oldDbFile = new File(oldDbPath);

        if (!oldDbFile.exists()) {
            Log.d("Migration", "Old database does not exist, no migration needed.");
            return;
        }

        String newDbPath = context.getDatabasePath(DATABASE_NAME).getPath();
        File newDbFile = new File(newDbPath);

        if (newDbFile.exists()) {
            newDbFile.delete();
        }

        try {
            SQLiteDatabase oldDb = SQLiteDatabase.openOrCreateDatabase(oldDbPath, encodeRawKeyToStr(key), null, null, new SQLiteDatabaseHook() {
                @Override
                public void preKey(SQLiteConnection connection) {
               }

                @Override
                public void postKey(SQLiteConnection connection) {
                    connection.executeForString("PRAGMA key = '" + encodeRawKeyToStr(key) + "';", null, null);
                    connection.execute("PRAGMA cipher_page_size = 1024;", null, null);
                    connection.execute("PRAGMA kdf_iter = 64000;", null, null);
                    connection.execute("PRAGMA cipher_hmac_algorithm = HMAC_SHA1;", null, null);
                    connection.execute("PRAGMA cipher_kdf_algorithm = PBKDF2_HMAC_SHA1;", null, null);                }
            });

            oldDb.rawExecSQL(String.format("ATTACH DATABASE '%s' AS sqlcipher4 KEY '%s';", newDbPath, encodeRawKeyToStr(key)));
            Cursor cursor = oldDb.rawQuery("SELECT sqlcipher_export('sqlcipher4');", null);
            if (cursor != null && cursor.moveToFirst()) {
                cursor.close();
            }

            oldDb.execSQL("DETACH DATABASE sqlcipher4;");
            oldDb.close();

            SQLiteDatabase newDb = SQLiteDatabase.openDatabase(newDbPath, encodeRawKeyToStr(key),null,SQLiteDatabase.OPEN_READWRITE ,null, new SQLiteDatabaseHook() {
                @Override
                public void preKey(SQLiteConnection connection) {
                }

                @Override
                public void postKey(SQLiteConnection connection) {
                    connection.executeForString("PRAGMA key = '" + encodeRawKeyToStr(key) + "';", null, null);
                }
            });

            if (newDbFile.exists()) {
                long newSize = newDbFile.length();
                Log.d("TAG", "New database file size: " + newSize + " bytes");
            }

            Log.d("Migration", "Database migration from SQLCipher 3 to 4 was successful.");
        } catch (Exception e) {
            Log.e("Migration", "Error during migration", e);
        }
    }

    @NonNull
    @Override
    public SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase db =SQLiteDatabase.openDatabase(context.getDatabasePath(DATABASE_NAME).getPath(), encodeRawKeyToStr(password),null,SQLiteDatabase.OPEN_READWRITE ,null, new SQLiteDatabaseHook() {
            @Override
            public void preKey(SQLiteConnection connection) {
            }

            @Override
            public void postKey(SQLiteConnection connection) {
                connection.executeForString("PRAGMA key = '" + encodeRawKeyToStr(password) + "';", null, null);
            }
        });

        return db;
    }

}
