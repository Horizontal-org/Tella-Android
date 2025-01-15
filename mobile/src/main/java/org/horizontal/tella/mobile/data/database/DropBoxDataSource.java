package org.horizontal.tella.mobile.data.database;

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
import org.horizontal.tella.mobile.domain.entity.EntityStatus;
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile;
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer;
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance;
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstanceBundle;
import org.horizontal.tella.mobile.domain.repository.dropbox.ITellaDropBoxRepository;
import org.horizontal.tella.mobile.domain.repository.reports.ITellaReportsRepository;
import timber.log.Timber;

public class DropBoxDataSource implements ITellaDropBoxRepository, ITellaReportsRepository {

    private static DropBoxDataSource dataSource;
    private final SQLiteDatabase database;
    private final DataBaseUtils dataBaseUtils;
    final private CompletableTransformer schedulersCompletableTransformer = observable -> observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());

    private CompletableTransformer applyCompletableSchedulers() {
        return schedulersCompletableTransformer;
    }

    private DropBoxDataSource(Context context, byte[] key) {
        HorizontalSQLiteOpenHelper sqLiteOpenHelper = new HorizontalSQLiteOpenHelper(context, key);
        database = sqLiteOpenHelper.getWritableDatabase();
        dataBaseUtils = new DataBaseUtils(database);
    }

    public static synchronized DropBoxDataSource getInstance(Context context, byte[] key) {
        if (dataSource == null) {
            dataSource = new DropBoxDataSource(context.getApplicationContext(), key);
        }

        return dataSource;
    }

    @NonNull
    @Override
    public Single<DropBoxServer> saveDropBoxServer(@NonNull DropBoxServer server) {
        return Single.fromCallable(() -> saveServer(server)).compose(applySchedulers());
    }

    private DropBoxServer saveServer(DropBoxServer server) {
        ContentValues values = new ContentValues();
        values.put(D.C_USERNAME, server.getUsername());
        values.put(D.C_DROPBOX_SERVER_NAME, server.getName());
        values.put(D.C_NAME, server.getName());
        values.put(D.C_DROPBOX_ACCESS_TOKEN, server.getToken());
        server.setId(database.insert(D.T_DROPBOX, null, values));
        return server;
    }


    private <T> SingleTransformer<T, T> applySchedulers() {
        //noinspection unchecked
        return (SingleTransformer<T, T>) schedulersTransformer;
    }

    final private SingleTransformer schedulersTransformer = observable -> observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());

    @NonNull
    @Override
    public Single<List<DropBoxServer>> listDropBoxServers() {
        return Single.fromCallable(() -> dataSource.getListDropBoxServers()).compose(applySchedulers());
    }

    private List<DropBoxServer> getListDropBoxServers() {
        Cursor cursor = null;
        List<DropBoxServer> servers = new ArrayList<>();

        try {
            cursor = database.query(D.T_DROPBOX, new String[]{D.C_ID, D.C_DROPBOX_ACCESS_TOKEN, D.C_DROPBOX_SERVER_NAME}, null, null, null, null, D.C_ID + " ASC", null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                DropBoxServer server = cursorToDropBoxServer(cursor);
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
    public Completable removeDropBoxServer(long id) {
        return Completable.fromCallable((Callable<Void>) () -> {
            removeServer(id);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    private void removeServer(long id) {
        database.delete(D.T_DROPBOX, D.C_ID + " = ?", new String[]{Long.toString(id)});
    }

    private DropBoxServer cursorToDropBoxServer(Cursor cursor) {
        long serverID = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID));
        String serverName = cursor.getString(cursor.getColumnIndexOrThrow(D.C_DROPBOX_SERVER_NAME));
        String token = cursor.getString(cursor.getColumnIndexOrThrow(D.C_DROPBOX_ACCESS_TOKEN));
        DropBoxServer server = new DropBoxServer(serverID, token);
        server.setName(serverName);
        return server;
    }

    @NonNull
    @Override
    public Single<ReportInstance> saveInstance(@NonNull ReportInstance instance) {
        return Single.fromCallable(() -> dataBaseUtils.updateTellaReportsFormInstance(instance, D.T_DROPBOX_FORM_INSTANCE, D.T_DROPBOX_INSTANCE_VAULT_FILE)).compose(applySchedulers());
    }

    public Single<List<FormMediaFile>> getReportMediaFiles(ReportInstance instance) {
        return Single.fromCallable(() -> dataBaseUtils.getReportMediaFilesDB(instance, D.T_DROPBOX_INSTANCE_VAULT_FILE)).compose(applySchedulers());
    }

    @NonNull
    @Override
    public Completable deleteReportInstance(long id) {
        return Completable.fromCallable((Callable<Void>) () -> {
            dataBaseUtils.deleteReportFormInstance(id, D.T_DROPBOX_FORM_INSTANCE);
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
        return Single.fromCallable(this::getDraftReportInstances).compose(applySchedulers());
    }

    private List<ReportInstance> getDraftReportInstances() {
        return dataBaseUtils.getReportFormInstances(new EntityStatus[]{EntityStatus.UNKNOWN, EntityStatus.DRAFT}, D.T_DROPBOX_FORM_INSTANCE, D.T_DROPBOX);
    }

    @NonNull
    @Override
    public Single<List<ReportInstance>> listOutboxReportInstances() {
        return Single.fromCallable(this::getOutboxReportInstances).compose(applySchedulers());
    }

    @NonNull
    @Override
    public Single<List<ReportInstance>> listSubmittedReportInstances() {
        return Single.fromCallable(this::getSubmittedReportInstances).compose(applySchedulers());
    }

    @NonNull
    @Override
    public Single<ReportInstanceBundle> getReportBundle(long id) {
        return Single.fromCallable(() -> dataBaseUtils.getReportInstanceBundle(id, D.T_DROPBOX_FORM_INSTANCE, D.T_DROPBOX_INSTANCE_VAULT_FILE)).compose(applySchedulers());
    }

    private List<ReportInstance> getOutboxReportInstances() {
        return dataBaseUtils.getReportFormInstances(new EntityStatus[]{EntityStatus.FINALIZED, EntityStatus.SUBMISSION_ERROR, EntityStatus.SUBMISSION_PENDING, EntityStatus.SUBMISSION_PARTIAL_PARTS, EntityStatus.SUBMISSION_IN_PROGRESS, EntityStatus.SCHEDULED, EntityStatus.PAUSED}, D.T_DROPBOX_FORM_INSTANCE, D.T_DROPBOX);
    }

    private List<ReportInstance> getSubmittedReportInstances() {
        return dataBaseUtils.getReportFormInstances(new EntityStatus[]{EntityStatus.SUBMITTED}, D.T_DROPBOX_FORM_INSTANCE, D.T_DROPBOX);
    }

    @NonNull
    @Override
    public Single<DropBoxServer> updateDropBoxServer(@NonNull DropBoxServer server) {
        return Single.fromCallable(() -> updateServer(server)).compose(applySchedulers());
    }

    private DropBoxServer updateServer(final DropBoxServer server) {
        ContentValues values = new ContentValues();
        values.put(D.C_DROPBOX_ACCESS_TOKEN, server.getToken());
        database.update(D.T_DROPBOX, values, D.C_ID + "= ?", new String[]{Long.toString(server.getId())});
        return server;
    }
}