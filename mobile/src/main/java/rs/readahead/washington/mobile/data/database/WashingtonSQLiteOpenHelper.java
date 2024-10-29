package rs.readahead.washington.mobile.data.database;

import android.content.Context;

import static rs.readahead.washington.mobile.data.sharedpref.Preferences.isAlreadyMigratedMainDB;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

class WashingtonSQLiteOpenHelper extends CipherOpenHelper {
    private static final String OBJ_QUOTE = "`";

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);

        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    WashingtonSQLiteOpenHelper(Context context, byte[] password) {
        super(context, password);
        if (!isAlreadyMigratedMainDB()) {
            migrateSqlCipher3To4IfNeeded(context, password);
        }
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


    private String createTableCollectServer() {
        return "CREATE TABLE " + sq(D.T_COLLECT_SERVER) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_NAME, D.TEXT) + " , " +
                cddl(D.C_URL, D.TEXT) + " , " +
                cddl(D.C_USERNAME, D.TEXT) + " , " +
                cddl(D.C_PASSWORD, D.TEXT) +
                ");";
    }

    private String createTableUwaziServer() {
        return "CREATE TABLE " + sq(D.T_UWAZI_SERVER) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_NAME, D.TEXT) + " , " +
                cddl(D.C_URL, D.TEXT) + " , " +
                cddl(D.C_CONNECT_COOKIES, D.TEXT) + " , " +
                cddl(D.C_LOCALE_COOKIES, D.TEXT) + " , " +
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

    private String createTableCollectFormInstanceVaultFile() {
        return "CREATE TABLE " + sq(D.T_COLLECT_FORM_INSTANCE_VAULT_FILE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_COLLECT_FORM_INSTANCE_ID, D.INTEGER, true) + " , " +
                cddl(D.C_VAULT_FILE_ID, D.TEXT, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0," +
                "FOREIGN KEY(" + sq(D.C_COLLECT_FORM_INSTANCE_ID) + ") REFERENCES " +
                sq(D.T_COLLECT_FORM_INSTANCE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "UNIQUE(" + sq(D.C_COLLECT_FORM_INSTANCE_ID) + ", " + sq(D.C_VAULT_FILE_ID) + ") ON CONFLICT IGNORE" +
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

    private String createTableReportFileUploads() {
        return "CREATE TABLE " + sq(D.T_REPORT_FILES_UPLOAD) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_REPORT_INSTANCE_ID, D.INTEGER, false) + " UNIQUE, " +
                cddl(D.C_UPDATED, D.INTEGER, true) + " , " +
                cddl(D.C_CREATED, D.INTEGER, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " , " +
                cddl(D.C_SIZE, D.INTEGER) + " , " +
                cddl(D.C_UPLOADED, D.INTEGER) + " DEFAULT 0, " +
                cddl(D.C_RETRY_COUNT, D.INTEGER) + " DEFAULT 0, " +
                cddl(D.C_SET, D.INTEGER) + " , " +
                "FOREIGN KEY(" + sq(D.C_REPORT_INSTANCE_ID) + ") REFERENCES " +
                sq(D.T_REPORT_FORM_INSTANCE) + "(" + sq(D.C_ID) + ") ON DELETE SET NULL" +
                ");";
    }

    private String createTableResources() {
        return "CREATE TABLE " + sq(D.T_RESOURCES) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_SERVER_ID, D.INTEGER) + " , " +
                cddl(D.C_RESOURCES_ID, D.TEXT, false) + " UNIQUE, " +
                cddl(D.C_RESOURCES_TITLE, D.TEXT, false) + " , " +
                cddl(D.C_RESOURCES_FILE_NAME, D.TEXT, true) + " UNIQUE, " +
                cddl(D.C_RESOURCES_SIZE, D.INTEGER) + " , " +
                cddl(D.C_RESOURCES_CREATED, D.TEXT, false) + " , " +
                cddl(D.C_RESOURCES_SAVED, D.INTEGER, false) + " , " +
                cddl(D.C_RESOURCES_PROJECT, D.TEXT, false) + " , " +
                cddl(D.C_RESOURCES_FILE_ID, D.TEXT, false) +
                ");";
    }

    private String createTableGoogleDrive() {
        return "CREATE TABLE " + sq(D.T_GOOGLE_DRIVE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_GOOGLE_DRIVE_FOLDER_ID, D.TEXT, true) + " UNIQUE, " +
                cddl(D.C_GOOGLE_DRIVE_FOLDER_NAME, D.TEXT, true) + " , " +
                cddl(D.C_NAME, D.TEXT) + " , " +
                cddl(D.C_USERNAME, D.TEXT, true) + " , " +
                cddl(D.C_GOOGLE_DRIVE_SERVER_NAME, D.TEXT, true) +
                ");";
    }

    private String createTableNextCloud() {
        return "CREATE TABLE " + sq(D.T_NEXT_CLOUD) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_NEXT_CLOUD_FOLDER_ID, D.TEXT, true) + " UNIQUE, " +
                cddl(D.C_NEXT_CLOUD_FOLDER_NAME, D.TEXT, true) + " , " +
                cddl(D.C_NEXT_CLOUD_USER_ID, D.TEXT, true) + " , " +
                cddl(D.C_PASSWORD, D.TEXT, true) + " , " +
                cddl(D.C_NAME, D.TEXT) + " , " +
                cddl(D.C_USERNAME, D.TEXT, true) + " , " +
                cddl(D.C_NEXT_CLOUD_SERVER_NAME, D.TEXT, true) +
                ");";
    }

    private String alterTableMediaFileUploadsAddManual() {
        return "ALTER TABLE " + sq(D.T_MEDIA_FILE_UPLOAD) + " ADD COLUMN " +
                cddl(D.C_MANUAL_UPLOAD, D.INTEGER, true) + " DEFAULT 0";
    }

    private String alterTableMediaFileUploadsAddServer() {
        return "ALTER TABLE " + sq(D.T_MEDIA_FILE_UPLOAD) + " ADD COLUMN " +
                cddl(D.C_SERVER_ID, D.INTEGER, true) + " DEFAULT 0";
    }

    private String alterTableTellaUploadServerAddAccessToken() {
        return "ALTER TABLE " + sq(D.T_TELLA_UPLOAD_SERVER) + " ADD COLUMN " +
                cddl(D.C_ACCESS_TOKEN, D.TEXT);
    }

    private String alterTableTellaUploadServerAddProjectName() {
        return "ALTER TABLE " + sq(D.T_TELLA_UPLOAD_SERVER) + " ADD COLUMN " +
                cddl(D.C_PROJECT_NAME, D.TEXT);
    }

    private String alterTableTellaUploadServerAddProjectID() {
        return "ALTER TABLE " + sq(D.T_TELLA_UPLOAD_SERVER) + " ADD COLUMN " +
                cddl(D.C_PROJECT_ID, D.TEXT);
    }

    private String alterTableTellaUploadServerAddProjectSlug() {
        return "ALTER TABLE " + sq(D.T_TELLA_UPLOAD_SERVER) + " ADD COLUMN " +
                cddl(D.C_PROJECT_SLUG, D.TEXT);
    }

    private String alterTableTellaUploadServerAddMetatData() {
        return "ALTER TABLE " + sq(D.T_TELLA_UPLOAD_SERVER) + " ADD COLUMN " +
                cddl(D.C_ACTIVATED_METADATA, D.INTEGER, true) + " DEFAULT 0";
    }

    private String alterTableTellaUploadServerAddBackgourndUpload() {
        return "ALTER TABLE " + sq(D.T_TELLA_UPLOAD_SERVER) + " ADD COLUMN " +
                cddl(D.C_BACKGROUND_UPLOAD, D.INTEGER, true) + " DEFAULT 0";
    }

    private String alterTableTellaUploadServerAddAutoDelete() {
        return "ALTER TABLE " + sq(D.T_TELLA_UPLOAD_SERVER) + " ADD COLUMN " +
                cddl(D.C_AUTO_DELETE, D.INTEGER, true) + " DEFAULT 0";
    }

    private String alterTableTellaUploadServerAddAutoUpload() {
        return "ALTER TABLE " + sq(D.T_TELLA_UPLOAD_SERVER) + " ADD COLUMN " +
                cddl(D.C_AUTO_UPLOAD, D.INTEGER, true) + " DEFAULT 0";
    }

    private String alterTableMediaFileUploadsAddMetadata() {
        return "ALTER TABLE " + sq(D.T_MEDIA_FILE_UPLOAD) + " ADD COLUMN " +
                cddl(D.C_INCLUDE_METADATA, D.INTEGER, true) + " DEFAULT 0";
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

    private String alterTableReportFormInstanceAddCurrentUpload() {
        return "ALTER TABLE " + sq(D.T_REPORT_FORM_INSTANCE) + " ADD COLUMN " +
                cddl(D.C_CURRENT_UPLOAD, D.INTEGER, true) + " DEFAULT 0";
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

    private String createTableCollectBlankTemplateUwazi() {
        return "CREATE TABLE " + sq(D.T_UWAZI_BLANK_TEMPLATES) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_UWAZI_SERVER_ID, D.INTEGER, true) + " , " +
                cddl(D.C_DOWNLOAD_URL, D.TEXT) + " , " +
                cddl(D.C_TEMPLATE_ENTITY, D.TEXT, true) + " , " +
                cddl(D.C_DOWNLOADED, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_FAVORITE, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_UPDATED, D.INTEGER, true) + " DEFAULT 0 , " +
                "FOREIGN KEY(" + sq(D.C_UWAZI_SERVER_ID) + ") REFERENCES " +
                sq(D.T_UWAZI_SERVER) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE, " +
                "UNIQUE(" + sq(D.C_ID) + ") ON CONFLICT REPLACE" +
                ");";
    }

    private String createTableCollectEntityUwazi() {
        return "CREATE TABLE " + sq(D.T_UWAZI_ENTITY_INSTANCES) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_UWAZI_SERVER_ID, D.INTEGER, true) + " , " +
                cddl(D.C_TEMPLATE_ENTITY, D.TEXT, true) + " , " +
                cddl(D.C_METADATA, D.TEXT, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_UPDATED, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_TEMPLATE, D.TEXT, true) + " , " +
                cddl(D.C_TITLE, D.TEXT, true) + " , " +
                cddl(D.C_TYPE, D.TEXT, true) + " , " +
                cddl(D.C_FORM_PART_STATUS, D.INTEGER, true) + " DEFAULT 0 , " +
                "FOREIGN KEY(" + sq(D.C_UWAZI_SERVER_ID) + ") REFERENCES " +
                sq(D.T_UWAZI_SERVER) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE, " +
                "UNIQUE(" + sq(D.C_ID) + ") ON CONFLICT REPLACE" +
                ");";
    }

    private String createTableUwaziEntityInstanceVaultFile() {
        return "CREATE TABLE " + sq(D.T_UWAZI_ENTITY_INSTANCE_VAULT_FILE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_UWAZI_ENTITY_INSTANCE_ID, D.INTEGER, true) + " , " +
                cddl(D.C_VAULT_FILE_ID, D.TEXT, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0," +
                "FOREIGN KEY(" + sq(D.C_UWAZI_ENTITY_INSTANCE_ID) + ") REFERENCES " +
                sq(D.T_UWAZI_ENTITY_INSTANCES) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "UNIQUE(" + sq(D.C_UWAZI_ENTITY_INSTANCE_ID) + ", " + sq(D.C_VAULT_FILE_ID) + ") ON CONFLICT IGNORE" +
                ");";
    }

    private String createTableReportFormInstance() {
        return "CREATE TABLE " + sq(D.T_REPORT_FORM_INSTANCE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_REPORT_SERVER_ID, D.INTEGER, true) + " , " +
                cddl(D.C_REPORT_API_ID, D.TEXT, true) + " , " +
                cddl(D.C_METADATA, D.TEXT, false) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_UPDATED, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_TITLE, D.TEXT, true) + " , " +
                cddl(D.C_DESCRIPTION_TEXT, D.TEXT, true) + " , " +
                cddl(D.C_FORM_PART_STATUS, D.INTEGER, true) + " DEFAULT 0 , " +
                "FOREIGN KEY(" + sq(D.C_REPORT_SERVER_ID) + ") REFERENCES " +
                sq(D.T_TELLA_UPLOAD_SERVER) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE" +
                ");";
    }

    private String createTableReportInstanceVaultFile() {
        return "CREATE TABLE " + sq(D.T_REPORT_INSTANCE_VAULT_FILE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_REPORT_INSTANCE_ID, D.INTEGER, true) + " , " +
                cddl(D.C_VAULT_FILE_ID, D.TEXT, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0," +
                cddl(D.C_UPLOADED_SIZE, D.INTEGER, true) + " DEFAULT 0," +
                "FOREIGN KEY(" + sq(D.C_REPORT_INSTANCE_ID) + ") REFERENCES " +
                sq(D.T_REPORT_FORM_INSTANCE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "UNIQUE(" + sq(D.C_REPORT_INSTANCE_ID) + ", " + sq(D.C_VAULT_FILE_ID) + ") ON CONFLICT IGNORE" +
                ");";
    }

    private String createTableFeedback() {
        return "CREATE TABLE " + sq(D.T_FEEDBACK) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_DESCRIPTION_TEXT, D.TEXT, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0," +
                cddl(D.C_UPDATED, D.INTEGER, true) + " DEFAULT 0  " +
                ");";
    }

    private String createTableGoogleDriveInstanceVaultFile() {
        return "CREATE TABLE " + sq(D.T_GOOGLE_DRIVE_INSTANCE_VAULT_FILE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_REPORT_INSTANCE_ID, D.INTEGER, true) + " , " +
                cddl(D.C_VAULT_FILE_ID, D.TEXT, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0," +
                cddl(D.C_UPLOADED_SIZE, D.INTEGER, true) + " DEFAULT 0," +
                "FOREIGN KEY(" + sq(D.C_REPORT_INSTANCE_ID) + ") REFERENCES " +
                sq(D.T_GOOGLE_DRIVE_FORM_INSTANCE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "UNIQUE(" + sq(D.C_REPORT_INSTANCE_ID) + ", " + sq(D.C_VAULT_FILE_ID) + ") ON CONFLICT IGNORE" +
                ");";
    }

    private String createTableGoogleDriveFormInstance() {
        return "CREATE TABLE " + sq(D.T_GOOGLE_DRIVE_FORM_INSTANCE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_REPORT_SERVER_ID, D.INTEGER, true) + " , " +
                cddl(D.C_REPORT_API_ID, D.TEXT, true) + " , " +
                cddl(D.C_METADATA, D.TEXT, false) + " , " +
                cddl(D.C_CURRENT_UPLOAD, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_UPDATED, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_TITLE, D.TEXT, true) + " , " +
                cddl(D.C_DESCRIPTION_TEXT, D.TEXT, true) + " , " +
                cddl(D.C_FORM_PART_STATUS, D.INTEGER, true) + " DEFAULT 0 , " +
                "FOREIGN KEY(" + sq(D.C_REPORT_SERVER_ID) + ") REFERENCES " +
                sq(D.T_GOOGLE_DRIVE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE" +
                ");";
    }

    private String createTableDropBoxInstanceVaultFile() {
        return "CREATE TABLE " + sq(D.T_DROPBOX_INSTANCE_VAULT_FILE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_REPORT_INSTANCE_ID, D.INTEGER, true) + " , " +
                cddl(D.C_VAULT_FILE_ID, D.TEXT, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0," +
                cddl(D.C_UPLOADED_SIZE, D.INTEGER, true) + " DEFAULT 0," +
                "FOREIGN KEY(" + sq(D.C_REPORT_INSTANCE_ID) + ") REFERENCES " +
                sq(D.T_DROPBOX_FORM_INSTANCE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "UNIQUE(" + sq(D.C_REPORT_INSTANCE_ID) + ", " + sq(D.C_VAULT_FILE_ID) + ") ON CONFLICT IGNORE" +
                ");";
    }

    private String createTableDropBoxFormInstance() {
        return "CREATE TABLE " + sq(D.T_DROPBOX_FORM_INSTANCE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_REPORT_SERVER_ID, D.INTEGER, true) + " , " +
                cddl(D.C_REPORT_API_ID, D.TEXT, true) + " , " +
                cddl(D.C_METADATA, D.TEXT, false) + " , " +
                cddl(D.C_CURRENT_UPLOAD, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_UPDATED, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_TITLE, D.TEXT, true) + " , " +
                cddl(D.C_DESCRIPTION_TEXT, D.TEXT, true) + " , " +
                cddl(D.C_FORM_PART_STATUS, D.INTEGER, true) + " DEFAULT 0 , " +
                "FOREIGN KEY(" + sq(D.C_REPORT_SERVER_ID) + ") REFERENCES " +
                sq(D.T_DROPBOX) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE" +
                ");";
    }

    private String createTableDropBox() {
        return "CREATE TABLE " + sq(D.T_DROPBOX) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_DROPBOX_ACCESS_TOKEN, D.TEXT, true) + " UNIQUE, " +
                cddl(D.C_NAME, D.TEXT) + " , " +
                cddl(D.C_USERNAME, D.TEXT, false) + " , " +
                cddl(D.C_DROPBOX_SERVER_NAME, D.TEXT, true) +
                ");";
    }


    @Override
    public void onCreate(net.zetetic.database.sqlcipher.SQLiteDatabase db) {
        // we have started from version 1

        // DBv1
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

        //DBv8
        db.execSQL(alterTableMediaFileUploadsAddMetadata());
        db.execSQL(alterTableMediaFileUploadsAddManual());
        db.execSQL(alterTableMediaFileUploadsAddServer());
        db.execSQL(createTableUwaziServer());
        db.execSQL(createTableCollectEntityUwazi());
        db.execSQL(createTableCollectBlankTemplateUwazi());
        db.execSQL(createTableCollectFormInstanceVaultFile());
        db.execSQL(createTableUwaziEntityInstanceVaultFile());

        //DBV9
        db.execSQL(alterTableTellaUploadServerAddAccessToken());
        db.execSQL(alterTableTellaUploadServerAddMetatData());
        db.execSQL(alterTableTellaUploadServerAddBackgourndUpload());
        db.execSQL(alterTableTellaUploadServerAddProjectName());
        db.execSQL(alterTableTellaUploadServerAddProjectSlug());
        db.execSQL(alterTableTellaUploadServerAddProjectID());
        db.execSQL(createTableReportFormInstance());
        db.execSQL(createTableReportInstanceVaultFile());
        db.execSQL(createTableReportFileUploads());

        //DBV11
        db.execSQL(alterTableTellaUploadServerAddAutoUpload());
        db.execSQL(alterTableTellaUploadServerAddAutoDelete());
        db.execSQL(alterTableReportFormInstanceAddCurrentUpload());
        db.execSQL(createTableFeedback());

        //DBV12
        db.execSQL(createTableResources());

        //DBV13
        db.execSQL(createTableGoogleDrive());
        db.execSQL(createTableGoogleDriveFormInstance());
        db.execSQL(createTableGoogleDriveInstanceVaultFile());

        //DBV14
        db.execSQL(createTableDropBox());
        db.execSQL(createTableDropBoxFormInstance());
        db.execSQL(createTableDropBoxInstanceVaultFile());

        //DBV15
        db.execSQL(createTableNextCloud());

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1:
                db.execSQL(alterTableCollectFormInstanceMediaFileAddStatus());
                db.execSQL(alterTableCollectServerAddChecked());
                // break;
            case 2:
                db.execSQL(alterTableCollectBlankFormAddUpdated());
                // break;
            case 3:
                db.execSQL(alterTableCollectFormInstanceAddFormPartStatus());
                // break;
            case 4:
                db.execSQL(alterTableMediaFileAddHash());
                //  break;
            case 5:
                db.execSQL(createTableTellaUploadServer());
                // break;
            case 6:
                db.execSQL(createTableMediaFileUploads());
                //break;
            case 7:
                db.execSQL(alterTableMediaFileUploadsAddServer());
                db.execSQL(alterTableMediaFileUploadsAddManual());
                db.execSQL(alterTableMediaFileUploadsAddMetadata());
                // break;
            case 8:
                db.execSQL(createTableUwaziServer());
                db.execSQL(createTableCollectEntityUwazi());
                db.execSQL(createTableCollectBlankTemplateUwazi());
                db.execSQL(createTableCollectFormInstanceVaultFile());
                db.execSQL(createTableUwaziEntityInstanceVaultFile());
                //break;
            case 9:
                db.execSQL(alterTableTellaUploadServerAddAccessToken());
                db.execSQL(alterTableTellaUploadServerAddMetatData());
                db.execSQL(alterTableTellaUploadServerAddBackgourndUpload());
                db.execSQL(alterTableTellaUploadServerAddProjectName());
                db.execSQL(alterTableTellaUploadServerAddProjectSlug());
                db.execSQL(alterTableTellaUploadServerAddProjectID());
                db.execSQL(createTableReportFormInstance());
                db.execSQL(createTableReportInstanceVaultFile());
                db.execSQL(createTableReportFileUploads());
                // break;
            case 10:
                db.execSQL(alterTableTellaUploadServerAddAutoUpload());
                db.execSQL(alterTableTellaUploadServerAddAutoDelete());
                db.execSQL(alterTableReportFormInstanceAddCurrentUpload());
                //  break;
            case 11:
                db.execSQL(createTableFeedback());
                // break;
            case 12:
                db.execSQL(createTableResources());
                // break;
            case 13:
                db.execSQL(createTableGoogleDrive());
                db.execSQL(createTableGoogleDriveFormInstance());
                db.execSQL(createTableGoogleDriveInstanceVaultFile());
            case 14:
                db.execSQL(createTableDropBox());
                db.execSQL(createTableDropBoxFormInstance());
                db.execSQL(createTableDropBoxInstanceVaultFile());
            case 15:
                db.execSQL(createTableNextCloud());

        }
    }

}
