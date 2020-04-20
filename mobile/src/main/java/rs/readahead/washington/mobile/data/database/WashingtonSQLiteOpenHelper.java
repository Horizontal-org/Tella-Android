package rs.readahead.washington.mobile.data.database;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;


class WashingtonSQLiteOpenHelper extends CipherOpenHelper {
    private static final String OBJ_QUOTE = "`";


    WashingtonSQLiteOpenHelper(Context context) {
        super(context, D.DATABASE_NAME, null, D.DATABASE_VERSION);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // we have started from version 1

        // DBv1
        db.execSQL(createTableTrustedPerson());
        db.execSQL(createTableCollectServer());
        db.execSQL(createTableMediaFile());
        db.execSQL(createTableCollectBlankForm());
        db.execSQL(createTableCollectFormInstance());
        db.execSQL(createTableCollectFormInstanceMediaFile());
        db.execSQL(createTableSettings());

        // DBv2
        db.execSQL(alterTableCollectFormInstanceMediaFileAddStatus());
        db.execSQL(alterTableCollectServerAddChecked());

        // DBv3
        db.execSQL(alterTableCollectBlankFormAddUpdated());

        // DBv4
        db.execSQL(alterTableCollectFormInstanceAddFormPartStatus());

        // DBv5
        db.execSQL(alterTableMediaFileAddHash());

        // DBv6
        db.execSQL(createTableTellaUploadServer());

        // DBv7
        db.execSQL(createTableMediaFileUploads());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1:
                db.execSQL(alterTableCollectFormInstanceMediaFileAddStatus());
                db.execSQL(alterTableCollectServerAddChecked());

            case 2:
                db.execSQL(alterTableCollectBlankFormAddUpdated());

            case 3:
                db.execSQL(alterTableCollectFormInstanceAddFormPartStatus());

            case 4:
                db.execSQL(alterTableMediaFileAddHash());

            case 5:
                db.execSQL(createTableTellaUploadServer());

            case 6:
                db.execSQL(createTableMediaFileUploads());
        }
    }

    private String createTableTrustedPerson() {
        return "CREATE TABLE " + sq(D.T_TRUSTED_PERSON) + "(" +
                sq(D.C_ID) + D.INTEGER + " PRIMARY KEY AUTOINCREMENT, " +
                sq(D.C_MAIL) + D.TEXT + " , " +
                sq(D.C_PHONE) + D.TEXT + " , " +
                sq(D.C_NAME) + D.TEXT + ");";
    }

    private String createTableCollectServer() {
        return "CREATE TABLE " + sq(D.T_COLLECT_SERVER) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_NAME, D.TEXT) + " , " +
                cddl(D.C_URL, D.TEXT) + " , " +
                cddl(D.C_USERNAME, D.TEXT) + " , " +
                cddl(D.C_PASSWORD, D.TEXT) +
                ");";
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

    private String createTableCollectBlankForm() {
        return "CREATE TABLE " + sq(D.T_COLLECT_BLANK_FORM) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_COLLECT_SERVER_ID, D.INTEGER, true) + " , " +
                cddl(D.C_FORM_ID, D.TEXT, true) + " , " +
                cddl(D.C_VERSION, D.TEXT, true) + " , " +
                cddl(D.C_HASH, D.TEXT, true) + " , " +
                cddl(D.C_NAME, D.TEXT, true) + " , " +
                cddl(D.C_DOWNLOAD_URL, D.TEXT) + " , " +
                cddl(D.C_FORM_DEF, D.BLOB) + " , " +
                cddl(D.C_DOWNLOADED, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_FAVORITE, D.INTEGER, true) + " DEFAULT 0 , " +
                "FOREIGN KEY(" + sq(D.C_COLLECT_SERVER_ID) + ") REFERENCES " +
                sq(D.T_COLLECT_SERVER) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE, " +
                "UNIQUE(" + sq(D.C_FORM_ID) + ") ON CONFLICT REPLACE" +
                ");";
    }

    private String createTableCollectFormInstance() {
        return "CREATE TABLE " + sq(D.T_COLLECT_FORM_INSTANCE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_COLLECT_SERVER_ID, D.INTEGER, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " , " +
                cddl(D.C_UPDATED, D.INTEGER, true) + " , " +
                cddl(D.C_FORM_ID, D.TEXT, true) + " , " +
                cddl(D.C_VERSION, D.TEXT, true) + " , " +
                cddl(D.C_FORM_NAME, D.TEXT, true) + " , " +
                cddl(D.C_INSTANCE_NAME, D.TEXT, true) + " , " +
                cddl(D.C_FORM_DEF, D.BLOB) + " , " +
                "FOREIGN KEY(" + sq(D.C_COLLECT_SERVER_ID) + ") REFERENCES " +
                sq(D.T_COLLECT_SERVER) + "(" + sq(D.C_ID) + ") ON DELETE RESTRICT" +
                ");";
    }

    private String createTableCollectFormInstanceMediaFile() {
        return "CREATE TABLE " + sq(D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_COLLECT_FORM_INSTANCE_ID, D.INTEGER, true) + " , " +
                cddl(D.C_MEDIA_FILE_ID, D.INTEGER, true) + " , " +
                "FOREIGN KEY(" + sq(D.C_COLLECT_FORM_INSTANCE_ID) + ") REFERENCES " +
                sq(D.T_COLLECT_FORM_INSTANCE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "FOREIGN KEY(" + sq(D.C_MEDIA_FILE_ID) + ") REFERENCES " +
                sq(D.T_MEDIA_FILE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "UNIQUE(" + sq(D.C_COLLECT_FORM_INSTANCE_ID) + ", " + sq(D.C_MEDIA_FILE_ID) + ") ON CONFLICT IGNORE" +
                ");";
    }

    private String createTableSettings() {
        return "CREATE TABLE " + sq(D.T_SETTINGS) + "(" +
                cddl(D.C_NAME, D.TEXT) + " PRIMARY KEY, " +
                cddl(D.C_INT_VALUE, D.INTEGER) + " , " +
                cddl(D.C_TEXT_VALUE, D.TEXT) +
                " );";
    }

    private String createTableMediaFileUploads() {
        return "CREATE TABLE " + sq(D.T_MEDIA_FILE_UPLOADS) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_MEDIA_FILE_ID, D.INTEGER, false) + " , " +
                cddl(D.C_UPDATED, D.INTEGER, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " , " +
                cddl(D.C_SIZE, D.INTEGER) + " , " +
                cddl(D.C_UPLOADED, D.INTEGER) + " , " +
                cddl(D.C_SET, D.INTEGER) + " , " +
                "FOREIGN KEY(" + sq(D.C_MEDIA_FILE_ID) + ") REFERENCES " +
                sq(D.T_MEDIA_FILE) + "(" + sq(D.C_ID) + ") ON DELETE SET NULL" +
                ");";
    }

    private String alterTableCollectFormInstanceMediaFileAddStatus() {
        return "ALTER TABLE " + sq(D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE) + " ADD COLUMN " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0";
    }

    private String alterTableCollectServerAddChecked() {
        return "ALTER TABLE " + sq(D.T_COLLECT_SERVER) + " ADD COLUMN " +
                cddl(D.C_CHECKED, D.INTEGER, true) + " DEFAULT 0";
    }

    private String alterTableCollectBlankFormAddUpdated() {
        return "ALTER TABLE " + sq(D.T_COLLECT_BLANK_FORM) + " ADD COLUMN " +
                cddl(D.C_UPDATED, D.INTEGER, true) + " DEFAULT 0";
    }

    private String alterTableCollectFormInstanceAddFormPartStatus() {
        return "ALTER TABLE " + sq(D.T_COLLECT_FORM_INSTANCE) + " ADD COLUMN " +
                cddl(D.C_FORM_PART_STATUS, D.INTEGER, true) + " DEFAULT 0";
    }

    private String alterTableMediaFileAddHash() {
        return "ALTER TABLE " + sq(D.T_MEDIA_FILE) + " ADD COLUMN " +
                cddl(D.C_HASH, D.INTEGER, false);
    }


    private String createTableTellaUploadServer() {
        return "CREATE TABLE " + sq(D.T_TELLA_UPLOAD_SERVER) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_NAME, D.TEXT) + " , " +
                cddl(D.C_URL, D.TEXT) + " , " +
                cddl(D.C_USERNAME, D.TEXT) + " , " +
                cddl(D.C_PASSWORD, D.TEXT) + " , " +
                cddl(D.C_CHECKED, D.INTEGER, true) + " DEFAULT 0" +
                ");";
    }

    private static String objQuote(String str) {
        return OBJ_QUOTE + str + OBJ_QUOTE;
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
}
