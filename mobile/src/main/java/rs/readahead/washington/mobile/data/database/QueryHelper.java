package rs.readahead.washington.mobile.data.database;

import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class QueryHelper {

    public static List<String> getListFromQuery(SupportSQLiteDatabase database, String query){
        Cursor cursor = database.query(query, new String[]{});
        List<String> results = new ArrayList<>();
        if(cursor != null){
            while(cursor.moveToNext()){
                results.add(cursor.getString(0));
            }
            cursor.close();
        }
        return results;
    }

    public static String singleValueFromQuery(SupportSQLiteDatabase database, String query){
        Cursor cursor = database.query(query, new String[]{});
        String value = "";
        if(cursor != null){
            cursor.moveToFirst();
            value = cursor.getString(0);
            cursor.close();
        }
        return value;
    }

    public static int singleIntegerValueFromQuery(SupportSQLiteDatabase database, String query){
        Cursor cursor = database.query(query, new String[]{});
        int value = 0;
        if(cursor != null){
            cursor.moveToFirst();
            value = cursor.getInt(0);
            cursor.close();
        }
        return value;
    }
}