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
        ContentValues values = new ContentValues();
        values.put(D.C_USERNAME, server.getUsername());
        values.put(D.C_GOOGLE_DRIVE_FOLDER_ID, server.getFolderId());
        values.put(D.C_GOOGLE_DRIVE_FOLDER_NAME, server.getFolderName());
        values.put(D.C_GOOGLE_DRIVE_SERVER_NAME, server.getName());
        server.setId(database.insert(D.T_GOOGLE_DRIVE, null, values));

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
                            D.C_USERNAME,
                            D.C_GOOGLE_DRIVE_FOLDER_ID,
                            D.C_GOOGLE_DRIVE_FOLDER_NAME,
                            D.C_GOOGLE_DRIVE_SERVER_NAME

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

    private void removeGoogleDriveServer(long id) {
        database.delete(D.T_GOOGLE_DRIVE, D.C_ID + " = ?", new String[]{Long.toString(id)});
    }

    private GoogleDriveServer cursorToGoogleDriveServer(Cursor cursor) {

        long googleDriveId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID));
        String folderId = cursor.getString(cursor.getColumnIndexOrThrow(D.C_GOOGLE_DRIVE_FOLDER_ID));
        String folderName = cursor.getString(cursor.getColumnIndexOrThrow(D.C_GOOGLE_DRIVE_FOLDER_NAME));
        String serverName = cursor.getString(cursor.getColumnIndexOrThrow(D.C_GOOGLE_DRIVE_SERVER_NAME));
        String userName = cursor.getString(cursor.getColumnIndexOrThrow(D.C_USERNAME));
        GoogleDriveServer server = new GoogleDriveServer(googleDriveId, folderId, folderName);
        server.setName(serverName);
        server.setUsername(userName);

        return server;
    }
}