package rs.readahead.washington.mobile.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;


import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.CompletableTransformer;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer;
import rs.readahead.washington.mobile.domain.entity.resources.Resource;
import rs.readahead.washington.mobile.domain.exception.NotFountException;
import rs.readahead.washington.mobile.domain.repository.resources.ITellaResourcesRepository;
import rs.readahead.washington.mobile.util.Util;
import timber.log.Timber;

public class ResourceDataSource implements ITellaResourcesRepository {

    private static ResourceDataSource dataSource;
    private final SQLiteDatabase database;

    final private SingleTransformer schedulersTransformer =
            observable -> observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());

    final private CompletableTransformer schedulersCompletableTransformer =
            observable -> observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());

    private ResourceDataSource(Context context, byte[] key) {
        DatabaseSecret databaseSecret = new DatabaseSecret(key);
        WashingtonSQLiteOpenHelper sqLiteOpenHelper = new WashingtonSQLiteOpenHelper(context, databaseSecret);
        database = sqLiteOpenHelper.getWritableDatabase();
    }

    public static synchronized ResourceDataSource getInstance(Context context, byte[] key) {
        if (dataSource == null) {
            dataSource = new ResourceDataSource(context.getApplicationContext(), key);
        }

        return dataSource;
    }

    private <T> SingleTransformer<T, T> applySchedulers() {
        //noinspection unchecked
        return (SingleTransformer<T, T>) schedulersTransformer;
    }

    private CompletableTransformer applyCompletableSchedulers() {
        return schedulersCompletableTransformer;
    }

    @NonNull
    @Override
    public Single<List<Resource>> listResources() {
        return Single.fromCallable(() -> dataSource.listResourcesDB())
                .compose(applySchedulers());
    }

    @NonNull
    @Override
    public Completable removeTellaServerAndResources(final long id) {
        return Completable.fromCallable((Callable<Void>) () -> {
            dataSource.deleteTellaServerAndResourcesDB(id);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @NonNull
    @Override
    public Single<String> deleteResource(@NonNull final Resource resource) {
        return Single.fromCallable(() -> deleteResourceFromDB(resource))
                .compose(applySchedulers());
    }

    @NonNull
    @Override
    public Single<List<TellaReportServer>> listTellaUploadServers() {
        return Single.fromCallable(() -> dataSource.getTUServers())
                .compose(applySchedulers());
    }

    @NonNull
    @Override
    public Single<TellaReportServer> getTellaUploadServer(final long id) {
        return Single.fromCallable(() -> getTUServer(id))
                .compose(applySchedulers());
    }

    @NonNull
    public Single<Resource> saveResource(@NonNull Resource instance) {
        return Single.fromCallable(() -> updateResourceInstance(instance)).compose(applySchedulers());
    }

    private Resource updateResourceInstance(Resource instance) {
        try {
            ContentValues values = new ContentValues();

            if (instance.getResourceId() > 0) {
                values.put(D.C_ID, instance.getResourceId());
            }

            values.put(D.C_RESOURCES_ID, instance.getId());
            values.put(D.C_SERVER_ID, instance.getServerId());
            values.put(D.C_RESOURCES_TITLE, instance.getTitle());
            values.put(D.C_RESOURCES_FILE_NAME, instance.getFileName());
            values.put(D.C_RESOURCES_SIZE, instance.getSize());
            values.put(D.C_RESOURCES_CREATED, instance.getCreatedAt());
            values.put(D.C_RESOURCES_SAVED, Util.currentTimestamp());
            values.put(D.C_RESOURCES_PROJECT, instance.getProject());
            values.put(D.C_RESOURCES_FILE_ID, instance.getFileId());
            database.beginTransaction();

            // insert/update resource instance
            long id = database.insertWithOnConflict(
                    D.T_RESOURCES,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE);
            instance.setResourceId(id);
            database.setTransactionSuccessful();
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            database.endTransaction();
        }
        return instance;
    }

    private List<Resource> listResourcesDB() {
        Cursor cursor = null;
        List<Resource> resources = new ArrayList<>();

        try {
            cursor = database.query(
                    D.T_RESOURCES,
                    new String[]{D.C_ID,
                            D.C_SERVER_ID,
                            D.C_RESOURCES_ID,
                            D.C_RESOURCES_TITLE,
                            D.C_RESOURCES_FILE_NAME,
                            D.C_RESOURCES_SIZE,
                            D.C_RESOURCES_CREATED,
                            D.C_RESOURCES_SAVED,
                            D.C_RESOURCES_PROJECT,
                            D.C_RESOURCES_FILE_ID
                    },
                    null,
                    null,
                    null, null,
                    D.C_ID + " ASC",
                    null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                Resource resource = cursorToResource(cursor);
                resources.add(resource);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return resources;
    }

    private TellaReportServer getTUServer(long id) {

        try (
                Cursor cursor = database.query(
                        D.T_TELLA_UPLOAD_SERVER,
                        new String[]{
                                D.C_ID,
                                D.C_NAME,
                                D.C_URL,
                                D.C_USERNAME,
                                D.C_PASSWORD,
                                D.C_CHECKED,
                                D.C_ACCESS_TOKEN,
                                D.C_AUTO_UPLOAD,
                                D.C_AUTO_DELETE,
                                D.C_ACTIVATED_METADATA,
                                D.C_BACKGROUND_UPLOAD,
                                D.C_PROJECT_SLUG,
                                D.C_PROJECT_NAME,
                                D.C_PROJECT_ID},
                        D.C_ID + "= ?",
                        new String[]{Long.toString(id)},
                        null, null, null, null)) {

            if (cursor.moveToFirst()) {
                return cursorToTellaUploadServer(cursor);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        }

        return TellaReportServer.NONE;
    }

    private void deleteTellaServerAndResourcesDB(long id) {
        database.delete(D.T_TELLA_UPLOAD_SERVER, D.C_ID + " = ?", new String[]{Long.toString(id)});
        database.delete(D.T_RESOURCES, D.C_SERVER_ID + " = ?", new String[]{Long.toString(id)});
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private String deleteResourceFromDB(Resource resource) throws NotFountException {
        int count = database.delete(D.T_RESOURCES, D.C_ID + " = ?", new String[]{Long.toString(resource.getResourceId())});

        if (count != 1) {
            throw new NotFountException();
        }
        return resource.getFileId();
    }

    private List<TellaReportServer> getTUServers() {
        Cursor cursor = null;
        List<TellaReportServer> servers = new ArrayList<>();

        try {
            cursor = database.query(
                    D.T_TELLA_UPLOAD_SERVER,
                    new String[]{D.C_ID,
                            D.C_NAME,
                            D.C_URL,
                            D.C_USERNAME,
                            D.C_PASSWORD,
                            D.C_CHECKED,
                            D.C_ACCESS_TOKEN,
                            D.C_AUTO_UPLOAD,
                            D.C_AUTO_DELETE,
                            D.C_ACTIVATED_METADATA,
                            D.C_BACKGROUND_UPLOAD,
                            D.C_PROJECT_SLUG,
                            D.C_PROJECT_NAME,
                            D.C_PROJECT_ID
                    },
                    null,
                    null,
                    null, null,
                    D.C_ID + " ASC",
                    null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                TellaReportServer tuServer = cursorToTellaUploadServer(cursor);
                servers.add(tuServer);
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

    private Resource cursorToResource(Cursor cursor) {

        long ResourceId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID));
        long ServerId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_SERVER_ID));
        String Id = cursor.getString(cursor.getColumnIndexOrThrow(D.C_RESOURCES_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(D.C_RESOURCES_TITLE));
        String fileName = cursor.getString(cursor.getColumnIndexOrThrow(D.C_RESOURCES_FILE_NAME));
        long size = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_RESOURCES_SIZE));
        String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(D.C_RESOURCES_CREATED));
        Long savedAt = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_RESOURCES_SAVED));
        String project = cursor.getString(cursor.getColumnIndexOrThrow(D.C_RESOURCES_PROJECT));
        String fileId = cursor.getString(cursor.getColumnIndexOrThrow(D.C_RESOURCES_FILE_ID));

        return new Resource(ResourceId, ServerId, Id, title, fileName, size, createdAt, savedAt, project, fileId);
    }

    private TellaReportServer cursorToTellaUploadServer(Cursor cursor) {
        TellaReportServer server = new TellaReportServer();
        server.setId(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID)));
        server.setName(cursor.getString(cursor.getColumnIndexOrThrow(D.C_NAME)));
        server.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(D.C_URL)));
        server.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(D.C_USERNAME)));
        server.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(D.C_PASSWORD)));
        server.setChecked(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_CHECKED)) > 0);
        server.setAccessToken(cursor.getString(cursor.getColumnIndexOrThrow(D.C_ACCESS_TOKEN)));
        server.setActivatedBackgroundUpload(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_BACKGROUND_UPLOAD)) > 0);
        server.setAutoUpload(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_AUTO_UPLOAD)) > 0);
        server.setAutoDelete(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_AUTO_DELETE)) > 0);
        server.setActivatedMetadata(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_ACTIVATED_METADATA)) > 0);
        server.setProjectSlug(cursor.getString(cursor.getColumnIndexOrThrow(D.C_PROJECT_SLUG)));
        server.setProjectId(cursor.getString(cursor.getColumnIndexOrThrow(D.C_PROJECT_ID)));
        server.setProjectName(cursor.getString(cursor.getColumnIndexOrThrow(D.C_PROJECT_NAME)));

        return server;
    }
}
