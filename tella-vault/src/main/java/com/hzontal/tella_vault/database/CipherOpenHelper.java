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

import org.hzontal.tella.keys.util.Preferences;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.CharBuffer;


abstract class CipherOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "CipherOpenHelper";

    final Context context;
    final DatabaseSecret databaseSecret;

    CipherOpenHelper(@NonNull Context context, @NonNull DatabaseSecret databaseSecret) {
        super(
                context,
                DATABASE_NAME,
                databaseSecret.asString(),
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
                        if (currentTime - com.hzontal.tella_vault.database.Preferences.getLastVacuumTime() > 604_800_000) {
                            connection.execute("VACUUM;", null, null);
                            com.hzontal.tella_vault.database.Preferences.setLastVacuumNow();
                        }
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
        this.databaseSecret = databaseSecret;
    }

    private static void applySQLCipherPragmas(SQLiteConnection connection, boolean useSQLCipher4) {
        if (useSQLCipher4) {
            connection.execute("PRAGMA kdf_iter = '256000';", null, null);
        }
        else {
            connection.execute("PRAGMA cipher_compatibility = 3;", null, null);
            connection.execute("PRAGMA kdf_iter = '1';", null, null);
        }

        connection.execute("PRAGMA cipher_page_size = 4096;", null, null);
    }

    public static void migrateSqlCipher3To4IfNeeded(@NonNull Context context, @NonNull DatabaseSecret databaseSecret) throws Exception {
        String oldDbPath = context.getDatabasePath(CIPHER3_DATABASE_NAME).getPath();
        File oldDbFile = new File(oldDbPath);

        // If the old SQLCipher3 database file doesn't exist then no need to do anything
        if (!oldDbFile.exists()) { return; }

        // Define the location for the new database
        String newDbPath = context.getDatabasePath(DATABASE_NAME).getPath();
        File newDbFile = new File(newDbPath);

        try {
            // If the new database file already exists then check if it's valid first, if it's in an
            // invalid state we should delete it and try to migrate again
            if (newDbFile.exists()) {
                // If the old database hasn't been modified since the new database was created, then we can
                // assume the user hasn't downgraded for some reason and made changes to the old database and
                // can remove the old database file (it won't be used anymore)
                if (oldDbFile.lastModified() <= newDbFile.lastModified()) {
                    try {
                        SQLiteDatabase newDb = CipherOpenHelper.open(newDbPath, databaseSecret, true);
                        int version = newDb.getVersion();
                        newDb.close();

                        // Make sure the new database has it's version set correctly (if not then the migration didn't
                        // fully succeed and the database will try to create all it's tables and immediately fail so
                        // we will need to remove and remigrate)
                        if (version > 0) {
                            // TODO: Delete 'CIPHER3_DATABASE_NAME' once enough time has past
//            //noinspection ResultOfMethodCallIgnored
//            oldDbFile.delete();
                            return;
                        }
                    }
                    catch (Exception e) {
                        Log.i(TAG, "Failed to retrieve version from new database, assuming invalid and remigrating");
                    }
                }

                // If the old database does have newer changes then the new database could have stale/invalid
                // data and we should re-migrate to avoid losing any data or issues
                if (!newDbFile.delete()) {
                    throw new Exception("Failed to remove invalid new database");
                }
            }

            if (!newDbFile.createNewFile()) {
                throw new Exception("Failed to create new database");
            }

            // Open the old database and extract it's version
            SQLiteDatabase oldDb = CipherOpenHelper.open(oldDbPath, databaseSecret, false);
            int oldDbVersion = oldDb.getVersion();

            // Export the old database to the new one (will have the default 'kdf_iter' and 'page_size' settings)
            oldDb.rawExecSQL(
                    String.format("ATTACH DATABASE '%s' AS sqlcipher4 KEY '%s'", newDbPath, databaseSecret.asString())
            );
            Cursor cursor = oldDb.rawQuery("SELECT sqlcipher_export('sqlcipher4')");
            cursor.moveToLast();
            cursor.close();
            oldDb.rawExecSQL("DETACH DATABASE sqlcipher4");
            oldDb.close();

            // Open the newly migrated database (to ensure it works) and set it's version so we don't try
            // to run any of our custom migrations
            SQLiteDatabase newDb = CipherOpenHelper.open(newDbPath, databaseSecret, true);
            newDb.setVersion(oldDbVersion);
            newDb.close();

            // TODO: Delete 'CIPHER3_DATABASE_NAME' once enough time has past
            // Remove the old database file since it will no longer be used
//      //noinspection ResultOfMethodCallIgnored
//      oldDbFile.delete();
        }
        catch (Exception e) {
            Log.e(TAG, "Migration from SQLCipher3 to SQLCipher4 failed", e);

            // If an exception was thrown then we should remove the new database file (it's probably invalid)
            if (!newDbFile.delete()) {
                Log.e(TAG, "Unable to delete invalid new database file");
            }

            // Notify the user of the issue so they know they can downgrade until the issue is fixed
           /* NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            String channelId = context.getString(R.string.NotificationChannel_failures);

            if (NotificationChannels.supported()) {
                NotificationChannel channel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH);
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(context.getResources().getColor(R.color.textsecure_primary))
                    .setCategory(NotificationCompat.CATEGORY_ERROR)
                    .setContentTitle(context.getString(R.string.ErrorNotifier_migration))
                    .setContentText(context.getString(R.string.ErrorNotifier_migration_downgrade))
                    .setAutoCancel(true);

            if (!NotificationChannels.supported()) {
                builder.setPriority(NotificationCompat.PRIORITY_HIGH);
            }

            notificationManager.notify(5874, builder.build());*/

            // Throw the error (app will crash but there is nothing else we can do unfortunately)
            throw e;
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
