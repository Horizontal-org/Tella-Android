package rs.readahead.washington.mobile.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.CompletableTransformer;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.domain.entity.googledrive.Config;
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer;
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance;
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstanceBundle;
import rs.readahead.washington.mobile.domain.repository.nextcloud.ITellaNextCloudRepository;
import rs.readahead.washington.mobile.domain.repository.reports.ITellaReportsRepository;
import timber.log.Timber;

public class NextCloudDataSource implements ITellaNextCloudRepository, ITellaReportsRepository {

    private static NextCloudDataSource dataSource;
    private final SQLiteDatabase database;
    private final DataBaseUtils dataBaseUtils;
    @Inject
    Config config;
    final private CompletableTransformer schedulersCompletableTransformer =
            observable -> observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());

    private CompletableTransformer applyCompletableSchedulers() {
        return schedulersCompletableTransformer;
    }

    private NextCloudDataSource(Context context, byte[] key) {
        System.loadLibrary("sqlcipher");
        WashingtonSQLiteOpenHelper sqLiteOpenHelper = new WashingtonSQLiteOpenHelper(context, key);
        database = sqLiteOpenHelper.getWritableDatabase();
        dataBaseUtils = new DataBaseUtils(database);
    }

    public static synchronized NextCloudDataSource getInstance(Context context, byte[] key) {
        if (dataSource == null) {
            dataSource = new NextCloudDataSource(context.getApplicationContext(), key);
        }

        return dataSource;
    }

    @NonNull
    @Override
    public Single<NextCloudServer> saveNextCloudServer(@NonNull NextCloudServer server) {
        return Single.fromCallable(() -> updateNextCloudServer(server)).compose(applySchedulers());
    }

    private NextCloudServer updateNextCloudServer(NextCloudServer server) {
        ContentValues values = new ContentValues();
        values.put(D.C_USERNAME, server.getUsername());
        values.put(D.C_PASSWORD, server.getPassword());
        values.put(D.C_NEXT_CLOUD_USER_ID, server.getUserId());
        values.put(D.C_NEXT_CLOUD_FOLDER_NAME, server.getFolderName());
        values.put(D.C_NEXT_CLOUD_SERVER_NAME, server.getName());
        server.setId(database.insert(D.T_NEXT_CLOUD, null, values));
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
    public Single<List<NextCloudServer>> listNextCloudServers() {
        return Single.fromCallable(() -> dataSource.getListNextCloudServers())
                .compose(applySchedulers());
    }

    private List<NextCloudServer> getListNextCloudServers() {
        Cursor cursor = null;
        List<NextCloudServer> servers = new ArrayList<>();

        try {
            cursor = database.query(
                    D.T_NEXT_CLOUD,
                    new String[]{D.C_ID,
                            D.C_USERNAME,D.C_NEXT_CLOUD_USER_ID,D.C_NEXT_CLOUD_FOLDER_NAME,D.C_NEXT_CLOUD_SERVER_NAME
                    },
                    null,
                    null,
                    null, null,
                    D.C_ID + " ASC",
                    null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                NextCloudServer server = cursorToNextCloudServer(cursor);
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


    private void removeServer(long id) {
        database.delete(D.T_NEXT_CLOUD, D.C_ID + " = ?", new String[]{Long.toString(id)});
    }

    private NextCloudServer cursorToNextCloudServer(Cursor cursor) {
        long nextCloudId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID));
        String serverName = cursor.getString(cursor.getColumnIndexOrThrow(D.C_NEXT_CLOUD_SERVER_NAME));
        String userName = cursor.getString(cursor.getColumnIndexOrThrow(D.C_USERNAME));
        NextCloudServer server = new NextCloudServer(nextCloudId);
        server.setName(serverName);
        server.setUsername(userName);
        return server;
    }

    @NonNull
    @Override
    public Single<ReportInstance> saveInstance(@NonNull ReportInstance instance) {
//        return Single.fromCallable(() -> dataBaseUtils.updateTellaReportsFormInstance(instance, D.T_GOOGLE_DRIVE_FORM_INSTANCE, D.T_GOOGLE_DRIVE_INSTANCE_VAULT_FILE))
//                .compose(applySchedulers());
        return null;
    }


    @NonNull
    @Override
    public Completable deleteReportInstance(long id) {
//        return Completable.fromCallable((Callable<Void>) () -> {
//            dataBaseUtils.deleteReportFormInstance(id, D.T_GOOGLE_DRIVE_FORM_INSTANCE);
//            return null;
//        }).compose(applyCompletableSchedulers());
        return null;
    }

    @NonNull
    @Override
    public Single<List<ReportInstance>> listAllReportInstances() {
        return null;
    }

    @NonNull
    @Override
    public Single<List<ReportInstance>> listDraftReportInstances() {
        return Single.fromCallable(this::getDraftReportInstances)
                .compose(applySchedulers());
    }

    private List<ReportInstance> getDraftReportInstances() {
//        return dataBaseUtils.getReportFormInstances(new EntityStatus[]{
//                EntityStatus.UNKNOWN,
//                EntityStatus.DRAFT
//        }, D.T_GOOGLE_DRIVE_FORM_INSTANCE, D.T_GOOGLE_DRIVE);
        return null;
    }

    @NonNull
    @Override
    public Single<List<ReportInstance>> listOutboxReportInstances() {
        return Single.fromCallable(this::getOutboxReportInstances)
                .compose(applySchedulers());
    }

    @NonNull
    @Override
    public Single<List<ReportInstance>> listSubmittedReportInstances() {
        return Single.fromCallable(this::getSubmittedReportInstances)
                .compose(applySchedulers());
    }

    @NonNull
    @Override
    public Single<ReportInstanceBundle> getReportBundle(long id) {
//        return Single.fromCallable(() -> dataBaseUtils.getReportInstanceBundle(id, D.T_GOOGLE_DRIVE_FORM_INSTANCE, D.T_GOOGLE_DRIVE_INSTANCE_VAULT_FILE))
//                .compose(applySchedulers());
        return null;
    }

    private List<ReportInstance> getOutboxReportInstances() {
//        return dataBaseUtils.getReportFormInstances(new EntityStatus[]{
//                EntityStatus.FINALIZED,
//                EntityStatus.SUBMISSION_ERROR,
//                EntityStatus.SUBMISSION_PENDING,
//                EntityStatus.SUBMISSION_PARTIAL_PARTS,
//                EntityStatus.SUBMISSION_IN_PROGRESS,
//                EntityStatus.SCHEDULED,
//                EntityStatus.PAUSED
//        }, D.T_GOOGLE_DRIVE_FORM_INSTANCE, D.T_GOOGLE_DRIVE);
        return null;
    }

    private List<ReportInstance> getSubmittedReportInstances() {

//        return dataBaseUtils.getReportFormInstances(new EntityStatus[]{
//                EntityStatus.SUBMITTED
//        }, D.T_GOOGLE_DRIVE_FORM_INSTANCE, D.T_GOOGLE_DRIVE);
        return null;
    }

    @NonNull
    @Override
    public Completable removeNextCloudServer(long id) {
        return Completable.fromCallable((Callable<Void>) () -> {
            removeServer(id);
            return null;
        }).compose(applyCompletableSchedulers());
    }
}