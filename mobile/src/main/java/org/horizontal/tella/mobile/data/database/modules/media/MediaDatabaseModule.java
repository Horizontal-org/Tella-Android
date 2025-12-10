package org.horizontal.tella.mobile.data.database.modules.media;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.horizontal.tella.mobile.data.database.D;
import org.horizontal.tella.mobile.data.database.modules.DatabaseModule;

import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.cddl;
import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.sq;

/**
 * Database module for Media Files functionality.
 * Manages tables for:
 * - Media files
 * - Media file uploads
 */
public class MediaDatabaseModule implements DatabaseModule {
    private static final int MIN_DB_VERSION = 1;

    @Override
    public String getModuleName() {
        return "Media";
    }

    @Override
    public int getMinDatabaseVersion() {
        return MIN_DB_VERSION;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // DBv1
        db.execSQL(createTableMediaFile());

        // DBv5
        db.execSQL(alterTableMediaFileAddHash());

        // DBv7
        db.execSQL(createTableMediaFileUploads());

        // DBv8
        db.execSQL(alterTableMediaFileUploadsAddMetadata());
        db.execSQL(alterTableMediaFileUploadsAddManual());
        db.execSQL(alterTableMediaFileUploadsAddServer());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            db.execSQL(alterTableMediaFileAddHash());
        }
        if (oldVersion < 7) {
            db.execSQL(createTableMediaFileUploads());
        }
        if (oldVersion < 8) {
            db.execSQL(alterTableMediaFileUploadsAddServer());
            db.execSQL(alterTableMediaFileUploadsAddManual());
            db.execSQL(alterTableMediaFileUploadsAddMetadata());
        }
    }

    private String createTableMediaFile() {
        return "CREATE TABLE " + sq(D.T_MEDIA_FILE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_PATH, D.TEXT, true) + " , " +
                cddl(D.C_UID, D.TEXT, true) + " , " +
                cddl(D.C_FILE_NAME, D.TEXT, true) + " , " +
                cddl(D.C_METADATA, D.TEXT) + " , " +
                cddl(D.C_THUMBNAIL, D.BLOB) + " , " +
                cddl(D.C_CREATED, D.INTEGER) + " , " +
                cddl(D.C_DURATION, D.INTEGER) + " , " +
                cddl(D.C_ANONYMOUS, D.INTEGER) + " , " +
                cddl(D.C_SIZE, D.INTEGER) + " , " +
                "UNIQUE(" + sq(D.C_UID) + ")" +
                "UNIQUE(" + sq(D.C_PATH) + ", " + sq(D.C_FILE_NAME) + ")" +
                ");";
    }

    private String alterTableMediaFileAddHash() {
        return "ALTER TABLE " + sq(D.T_MEDIA_FILE) + " ADD COLUMN " +
                cddl(D.C_HASH, D.INTEGER, false);
    }

    private String createTableMediaFileUploads() {
        return "CREATE TABLE " + sq(D.T_MEDIA_FILE_UPLOAD) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_MEDIA_FILE_ID, D.INTEGER, false) + " UNIQUE, " +
                cddl(D.C_UPDATED, D.INTEGER, true) + " , " +
                cddl(D.C_CREATED, D.INTEGER, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " , " +
                cddl(D.C_SIZE, D.INTEGER) + " , " +
                cddl(D.C_UPLOADED, D.INTEGER) + " DEFAULT 0, " +
                cddl(D.C_RETRY_COUNT, D.INTEGER) + " DEFAULT 0, " +
                cddl(D.C_SET, D.INTEGER) + " , " +
                "FOREIGN KEY(" + sq(D.C_MEDIA_FILE_ID) + ") REFERENCES " +
                sq(D.T_MEDIA_FILE) + "(" + sq(D.C_ID) + ") ON DELETE SET NULL" +
                ");";
    }

    private String alterTableMediaFileUploadsAddMetadata() {
        return "ALTER TABLE " + sq(D.T_MEDIA_FILE_UPLOAD) + " ADD COLUMN " +
                cddl(D.C_INCLUDE_METADATA, D.INTEGER, true) + " DEFAULT 0";
    }

    private String alterTableMediaFileUploadsAddManual() {
        return "ALTER TABLE " + sq(D.T_MEDIA_FILE_UPLOAD) + " ADD COLUMN " +
                cddl(D.C_MANUAL_UPLOAD, D.INTEGER, true) + " DEFAULT 0";
    }

    private String alterTableMediaFileUploadsAddServer() {
        return "ALTER TABLE " + sq(D.T_MEDIA_FILE_UPLOAD) + " ADD COLUMN " +
                cddl(D.C_SERVER_ID, D.INTEGER, true) + " DEFAULT 0";
    }
}

