package org.horizontal.tella.mobile.data.database.modules.reports;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.horizontal.tella.mobile.data.database.D;
import org.horizontal.tella.mobile.data.database.modules.DatabaseModule;

import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.cddl;
import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.sq;

/**
 * Database module for Reports functionality.
 * Manages tables for:
 * - Tella upload servers
 * - Report form instances
 * - Report instance vault files
 * - Report file uploads
 */
public class ReportsDatabaseModule implements DatabaseModule {
    private static final int MIN_DB_VERSION = 6;

    @Override
    public String getModuleName() {
        return "Reports";
    }

    @Override
    public int getMinDatabaseVersion() {
        return MIN_DB_VERSION;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // DBv6
        db.execSQL(createTableTellaUploadServer());

        // DBv9
        db.execSQL(alterTableTellaUploadServerAddAccessToken());
        db.execSQL(alterTableTellaUploadServerAddMetatData());
        db.execSQL(alterTableTellaUploadServerAddBackgourndUpload());
        db.execSQL(alterTableTellaUploadServerAddProjectName());
        db.execSQL(alterTableTellaUploadServerAddProjectSlug());
        db.execSQL(alterTableTellaUploadServerAddProjectID());
        db.execSQL(createTableReportFormInstance());
        db.execSQL(createTableReportInstanceVaultFile());
        db.execSQL(createTableReportFileUploads());

        // DBv11
        db.execSQL(alterTableTellaUploadServerAddAutoUpload());
        db.execSQL(alterTableTellaUploadServerAddAutoDelete());
        db.execSQL(alterTableReportFormInstanceAddCurrentUpload());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 6) {
            db.execSQL(createTableTellaUploadServer());
        }
        if (oldVersion < 9) {
            db.execSQL(alterTableTellaUploadServerAddAccessToken());
            db.execSQL(alterTableTellaUploadServerAddMetatData());
            db.execSQL(alterTableTellaUploadServerAddBackgourndUpload());
            db.execSQL(alterTableTellaUploadServerAddProjectName());
            db.execSQL(alterTableTellaUploadServerAddProjectSlug());
            db.execSQL(alterTableTellaUploadServerAddProjectID());
            db.execSQL(createTableReportFormInstance());
            db.execSQL(createTableReportInstanceVaultFile());
            db.execSQL(createTableReportFileUploads());
        }
        if (oldVersion < 11) {
            db.execSQL(alterTableTellaUploadServerAddAutoUpload());
            db.execSQL(alterTableTellaUploadServerAddAutoDelete());
            db.execSQL(alterTableReportFormInstanceAddCurrentUpload());
        }
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

    private String alterTableReportFormInstanceAddCurrentUpload() {
        return "ALTER TABLE " + sq(D.T_REPORT_FORM_INSTANCE) + " ADD COLUMN " +
                cddl(D.C_CURRENT_UPLOAD, D.INTEGER, true) + " DEFAULT 0";
    }
}

