package org.horizontal.tella.mobile.data.database;

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
import org.horizontal.tella.mobile.domain.entity.EntityStatus;
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile;
import org.horizontal.tella.mobile.domain.entity.googledrive.Config;
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer;
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance;
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstanceBundle;
import org.horizontal.tella.mobile.domain.repository.googledrive.ITellaGoogleDriveRepository;
import org.horizontal.tella.mobile.domain.repository.reports.ITellaReportsRepository;
import timber.log.Timber;

public class GoogleDriveDataSource implements ITellaGoogleDriveRepository, ITellaReportsRepository {

    private static GoogleDriveDataSource dataSource;
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

    private GoogleDriveDataSource(Context context, byte[] key) {
        System.loadLibrary("sqlcipher");
        HorizontalSQLiteOpenHelper sqLiteOpenHelper = new HorizontalSQLiteOpenHelper(context, key);
        database = sqLiteOpenHelper.getWritableDatabase();
        dataBaseUtils = new DataBaseUtils(database);
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
    public Single<List<GoogleDriveServer>> listGoogleDriveServers(String googleDriveId) {
        return Single.fromCallable(() -> dataSource.getListGoogleDriveServers(googleDriveId))
                .compose(applySchedulers());
    }

    private List<GoogleDriveServer> getListGoogleDriveServers(String googleDriveId) {
        Cursor cursor = null;
        List<GoogleDriveServer> servers = new ArrayList<>();

        try {
            cursor = database.query(
                    D.T_GOOGLE_DRIVE,
                    new String[]{D.C_ID,
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
                GoogleDriveServer server = cursorToGoogleDriveServer(cursor, googleDriveId);
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

    @NonNull
    @Override
    public Completable removeGoogleDriveServer(long id) {
        return Completable.fromCallable((Callable<Void>) () -> {
            removeServer(id);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    private void removeServer(long id) {
        database.delete(D.T_GOOGLE_DRIVE, D.C_ID + " = ?", new String[]{Long.toString(id)});
    }

    private GoogleDriveServer cursorToGoogleDriveServer(Cursor cursor, String googleId) {
        long googleDriveId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID));
        String folderId = cursor.getString(cursor.getColumnIndexOrThrow(D.C_GOOGLE_DRIVE_FOLDER_ID));
        String folderName = cursor.getString(cursor.getColumnIndexOrThrow(D.C_GOOGLE_DRIVE_FOLDER_NAME));
        String serverName = cursor.getString(cursor.getColumnIndexOrThrow(D.C_GOOGLE_DRIVE_SERVER_NAME));
        String userName = cursor.getString(cursor.getColumnIndexOrThrow(D.C_USERNAME));
        GoogleDriveServer server = new GoogleDriveServer(googleDriveId, folderName, folderId, googleId);
        server.setName(serverName);
        server.setUsername(userName);
        return server;
    }

    @NonNull
    @Override
    public Single<ReportInstance> saveInstance(@NonNull ReportInstance instance) {
        return Single.fromCallable(() -> dataBaseUtils.updateTellaReportsFormInstance(instance, D.T_GOOGLE_DRIVE_FORM_INSTANCE, D.T_GOOGLE_DRIVE_INSTANCE_VAULT_FILE))
                .compose(applySchedulers());
    }

    public Single<List<FormMediaFile>> getReportMediaFiles(ReportInstance instance) {
        return Single.fromCallable(() -> dataBaseUtils.getReportMediaFilesDB(instance, D.T_GOOGLE_DRIVE_INSTANCE_VAULT_FILE))
                .compose(applySchedulers());
    }

    @NonNull
    @Override
    public Completable deleteReportInstance(long id) {
        return Completable.fromCallable((Callable<Void>) () -> {
            dataBaseUtils.deleteReportFormInstance(id, D.T_GOOGLE_DRIVE_FORM_INSTANCE);
            return null;
        }).compose(applyCompletableSchedulers());
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
        return dataBaseUtils.getReportFormInstances(new EntityStatus[]{
                EntityStatus.UNKNOWN,
                EntityStatus.DRAFT
        }, D.T_GOOGLE_DRIVE_FORM_INSTANCE, D.T_GOOGLE_DRIVE);
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
        return Single.fromCallable(() -> dataBaseUtils.getReportInstanceBundle(id, D.T_GOOGLE_DRIVE_FORM_INSTANCE, D.T_GOOGLE_DRIVE_INSTANCE_VAULT_FILE))
                .compose(applySchedulers());
    }

    private List<ReportInstance> getOutboxReportInstances() {
        return dataBaseUtils.getReportFormInstances(new EntityStatus[]{
                EntityStatus.FINALIZED,
                EntityStatus.SUBMISSION_ERROR,
                EntityStatus.SUBMISSION_PENDING,
                EntityStatus.SUBMISSION_PARTIAL_PARTS,
                EntityStatus.SUBMISSION_IN_PROGRESS,
                EntityStatus.SCHEDULED,
                EntityStatus.PAUSED
        }, D.T_GOOGLE_DRIVE_FORM_INSTANCE, D.T_GOOGLE_DRIVE);
    }

    private List<ReportInstance> getSubmittedReportInstances() {

        return dataBaseUtils.getReportFormInstances(new EntityStatus[]{
                EntityStatus.SUBMITTED
        }, D.T_GOOGLE_DRIVE_FORM_INSTANCE, D.T_GOOGLE_DRIVE);
    }
}