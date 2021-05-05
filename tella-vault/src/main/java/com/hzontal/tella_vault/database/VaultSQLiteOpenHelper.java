package com.hzontal.tella_vault.database;

import android.content.ContentValues;
import android.content.Context;

import com.hzontal.tella_vault.VaultFile;

import net.sqlcipher.database.SQLiteDatabase;


public class VaultSQLiteOpenHelper extends CipherOpenHelper {
    public VaultSQLiteOpenHelper(Context context) {
        super(context, D.DATABASE_NAME, null, D.DATABASE_VERSION);
    }

    private static String objQuote(String str) {
        return "`" + str + "`";
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

    private static String cddl(String columnName, String columnType, boolean notNull, int defaultValue) {
        return objQuote(columnName) + " " + columnType + (notNull ? " NOT NULL " : "") + "DEFAULT " + defaultValue;
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
        createVaultFileTable(db);
        insertRootVaultFile(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1:
        }
    }

    private void createVaultFileTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + sq(D.T_VAULT_FILE) + " (" +
                cddl(D.C_ID, D.TEXT) + " PRIMARY KEY , " +
                cddl(D.C_PARENT_ID, D.INTEGER) + ", " +
                cddl(D.C_NAME, D.TEXT, true) + ", " +
                cddl(D.C_TYPE, D.INTEGER, true) + ", " +
                cddl(D.C_HASH, D.TEXT) + ", " +
                cddl(D.C_METADATA, D.TEXT) + ", " +
                cddl(D.C_MIME_TYPE, D.TEXT) + ", " +
                cddl(D.C_THUMBNAIL, D.BLOB) + " , " +
                cddl(D.C_CREATED, D.INTEGER, true) + " , " +
                cddl(D.C_DURATION, D.INTEGER, true, 0) + ", " +
                cddl(D.C_ANONYMOUS, D.INTEGER, true, 0) + ", " +
                cddl(D.C_SIZE, D.INTEGER, true, 0) + ", " +
                "UNIQUE(" + sq(D.C_PARENT_ID) + ", " + sq(D.C_NAME) + ")" +
                ");");
    }

    private void insertRootVaultFile(SQLiteDatabase db) {
        ContentValues values = new ContentValues();

        values.put(D.C_ID, VaultDataSource.ROOT_ID);
        //  values.put(D.C_UID, VaultDataSource.ROOT_UID);
        values.put(D.C_NAME, "");
        values.put(D.C_TYPE, VaultFile.Type.DIRECTORY.getValue());
        values.put(D.C_CREATED, System.currentTimeMillis());

        db.insert(D.T_VAULT_FILE, null, values);
    }
}
