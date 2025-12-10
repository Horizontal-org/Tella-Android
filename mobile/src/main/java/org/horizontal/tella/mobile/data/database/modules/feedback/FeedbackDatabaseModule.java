package org.horizontal.tella.mobile.data.database.modules.feedback;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.horizontal.tella.mobile.data.database.D;
import org.horizontal.tella.mobile.data.database.modules.DatabaseModule;

import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.cddl;
import static org.horizontal.tella.mobile.data.database.HorizontalSQLiteOpenHelper.sq;

/**
 * Database module for Feedback functionality.
 * Manages tables for:
 * - Feedback
 */
public class FeedbackDatabaseModule implements DatabaseModule {
    private static final int MIN_DB_VERSION = 11;

    @Override
    public String getModuleName() {
        return "Feedback";
    }

    @Override
    public int getMinDatabaseVersion() {
        return MIN_DB_VERSION;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // DBv11
        db.execSQL(createTableFeedback());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 11) {
            db.execSQL(createTableFeedback());
        }
    }

    private String createTableFeedback() {
        return "CREATE TABLE " + sq(D.T_FEEDBACK) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_DESCRIPTION_TEXT, D.TEXT, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " DEFAULT 0," +
                cddl(D.C_UPDATED, D.INTEGER, true) + " DEFAULT 0  " +
                ");";
    }
}

