package org.horizontal.tella.mobile.data.database;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.horizontal.tella.mobile.data.database.D.CIPHER3_DATABASE_NAME;
import static org.horizontal.tella.mobile.data.database.D.DATABASE_NAME;
import static org.horizontal.tella.mobile.data.database.D.DATABASE_VERSION;
import static org.horizontal.tella.mobile.data.database.D.MIN_DATABASE_VERSION;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;

import net.zetetic.database.sqlcipher.SQLiteConnection;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import timber.log.Timber;

abstract class CipherOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "CipherOpenHelper";

    final Context context;
    final byte[] password;
    final DatabasePreferences preferences;

    CipherOpenHelper(@NonNull Context context, byte[] password, @NonNull DatabasePreferences preferences) {
        super(context, DATABASE_NAME, encodeRawKeyToUtf8Bytes(password), null, DATABASE_VERSION, MIN_DATABASE_VERSION, null, null, false);

        this.context = context.getApplicationContext();
        // Never retain the caller's array reference (e.g. MainKey material); close() can zero our copy.
        this.password = Arrays.copyOf(password, password.length);
        this.preferences = preferences;
    }

    private static char[] encodeRawKey(byte[] raw_key) {
        if (raw_key.length != 32)
            throw new IllegalArgumentException("provided key not 32 bytes (256 bits) wide");

        final String kPrefix;
        final String kSuffix;

        if (sqlcipher_uses_native_key) {
            kPrefix = "x'";
            kSuffix = "'";
        } else {
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

    /** UTF-8 bytes of the SQLCipher raw key literal (e.g. {@code x'...'}), same as the String-password API path. */
    private static byte[] encodeRawKeyToUtf8Bytes(byte[] raw_key) {
        char[] encoded = encodeRawKey(raw_key);
        try {
            ByteBuffer bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap(encoded));
            byte[] out = new byte[bb.remaining()];
            bb.get(out);
            return out;
        } finally {
            Arrays.fill(encoded, '\0');
        }
    }

    private static final boolean sqlcipher_uses_native_key = check_sqlcipher_uses_native_key();

    private static boolean check_sqlcipher_uses_native_key() {
        for (Method method : SQLiteDatabase.class.getDeclaredMethods()) {
            if (method.getName().equals("native_key")) return true;
        }
        return false;
    }

    private static final char[] HEX_DIGITS_LOWER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static char[] encodeHex(final byte[] data, final char[] toDigits) {
        final int l = data.length;
        final char[] out = new char[l << 1];
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
            out[j++] = toDigits[0x0F & data[i]];
        }
        return out;
    }

    public static void migrateSqlCipher3To4IfNeeded(@NonNull Context context, byte[] key, @NonNull DatabasePreferences preferences) {
        String oldDbPath = context.getDatabasePath(CIPHER3_DATABASE_NAME).getAbsolutePath();
        File oldDbFile = new File(oldDbPath);

        if (!oldDbFile.exists()) {
            Timber.tag(TAG).d("Old database does not exist, no migration needed.");
            preferences.setFreshInstall(true);
            return;
        }

        String newDbPath = context.getDatabasePath(DATABASE_NAME).getPath();
        File newDbFile = new File(newDbPath);
        String backupDbPath = oldDbPath + ".backup";

        // Backup the old database
        try {
            copyFile(oldDbFile, new File(backupDbPath));
            Timber.tag(TAG).d("Backup of old database created at: %s", backupDbPath);
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Error creating backup of old database");
            return;
        }

        if (newDbFile.exists()) {
            newDbFile.delete();
        }

        boolean migrationSuccess = false;

        char[] keyChars = encodeRawKey(key);
        try {
            SQLiteDatabase oldDb = SQLiteDatabase.openOrCreateDatabase(
                    oldDbPath,
                    new String(keyChars),
                    null,
                    null,
                    new SQLiteDatabaseHook() {
                        @Override
                        public void preKey(SQLiteConnection connection) {
                        }

                        @Override
                        public void postKey(SQLiteConnection connection) {
                            char[] pragmaChars = encodeRawKey(key);
                            try {
                                connection.executeForString(
                                        "PRAGMA key = '" + new String(pragmaChars) + "';",
                                        null,
                                        null
                                );
                                connection.execute("PRAGMA cipher_page_size = 1024;", null, null);
                                connection.execute("PRAGMA kdf_iter = 64000;", null, null);
                                connection.execute("PRAGMA cipher_hmac_algorithm = HMAC_SHA1;", null, null);
                                connection.execute("PRAGMA cipher_kdf_algorithm = PBKDF2_HMAC_SHA1;", null, null);
                            } finally {
                                Arrays.fill(pragmaChars, '\0');
                            }
                        }
                    }
            );

            char[] attachChars = encodeRawKey(key);
            try {
                oldDb.rawExecSQL(
                        "ATTACH DATABASE '" + newDbPath + "' AS sqlcipher4 KEY '" + new String(attachChars) + "';"
                );
            } finally {
                Arrays.fill(attachChars, '\0');
            }
            Cursor cursor = oldDb.rawQuery("SELECT sqlcipher_export('sqlcipher4');", null);
            if (cursor != null && cursor.moveToFirst()) {
                cursor.close();
            }

            oldDb.execSQL("DETACH DATABASE sqlcipher4;");
            oldDb.close();

            if (newDbFile.exists()) {
                long newSize = newDbFile.length();
                Timber.tag(TAG).d("New database file size: " + newSize + " bytes");
            }

            preferences.setAlreadyMigratedMainDB(true);
            Timber.tag(TAG).d("Database migration from SQLCipher 3 to 4 was successful.");
            migrationSuccess = true;
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Error during migration");
        } finally {
            Arrays.fill(keyChars, '\0');
        }

        // Handle the result of the migration
        if (migrationSuccess) {
            Timber.tag(TAG).d("Migration successful, deleting old database...");
            oldDbFile.delete();
            new File(backupDbPath).delete();
        } else {
            Timber.tag(TAG).d("Migration failed, keeping the old database.");
        }
    }

    /**
     * Closes the database. After this, do not use this helper again to open the DB; create a new
     * helper with a fresh key material array if needed (e.g. after unlock).
     */
    @Override
    public synchronized void close() {
        try {
            super.close();
        } finally {
            if (password != null) {
                Arrays.fill(password, (byte) 0);
            }
        }
    }

    @NonNull
    @Override
    public SQLiteDatabase getWritableDatabase() {

        if (preferences.isAlreadyMigratedMainDB()) {
            char[] keyChars = encodeRawKey(password);
            try {
                return SQLiteDatabase.openDatabase(
                        context.getDatabasePath(DATABASE_NAME).getPath(),
                        new String(keyChars),
                        null,
                        SQLiteDatabase.OPEN_READWRITE,
                        null,
                        new SQLiteDatabaseHook() {
                            @Override
                            public void preKey(SQLiteConnection connection) {
                            }

                            @Override
                            public void postKey(SQLiteConnection connection) {
                                char[] pragmaChars = encodeRawKey(password);
                                try {
                                    connection.executeForString(
                                            "PRAGMA key = '" + new String(pragmaChars) + "';",
                                            null,
                                            null
                                    );
                                } finally {
                                    Arrays.fill(pragmaChars, '\0');
                                }
                            }
                        }
                );
            } finally {
                Arrays.fill(keyChars, '\0');
            }
        } else {
            Timber.tag(TAG).d("Database is from a fresh install, not calling getWritableDatabase.");
            return super.getWritableDatabase(); // or handle appropriately
        }

    }

}
