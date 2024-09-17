package rs.readahead.washington.mobile.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer;
import rs.readahead.washington.mobile.domain.repository.googledrive.IGoogleDriveRepository;
import timber.log.Timber;

public class GoogleDriveDataSource implements IGoogleDriveRepository {

    private static GoogleDriveDataSource dataSource;
    private final SQLiteDatabase database;

    private GoogleDriveDataSource(Context context, byte[] key) {
        System.loadLibrary("sqlcipher");
        WashingtonSQLiteOpenHelper sqLiteOpenHelper = new WashingtonSQLiteOpenHelper(context, key);
        database = sqLiteOpenHelper.getWritableDatabase();
    }

    public static synchronized GoogleDriveDataSource getInstance(Context context, byte[] key) {
        if (dataSource == null) {
            dataSource = new GoogleDriveDataSource(context.getApplicationContext(), key);
        }

        return dataSource;
    }

    @NonNull
    @Override
    public Single<GoogleDriveServer> saveGoogleDriveServer(@NonNull GoogleDriveServer server) {
        return Single.fromCallable(() -> updateGoogleDriveServer(server)).compose(applySchedulers());
    }

    private GoogleDriveServer updateGoogleDriveServer(GoogleDriveServer server) {
        try {
            ContentValues values = new ContentValues();

            values.put(D.T_GOOGLE_DRIVE_FOLDER_ID, server.getFolderId());
            values.put(D.T_GOOGLE_DRIVE_FOLDER_NAME, server.getFolderName());
            values.put(D.T_GOOGLE_DRIVE_SERVER_NAME, server.getName());

            database.beginTransaction();

            // insert/update resource instance
            long id = database.insertWithOnConflict(
                    D.T_GOOGLE_DRIVE,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE);
            server.setFolderId(String.valueOf(id));
            database.setTransactionSuccessful();
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            database.endTransaction();
        }
        return server;
    }


    private <T> SingleTransformer<T, T> applySchedulers() {
        //noinspection unchecked
        return (SingleTransformer<T, T>) schedulersTransformer;
    }

    final private SingleTransformer schedulersTransformer =
            observable -> observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());

    @NonNull
    @Override
    public Single<List<GoogleDriveServer>> listGoogleDriveServers() {
        return Single.fromCallable(() -> dataSource.getListGoogleDriveServers())
                .compose(applySchedulers());
    }

    private List<GoogleDriveServer> getListGoogleDriveServers() {
        Cursor cursor = null;
        List<GoogleDriveServer> servers = new ArrayList<>();

        try {
            cursor = database.query(
                    D.T_GOOGLE_DRIVE,
                    new String[]{D.C_ID,
                            D.C_SERVER_ID,
                            D.T_GOOGLE_DRIVE_FOLDER_ID,
                            D.T_GOOGLE_DRIVE_FOLDER_NAME,
                            D.T_GOOGLE_DRIVE_SERVER_NAME

                    },
                    null,
                    null,
                    null, null,
                    D.C_ID + " ASC",
                    null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                GoogleDriveServer server = cursorToGoogleDriveServer(cursor);
                servers.add(server);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return servers;
    }


    private GoogleDriveServer cursorToGoogleDriveServer(Cursor cursor) {

        long googleDriveId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID));
        String folderId = cursor.getString(cursor.getColumnIndexOrThrow(D.T_GOOGLE_DRIVE_FOLDER_ID));
        String folderName = cursor.getString(cursor.getColumnIndexOrThrow(D.T_GOOGLE_DRIVE_FOLDER_NAME));
        String serverName = cursor.getString(cursor.getColumnIndexOrThrow(D.T_GOOGLE_DRIVE_SERVER_NAME));

        GoogleDriveServer server = new GoogleDriveServer(googleDriveId, folderId, folderName);
        server.setName(serverName);

        return server;
    }
}