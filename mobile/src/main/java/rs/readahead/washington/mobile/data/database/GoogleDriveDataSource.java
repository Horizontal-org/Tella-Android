package rs.readahead.washington.mobile.data.database;

import android.content.ContentValues;
import android.content.Context;

import androidx.annotation.NonNull;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.domain.entity.googledrive.Folder;
import rs.readahead.washington.mobile.domain.repository.googledrive.IGoogleDriveRepository;
import timber.log.Timber;

public class GoogleDriveDataSource implements IGoogleDriveRepository {

    private static GoogleDriveDataSource dataSource;
    private final SQLiteDatabase database;

    private GoogleDriveDataSource(Context context, byte[] key) {
        System.loadLibrary("sqlcipher");
        WashingtonSQLiteOpenHelper sqLiteOpenHelper = new WashingtonSQLiteOpenHelper(context,key);
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
    public Single<Folder> saveFolder(@NonNull Folder instance) {
        return Single.fromCallable(() -> updateResourceInstance(instance)).compose(applySchedulers());
    }

    private Folder updateResourceInstance(Folder instance) {
        try {
            ContentValues values = new ContentValues();

//            if (instance.getId() > 0) {
//                values.put(D.C_ID, instance.getResourceId());
//            }

            values.put(D.T_GOOGLE_DRIVE_FOLDER_ID, instance.getFolderId());
            values.put(D.T_GOOGLE_DRIVE_FOLDER_NAME, instance.getName());
            database.beginTransaction();

            // insert/update resource instance
            long id = database.insertWithOnConflict(
                    D.T_GOOGLE_DRIVE,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE);
            instance.setFolderId(String.valueOf(id));
            database.setTransactionSuccessful();
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            database.endTransaction();
        }
        return instance;
    }


    private <T> SingleTransformer<T, T> applySchedulers() {
        //noinspection unchecked
        return (SingleTransformer<T, T>) schedulersTransformer;
    }

    final private SingleTransformer schedulersTransformer =
            observable -> observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());

}