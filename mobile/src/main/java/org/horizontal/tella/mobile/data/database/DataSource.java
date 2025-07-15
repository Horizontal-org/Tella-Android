package org.horizontal.tella.mobile.data.database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.VaultFile;

import net.zetetic.database.DatabaseUtils;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteQueryBuilder;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.CompletableTransformer;
import io.reactivex.Maybe;
import io.reactivex.MaybeTransformer;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import org.horizontal.tella.mobile.data.entity.MetadataEntity;
import org.horizontal.tella.mobile.data.entity.mapper.EntityMapper;
import org.horizontal.tella.mobile.data.sharedpref.Preferences;
import org.horizontal.tella.mobile.domain.entity.EntityStatus;
import org.horizontal.tella.mobile.domain.entity.FileUploadBundle;
import org.horizontal.tella.mobile.domain.entity.FileUploadInstance;
import org.horizontal.tella.mobile.domain.entity.IErrorBundle;
import org.horizontal.tella.mobile.domain.entity.OldMediaFile;
import org.horizontal.tella.mobile.domain.entity.collect.CollectForm;
import org.horizontal.tella.mobile.domain.entity.collect.CollectFormInstance;
import org.horizontal.tella.mobile.domain.entity.collect.CollectFormInstanceStatus;
import org.horizontal.tella.mobile.domain.entity.collect.CollectInstanceVaultFile;
import org.horizontal.tella.mobile.domain.entity.collect.CollectServer;
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile;
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFileStatus;
import org.horizontal.tella.mobile.domain.entity.collect.ListFormResult;
import org.horizontal.tella.mobile.domain.entity.collect.OdkForm;
import org.horizontal.tella.mobile.domain.entity.feedback.FeedbackInstance;
import org.horizontal.tella.mobile.domain.entity.feedback.FeedbackStatus;
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance;
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstanceBundle;
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer;
import org.horizontal.tella.mobile.domain.exception.NotFountException;
import org.horizontal.tella.mobile.domain.repository.ICollectFormsRepository;
import org.horizontal.tella.mobile.domain.repository.ICollectServersRepository;
import org.horizontal.tella.mobile.domain.repository.IMediaFileRecordRepository;
import org.horizontal.tella.mobile.domain.repository.IServersRepository;
import org.horizontal.tella.mobile.domain.repository.ITellaUploadServersRepository;
import org.horizontal.tella.mobile.domain.repository.ITellaUploadsRepository;
import org.horizontal.tella.mobile.domain.repository.feedback.ITellaFeedBackRepository;
import org.horizontal.tella.mobile.domain.repository.reports.ITellaReportsRepository;
import org.horizontal.tella.mobile.util.C;
import org.horizontal.tella.mobile.util.DateUtil;
import org.horizontal.tella.mobile.util.FileUtil;
import org.horizontal.tella.mobile.util.Util;
import timber.log.Timber;

public class DataSource implements IServersRepository, ITellaUploadServersRepository, ITellaUploadsRepository, ITellaReportsRepository, ICollectServersRepository, ICollectFormsRepository,
        IMediaFileRecordRepository, ITellaFeedBackRepository {
    private static DataSource dataSource;
    private final SQLiteDatabase database;
    private final DataBaseUtils dataBaseUtils;


    final private SingleTransformer schedulersTransformer =
            observable -> observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());

    final private CompletableTransformer schedulersCompletableTransformer =
            observable -> observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());

    final private MaybeTransformer schedulersMaybeTransformer =
            observable -> observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());


    private DataSource(Context context, byte[] key) {
        System.loadLibrary("sqlcipher");
        HorizontalSQLiteOpenHelper sqLiteOpenHelper = new HorizontalSQLiteOpenHelper(context, key);
        database = sqLiteOpenHelper.getWritableDatabase();
        dataBaseUtils = new DataBaseUtils(database);
    }

    public static synchronized DataSource getInstance(Context context, byte[] key) {
        if (dataSource == null) {
            try {
                dataSource = new DataSource(context.getApplicationContext(), key);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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

    private <T> MaybeTransformer<T, T> applyMaybeSchedulers() {
        //noinspection unchecked
        return (MaybeTransformer<T, T>) schedulersMaybeTransformer;
    }

    @Override
    public Single<List<CollectServer>> listCollectServers() {
        return Single.fromCallable(() -> dataSource.getServers())
                .compose(applySchedulers());
    }

    @Override
    public Single<List<TellaReportServer>> listTellaUploadServers() {
        return Single.fromCallable(() -> dataSource.getTUServers())
                .compose(applySchedulers());
    }

    @Override
    public Single<CollectServer> createCollectServer(final CollectServer server) {
        return Single.fromCallable(() -> dataSource.createServer(server))
                .compose(applySchedulers());
    }

    @Override
    public Single<TellaReportServer> createTellaUploadServer(final TellaReportServer server) {
        return Single.fromCallable(() -> dataSource.createTUServer(server))
                .compose(applySchedulers());
    }


    @Override
    public Single<TellaReportServer> updateTellaUploadServer(TellaReportServer server) {
        return Single.fromCallable(() -> dataSource.updateTUServer(server))
                .compose(applySchedulers());
    }

    @Override
    public Single<CollectServer> updateCollectServer(final CollectServer server) {
        return Single.fromCallable(() -> dataSource.updateServer(server))
                .compose(applySchedulers());
    }

    @Override
    public Single<CollectServer> getCollectServer(final long id) {
        return Single.fromCallable(() -> getServer(id))
                .compose(applySchedulers());
    }

    @Override
    public Single<TellaReportServer> getTellaUploadServer(final long id) {
        return Single.fromCallable(() -> getTUServer(id))
                .compose(applySchedulers());
    }

    @Override
    public Completable removeCollectServer(final long id) {
        return Completable.fromCallable((Callable<Void>) () -> {
            dataSource.removeServer(id);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    public Completable removeCachedForms() {
        return Completable.fromCallable(() -> {
            dataSource.removeCachedFormInstances();
            return true;
        });
    }

    @Override
    public Completable removeTellaServerAndResources(final long id) {
        return Completable.fromCallable((Callable<Void>) () -> {
            dataSource.deleteTellaServerAndResourcesDB(id);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @Override
    public Single<Long> countCollectServers() {
        return Single.fromCallable(() -> dataSource.countDBCollectServers())
                .compose(applySchedulers());
    }

    @Override
    public Single<Long> countTUServers() {
        return Single.fromCallable(() -> dataSource.countDBTUServers())
                .compose(applySchedulers());
    }

    @Override
    public Completable deleteAllServers() {
        return Completable.fromCallable((Callable<Void>) () -> {
            dataSource.deleteAllServersDB();
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @Override
    public Completable deleteFileUploadInstancesInStatus(UploadStatus status) {
        return Completable.fromCallable((Callable<Void>) () -> {
            dataSource.deleteFileUploadInstancesInStatusDB(status);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @Override
    public Completable deleteFileUploadInstancesNotInStatus(UploadStatus status) {
        return Completable.fromCallable((Callable<Void>) () -> {
            dataSource.deleteFileUploadInstancesNotInStatusDB(status);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @Override
    public Completable deleteFileUploadInstancesBySet(long set) {
        return Completable.fromCallable((Callable<Void>) () -> {
            dataSource.deleteFileUploadInstancesBySetDB(set);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @Override
    public Completable deleteFileUploadInstanceById(long id) {
        return Completable.fromCallable((Callable<Void>) () -> {
            dataSource.deleteFileUploadInstanceByIdDB(id);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @Override
    public Completable scheduleUploadReport(FormMediaFile mediaFile, Long serverId) {
        return Completable.fromCallable((Callable<Void>) () -> {
            scheduledReportUploadDB(mediaFile, serverId);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @Override
    public Single<List<CollectForm>> listBlankForms() {
        return Single.fromCallable(() -> dataSource.getBlankCollectForms())
                .compose(applySchedulers());
    }

    @Override
    public Single<List<CollectForm>> listFavoriteCollectForms() {
        return Single.fromCallable(() -> dataSource.getFavoriteCollectForms())
                .compose(applySchedulers());
    }

    public Single<CollectForm> getBlankCollectFormById(final String formID) {
        return Single.fromCallable(() -> dataSource.getBlankCollectForm(formID))
                .compose(applySchedulers());
    }

    @Override
    public Completable removeBlankFormDef(CollectForm form) {
        return Completable.fromCallable((Callable<Void>) () -> {
            removeCollectFormDef(form.getId());
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @Override
    public Single<ListFormResult> updateBlankForms(final ListFormResult listFormResult) {
        return Single.fromCallable(() -> {
            dataSource.updateBlankCollectForms(listFormResult);
            listFormResult.setForms(dataSource.getBlankCollectForms());
            return listFormResult;
        }).compose(applySchedulers());
    }

    @Override
    public Single<CollectForm> toggleFavorite(final CollectForm form) {
        return Single.fromCallable(() -> dataSource.toggleFavoriteCollectForm(form))
                .compose(applySchedulers());
    }

    @Override
    public Maybe<FormDef> getBlankFormDef(final CollectForm form) {
        return Maybe.fromCallable(() -> getCollectFormDef(form))
                .compose(applyMaybeSchedulers());
    }

    @Override
    public Single<FormDef> updateBlankCollectFormDef(final CollectForm form, final FormDef formDef) {
        return Single.fromCallable(() -> updateCollectFormDef(form, formDef)).compose(applySchedulers());
    }

    @Override
    public Single<FormDef> updateBlankFormDef(final CollectForm form, final FormDef formDef) {
        return Single.fromCallable(() -> updateCollectFormDef(form, formDef))
                .compose(applySchedulers());
    }

    @Override
    public Single<List<CollectFormInstance>> listDraftForms() {
        return Single.fromCallable(this::getDraftCollectFormInstances)
                .compose(applySchedulers());
    }

    @Override
    public Single<List<CollectFormInstance>> listSentForms() {
        return Single.fromCallable(this::getSubmitCollectFormInstances)
                .compose(applySchedulers());
    }

    @Override
    public Single<List<CollectFormInstance>> listPendingForms() {
        return Single.fromCallable(this::getPendingCollectFormInstances)
                .compose(applySchedulers());
    }

    @Override
    public Single<CollectFormInstance> saveInstance(final CollectFormInstance instance) {
        return Single.fromCallable(() -> updateCollectFormInstance(instance))
                .compose(applySchedulers());
    }

    @Override
    public Single<CollectFormInstance> getInstance(final long id) {
        return Single.fromCallable(() -> getCollectFormInstance(id))
                .compose(applySchedulers());
    }

    @Override
    public Completable deleteInstance(final long id) {
        return Completable.fromCallable((Callable<Void>) () -> {
            deleteCollectFormInstance(id);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @Override
    public Completable scheduleUploadMediaFiles(final List<VaultFile> vaultFiles) {
        return Completable.fromCallable((Callable<Void>) () -> {
            scheduleUploadMediaFilesDb(vaultFiles);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @Override
    public Completable scheduleUploadMediaFilesWithPriority(final List<VaultFile> vaultFiles, long uploadServerId, boolean metadata) {
        return Completable.fromCallable((Callable<Void>) () -> {
            scheduleUploadMediaFilesWithPriorityDb(vaultFiles, uploadServerId, metadata);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @Override
    public Completable scheduleUploadReportInstances(List<ReportInstance> reportInstances) {
        return null;
    }

    @Override
    public Completable logUploadedFile(final VaultFile vaultFile) {
        return Completable.fromCallable((Callable<Void>) () -> {
            logUploadedFileDb(vaultFile);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @Override
    public Completable setUploadStatus(final String mediaFileId, UploadStatus status, long uploadedSize, boolean retry) {
        return Completable.fromCallable((Callable<Void>) () -> {
            setUploadStatusDb(mediaFileId, status, uploadedSize, retry);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @Override
    public Completable setUploadReportStatus(String reportId, String vaultFileId, UploadStatus status, long uploadedSize, boolean retry) {
        return null;
    }

    public Single<List<VaultFile>> listMediaFiles(final Filter filter, final Sort sort) {
        return Single.fromCallable(() -> getMediaFiles(filter, sort))
                .compose(applySchedulers());
    }

    public Single<List<OldMediaFile>> listOldMediaFiles() {
        return Single.fromCallable(this::getAllOldMediaFiles)
                .compose(applySchedulers());
    }

    public Single<List<FileUploadInstance>> getFileUploadInstances() {
        return Single.fromCallable(this::getFileUploadInstancesDB)
                .compose(applySchedulers());
    }

    public Single<List<FileUploadInstance>> getFileUploadInstances(long set) {
        return Single.fromCallable(() -> getFileUploadInstancesDB(set))
                .compose(applySchedulers());
    }

    public Single<List<FormMediaFile>> getReportMediaFiles(ReportInstance instance) {
        return Single.fromCallable(() -> dataBaseUtils.getReportMediaFilesDB(instance, D.T_REPORT_INSTANCE_VAULT_FILE))
                .compose(applySchedulers());
    }

    public Single<List<FormMediaFile>> getReportMediaFiles() {
        return Single.fromCallable(this::getReportMediaFilesDB)
                .compose(applySchedulers());
    }

    private List<FormMediaFile> getReportMediaFilesDB() {
        Cursor cursor = null;
        List<FormMediaFile> files = new ArrayList<>();

        try {
            cursor = database.query(
                    D.T_REPORT_INSTANCE_VAULT_FILE,
                    new String[]{D.C_VAULT_FILE_ID, D.C_STATUS, D.C_UPLOADED_SIZE},
                    null,
                    null,
                    null, null,
                    D.C_VAULT_FILE_ID + " ASC",
                    null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                FormMediaFile formMediaFile = dataBaseUtils.cursorToFormMediaFile(cursor);
                files.add(formMediaFile);
            }

        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return files;
    }


    public Single<List<ReportInstance>> getFileUploadReportBundles(final UploadStatus status) {
        return null;
    }

    @Override
    public Maybe<VaultFile> getMediaFileThumbnail(final long id) {
        return Maybe.fromCallable(() -> getThumbnail(id))
                .compose(applyMaybeSchedulers());
    }

    @Override
    public Maybe<VaultFile> getMediaFileThumbnail(String uid) {
        return Maybe.fromCallable(() -> getThumbnail(uid))
                .compose(applyMaybeSchedulers());
    }

    @Override
    public Single<VaultFile> updateMediaFileThumbnail(VaultFile vaultFile) {
        return Single.fromCallable(() -> updateThumbnail(vaultFile))
                .compose(applySchedulers());
    }

    @Override
    public Single<List<VaultFile>> getMediaFiles(final long[] ids) {
        return Single.fromCallable(() -> getMediaFilesFromDb(ids))
                .compose(applySchedulers());
    }

    @Override
    public Single<VaultFile> getMediaFile(final long id) {
        return Single.fromCallable(() -> getMediaFileFromDb(id))
                .compose(applySchedulers());
    }

    @Override
    public Single<VaultFile> getMediaFile(final String uid) {
        return Single.fromCallable(() -> getMediaFileFromDb(uid))
                .compose(applySchedulers());
    }

    @Override
    public Single<VaultFile> deleteMediaFile(final VaultFile vaultFile, final IMediaFileDeleter deleter) {
        return Single.fromCallable(() -> deleteMediaFileFromDb(vaultFile, deleter))
                .compose(applySchedulers());
    }

    @Override
    public Single<VaultFile> attachMetadata(final String mediaFileId, final Metadata metadata) {
        return Single.fromCallable(() -> attachMediaFileMetadataDb(mediaFileId, metadata))

                .compose(applySchedulers());
    }

    private long countDBCollectServers() {
        return DatabaseUtils.queryNumEntries(database, D.T_COLLECT_SERVER);
    }

    private long countDBTUServers() {
        return DatabaseUtils.queryNumEntries(database, D.T_TELLA_UPLOAD_SERVER);
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

    private List<CollectServer> getServers() {
        Cursor cursor = null;
        List<CollectServer> servers = new ArrayList<>();

        try {
            cursor = database.query(
                    D.T_COLLECT_SERVER,
                    new String[]{D.C_ID, D.C_NAME, D.C_URL, D.C_USERNAME, D.C_PASSWORD, D.C_CHECKED},
                    null,
                    null,
                    null, null,
                    D.C_ID + " ASC",
                    null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                CollectServer collectServer = cursorToCollectServer(cursor);
                servers.add(collectServer);
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

    @Nullable
    private CollectServer getServer(long id) {
        try (Cursor cursor = database.query(
                D.T_COLLECT_SERVER,
                new String[]{D.C_ID, D.C_NAME, D.C_URL, D.C_USERNAME, D.C_PASSWORD, D.C_CHECKED},
                D.C_ID + "= ?",
                new String[]{Long.toString(id)},
                null, null, null, null)) {

            if (cursor.moveToFirst()) {
                return cursorToCollectServer(cursor);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        }

        return CollectServer.NONE;
    }

    @Nullable
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

    @Nullable
    private CollectForm getBlankCollectForm(String formID) {
        Cursor cursor = null;

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_COLLECT_BLANK_FORM + " JOIN " + D.T_COLLECT_SERVER + " ON " +
                            D.T_COLLECT_BLANK_FORM + "." + D.C_COLLECT_SERVER_ID + " = " + D.T_COLLECT_SERVER + "." + D.C_ID,
                    new String[]{
                            cn(D.T_COLLECT_BLANK_FORM, D.C_ID, D.A_COLLECT_BLANK_FORM_ID),
                            D.C_COLLECT_SERVER_ID,
                            D.C_FORM_ID,
                            D.T_COLLECT_BLANK_FORM + "." + D.C_NAME,
                            D.C_VERSION,
                            D.C_HASH,
                            D.C_DOWNLOADED,
                            D.C_FAVORITE,
                            D.C_UPDATED,
                            D.C_DOWNLOAD_URL,
                            cn(D.T_COLLECT_SERVER, D.C_NAME, D.A_SERVER_NAME),
                            cn(D.T_COLLECT_SERVER, D.C_USERNAME, D.A_SERVER_USERNAME)},
                    D.C_FORM_ID + " = ?",
                    null, null, null, null
            );

            cursor = database.rawQuery(query, new String[]{formID});

            if (cursor.moveToFirst()) {
                OdkForm form = cursorToOdkForm(cursor);

                long id = cursor.getLong(cursor.getColumnIndexOrThrow(D.A_COLLECT_BLANK_FORM_ID));
                long serverId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_COLLECT_SERVER_ID));
                boolean downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_DOWNLOADED)) == 1;
                boolean favorite = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_FAVORITE)) == 1;
                boolean updated = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_UPDATED)) == 1;
                String serverName = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_NAME));
                String username = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_USERNAME));

                CollectForm collectForm = new CollectForm(serverId, form);
                collectForm.setId(id);
                collectForm.setServerName(serverName);
                collectForm.setUsername(username);
                collectForm.setDownloaded(downloaded);
                collectForm.setFavorite(favorite);
                collectForm.setUpdated(updated);

                return collectForm;
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    public List<CollectForm> getBlankCollectForms() {
        Cursor cursor = null;
        List<CollectForm> forms = new ArrayList<>();

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_COLLECT_BLANK_FORM + " JOIN " + D.T_COLLECT_SERVER + " ON " +
                            D.T_COLLECT_BLANK_FORM + "." + D.C_COLLECT_SERVER_ID + " = " + D.T_COLLECT_SERVER + "." + D.C_ID,
                    new String[]{
                            cn(D.T_COLLECT_BLANK_FORM, D.C_ID, D.A_COLLECT_BLANK_FORM_ID),
                            D.C_COLLECT_SERVER_ID,
                            D.C_FORM_ID,
                            D.T_COLLECT_BLANK_FORM + "." + D.C_NAME,
                            D.C_VERSION,
                            D.C_HASH,
                            D.C_DOWNLOADED,
                            D.C_UPDATED,
                            D.C_FAVORITE,
                            D.C_DOWNLOAD_URL,
                            cn(D.T_COLLECT_SERVER, D.C_NAME, D.A_SERVER_NAME),
                            cn(D.T_COLLECT_SERVER, D.C_USERNAME, D.A_SERVER_USERNAME)},
                    null, null, null,
                    cn(D.T_COLLECT_BLANK_FORM, D.C_FAVORITE) + " DESC, " + cn(D.T_COLLECT_BLANK_FORM, D.C_ID) + " DESC",
                    null
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                OdkForm form = cursorToOdkForm(cursor);

                // todo: implement cursorToCollectForm
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(D.A_COLLECT_BLANK_FORM_ID));
                long serverId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_COLLECT_SERVER_ID));
                boolean downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_DOWNLOADED)) == 1;
                boolean favorite = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_FAVORITE)) == 1;
                boolean updated = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_UPDATED)) == 1;
                String serverName = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_NAME));
                String username = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_USERNAME));

                CollectForm collectForm = new CollectForm(serverId, form);
                collectForm.setId(id);
                collectForm.setServerName(serverName);
                collectForm.setUsername(username);
                collectForm.setDownloaded(downloaded);
                collectForm.setFavorite(favorite);
                collectForm.setUpdated(updated);

                forms.add(collectForm);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return forms;
    }

    private List<CollectForm> getFavoriteCollectForms() {
        Cursor cursor = null;
        List<CollectForm> forms = new ArrayList<>();

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_COLLECT_BLANK_FORM + " JOIN " + D.T_COLLECT_SERVER + " ON " +
                            D.T_COLLECT_BLANK_FORM + "." + D.C_COLLECT_SERVER_ID + " = " + D.T_COLLECT_SERVER + "." + D.C_ID,
                    new String[]{
                            cn(D.T_COLLECT_BLANK_FORM, D.C_ID, D.A_COLLECT_BLANK_FORM_ID),
                            D.C_COLLECT_SERVER_ID,
                            D.C_FORM_ID,
                            D.T_COLLECT_BLANK_FORM + "." + D.C_NAME,
                            D.C_VERSION,
                            D.C_HASH,
                            D.C_DOWNLOADED,
                            D.C_UPDATED,
                            D.C_FAVORITE,
                            D.C_DOWNLOAD_URL,
                            cn(D.T_COLLECT_SERVER, D.C_NAME, D.A_SERVER_NAME),
                            cn(D.T_COLLECT_SERVER, D.C_USERNAME, D.A_SERVER_USERNAME)},
                    D.C_FAVORITE + " =1 ", null, null,
                    cn(D.T_COLLECT_BLANK_FORM, D.C_FAVORITE) + " DESC, " + cn(D.T_COLLECT_BLANK_FORM, D.C_ID) + " DESC",
                    null
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                OdkForm form = cursorToOdkForm(cursor);

                // todo: implement cursorToCollectForm
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(D.A_COLLECT_BLANK_FORM_ID));
                long serverId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_COLLECT_SERVER_ID));
                boolean downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_DOWNLOADED)) == 1;
                boolean favorite = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_FAVORITE)) == 1;
                boolean updated = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_UPDATED)) == 1;
                String serverName = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_NAME));
                String username = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_USERNAME));

                CollectForm collectForm = new CollectForm(serverId, form);
                collectForm.setId(id);
                collectForm.setServerName(serverName);
                collectForm.setUsername(username);
                collectForm.setDownloaded(downloaded);
                collectForm.setFavorite(favorite);
                collectForm.setUpdated(updated);

                forms.add(collectForm);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return forms;
    }

    public List<CollectInstanceVaultFile> getCollectInstanceVaultFilesDB() {
        List<CollectInstanceVaultFile> files = new ArrayList<>();
        Cursor cursor = null;

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_MEDIA_FILE + " JOIN " + D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE + " ON " +
                            cn(D.T_MEDIA_FILE, D.C_ID) + " = " + cn(D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE, D.C_MEDIA_FILE_ID),
                    new String[]{
                            cn(D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE, D.C_ID, D.A_COLLECT_FORM_INSTANCE_ID),
                            D.C_COLLECT_FORM_INSTANCE_ID,
                            D.C_UID,
                            cn(D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE, D.C_STATUS, D.A_FORM_MEDIA_FILE_STATUS)},
                    "1 = 1",
                    null, null, null, null
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(D.A_COLLECT_FORM_INSTANCE_ID));
                long instanceId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_COLLECT_FORM_INSTANCE_ID));
                String vaultFileId = cursor.getString(cursor.getColumnIndexOrThrow(D.C_UID));
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(D.A_FORM_MEDIA_FILE_STATUS));

                files.add(new CollectInstanceVaultFile(id, instanceId, vaultFileId, status));
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return files;
    }

    public boolean insertCollectInstanceVaultFiles(final List<CollectInstanceVaultFile> files) {
        long count = files.size();
        try {
            database.beginTransaction();
            for (CollectInstanceVaultFile file : files) {
                ContentValues values = new ContentValues();
                values.put(D.C_ID, file.getId());
                values.put(D.C_COLLECT_FORM_INSTANCE_ID, file.getInstanceId());
                values.put(D.C_VAULT_FILE_ID, file.getVaultFileId());
                values.put(D.C_STATUS, file.getStatus());

                database.insert(D.T_COLLECT_FORM_INSTANCE_VAULT_FILE, null, values);
                count = count - 1;
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            database.endTransaction();
        }
        return (count == 0);
    }

    private CollectServer createServer(final CollectServer server) {
        ContentValues values = new ContentValues();
        values.put(D.C_NAME, server.getName());
        values.put(D.C_URL, server.getUrl());
        values.put(D.C_USERNAME, server.getUsername());
        values.put(D.C_PASSWORD, server.getPassword());
        values.put(D.C_CHECKED, server.isChecked() ? 1 : 0);

        server.setId(database.insert(D.T_COLLECT_SERVER, null, values));

        return server;
    }

    private TellaReportServer createTUServer(final TellaReportServer server) {
        ContentValues values = new ContentValues();
        values.put(D.C_NAME, server.getName());
        values.put(D.C_URL, server.getUrl());
        values.put(D.C_USERNAME, server.getUsername());
        values.put(D.C_PROJECT_ID, server.getProjectId());
        values.put(D.C_PROJECT_NAME, server.getProjectName());
        values.put(D.C_PROJECT_SLUG, server.getProjectSlug());
        values.put(D.C_PASSWORD, server.getPassword());
        values.put(D.C_CHECKED, server.isChecked() ? 1 : 0);
        values.put(D.C_ACCESS_TOKEN, "Bearer " + server.getAccessToken());
        values.put(D.C_ACTIVATED_METADATA, server.isActivatedMetadata() ? 1 : 0);
        values.put(D.C_BACKGROUND_UPLOAD, server.isActivatedBackgroundUpload() ? 1 : 0);
        values.put(D.C_AUTO_DELETE, server.isAutoDelete() ? 1 : 0);
        values.put(D.C_AUTO_UPLOAD, server.isAutoUpload() ? 1 : 0);

        server.setId(database.insert(D.T_TELLA_UPLOAD_SERVER, null, values));

        return server;
    }

    private void updateBlankCollectForms(final ListFormResult result) {
        List<CollectForm> forms = result.getForms();
        List<IErrorBundle> errors = result.getErrors();

        List<String> formIDs = new ArrayList<>(forms.size());
        List<String> errorServerIDs = new ArrayList<>(errors.size());

        for (CollectForm form : forms) {
            formIDs.add(DatabaseUtils.sqlEscapeString(form.getForm().getFormID()));

            CollectForm current = getBlankCollectForm(form.getForm().getFormID());
            ContentValues values = new ContentValues();

            if (current != null) {
                // if hashes are same, do nothing
                if (TextUtils.equals(form.getForm().getHash(), current.getForm().getHash())) {
                    continue;
                }

                values.put(D.C_UPDATED, true);
                values.put(D.C_HASH, form.getForm().getHash());

                int num = database.update(D.T_COLLECT_BLANK_FORM, values, D.C_ID + " = ?",
                        new String[]{Long.toString(current.getId())});
                if (num > 0) {
                    form.setUpdated(true);
                }
            } else {
                values.put(D.C_COLLECT_SERVER_ID, form.getServerId());
                values.put(D.C_FORM_ID, form.getForm().getFormID());
                values.put(D.C_VERSION, form.getForm().getVersion());
                values.put(D.C_HASH, form.getForm().getHash());
                values.put(D.C_NAME, form.getForm().getName());
                values.put(D.C_DOWNLOAD_URL, form.getForm().getDownloadUrl());

                long id = database.insert(D.T_COLLECT_BLANK_FORM, null, values);
                if (id != -1) {
                    form.setId(id);
                }
            }
        }

        // get serverIds with errors in form list
        for (IErrorBundle error : errors) {
            errorServerIDs.add(Long.toString(error.getServerId()));
        }

        // construct where clause for deletion
        String whereClause = "1 = 1";

        if (formIDs.size() > 0) {
            whereClause += " AND (" + D.C_FORM_ID + " NOT IN (" + TextUtils.join(",", formIDs) + "))";
        }

        if (errorServerIDs.size() > 0) {
            whereClause += " AND (" + D.C_COLLECT_SERVER_ID + " NOT IN (" + TextUtils.join(",", errorServerIDs) + "))";
        }

        // delete all forms not sent by server, leave forms from servers with error
        database.delete(D.T_COLLECT_BLANK_FORM, whereClause, null);

        // todo: mark them with updated num, delete everyone not updated..
    }

    private CollectForm toggleFavoriteCollectForm(CollectForm form) {
        ContentValues values = new ContentValues();
        values.put(D.C_FAVORITE, !form.isPinned());

        int num = database.update(D.T_COLLECT_BLANK_FORM, values, D.C_ID + "= ?", new String[]{Long.toString(form.getId())});
        if (num > 0) {
            form.setFavorite(!form.isPinned());
        }

        return form;
    }

   /* @Nullable
    private FormDef getCollectFormDef(String formId, String versionId) {

        try (Cursor cursor = database.query(
                D.T_COLLECT_BLANK_FORM,
                new String[]{D.C_FORM_DEF},
                D.C_FORM_ID + "= ? AND " + D.C_VERSION + " = ?",
                new String[]{formId, versionId},
                null, null, null, null)) {

            if (cursor.moveToFirst()) {
                return deserializeFormDef(cursor.getBlob(cursor.getColumnIndexOrThrow(D.C_FORM_DEF)));
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        }

        return null;
    }*/

    @Nullable
    private FormDef getCollectFormDef(CollectForm form) {
        Cursor cursor = null;

        try {
            cursor = database.query(
                    D.T_COLLECT_BLANK_FORM,
                    new String[]{D.C_FORM_DEF},
                    D.C_FORM_ID + "= ? AND " + D.C_VERSION + " = ?",
                    new String[]{form.getForm().getFormID(), form.getForm().getVersion()},
                    null, null, null, null);

            if (cursor.moveToFirst()) {
                // todo: check if byte[] is empty and return null
                return deserializeFormDef(cursor.getBlob(cursor.getColumnIndexOrThrow(D.C_FORM_DEF)));
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null; // let rx crash for this..
    }

    @Nullable
    private void removeCollectFormDef(Long formId) throws NotFountException {
        try {
            database.beginTransaction();
            int count = database.delete(D.T_COLLECT_BLANK_FORM, D.C_ID + " = ?", new String[]{Long.toString(formId)});

            if (count != 1) {
                throw new NotFountException();
            }
            database.setTransactionSuccessful();

        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            database.endTransaction();
        }
        throw new NotFountException();
    }

    private List<VaultFile> getMediaFilesFromDb(long[] ids) {
        List<VaultFile> mediaFiles = new ArrayList<>();
        Cursor cursor = null;

        if (ids.length == 0) {
            return mediaFiles;
        }

        String[] stringIds = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            stringIds[i] = Long.toString(ids[i]);
        }

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_MEDIA_FILE,
                    new String[]{
                            cn(D.T_MEDIA_FILE, D.C_ID, D.A_MEDIA_FILE_ID),
                            D.C_PATH,
                            D.C_UID,
                            D.C_FILE_NAME,
                            D.C_METADATA,
                            D.C_CREATED,
                            D.C_DURATION,
                            D.C_ANONYMOUS,
                            D.C_SIZE,
                            D.C_HASH},
                    cn(D.T_MEDIA_FILE, D.C_ID) + " IN (" + TextUtils.join(", ", stringIds) + ")",
                    null, null, null, null
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                mediaFiles.add(cursorToMediaFile(cursor));
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return mediaFiles;
    }

    private VaultFile getMediaFileFromDb(long id) throws NotFountException {
        Cursor cursor = null;

        try {
            cursor = database.query(
                    D.T_MEDIA_FILE,
                    new String[]{
                            cn(D.T_MEDIA_FILE, D.C_ID, D.A_MEDIA_FILE_ID),
                            D.C_PATH,
                            D.C_UID,
                            D.C_FILE_NAME,
                            D.C_METADATA,
                            D.C_CREATED,
                            D.C_DURATION,
                            D.C_ANONYMOUS,
                            D.C_SIZE,
                            D.C_HASH},
                    cn(D.T_MEDIA_FILE, D.C_ID) + " = ?",
                    new String[]{Long.toString(id)},
                    null, null, null, null
            );

            if (cursor.moveToFirst()) {
                return cursorToMediaFile(cursor);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        throw new NotFountException();
    }

    private VaultFile getMediaFileFromDb(String uid) throws NotFountException {
        Cursor cursor = null;

        try {
            cursor = database.query(
                    D.T_MEDIA_FILE,
                    new String[]{
                            cn(D.T_MEDIA_FILE, D.C_ID, D.A_MEDIA_FILE_ID),
                            D.C_PATH,
                            D.C_UID,
                            D.C_FILE_NAME,
                            D.C_METADATA,
                            D.C_CREATED,
                            D.C_DURATION,
                            D.C_ANONYMOUS,
                            D.C_SIZE,
                            D.C_HASH
                    },
                    cn(D.T_MEDIA_FILE, D.C_UID) + " = ?",
                    new String[]{uid},
                    null, null, null, null
            );

            if (cursor.moveToFirst()) {
                return cursorToMediaFile(cursor);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        throw new NotFountException();
    }

    private VaultFile deleteMediaFileFromDb(VaultFile vaultFile, IMediaFileDeleter deleter) throws NotFountException {
        try {
            database.beginTransaction();

            int count = database.delete(D.T_MEDIA_FILE, D.C_ID + " = ?", new String[]{vaultFile.id});

            if (count != 1) {
                throw new NotFountException();
            }

            if (deleter.delete(vaultFile)) {
                database.setTransactionSuccessful();
            }

            return vaultFile;
        } finally {
            database.endTransaction();
        }
    }

    private void logUploadedFileDb(VaultFile file) {
        try {
            long set = calculateCurrentFileUploadSet();

            ContentValues values = new ContentValues();
            values.put(D.C_MEDIA_FILE_ID, file.id);
            values.put(D.C_UPDATED, Util.currentTimestamp());
            values.put(D.C_CREATED, Util.currentTimestamp());
            values.put(D.C_STATUS, UploadStatus.UPLOADED.ordinal());
            values.put(D.C_SIZE, file.size);
            values.put(D.C_SET, set);

            database.insertWithOnConflict(
                    D.T_MEDIA_FILE_UPLOAD,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_IGNORE);

        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        }
    }

    private void scheduleUploadMediaFilesWithPriorityDb(List<VaultFile> vaultFiles, long uploadServerId, boolean metadata) {
        try {

            long set = calculateCurrentFileUploadSet();
            int retries = getMaxRetries(); //make sure that these files are taken first from the set
            int manualUpload = 1;

            for (VaultFile vaultFile : vaultFiles) {
                ContentValues values = new ContentValues();
                values.put(D.C_MEDIA_FILE_ID, vaultFile.id);
                values.put(D.C_UPDATED, Util.currentTimestamp());
                values.put(D.C_CREATED, Util.currentTimestamp());
                values.put(D.C_STATUS, UploadStatus.SCHEDULED.ordinal());
                values.put(D.C_INCLUDE_METADATA, metadata ? 1 : 0);
                values.put(D.C_MANUAL_UPLOAD, manualUpload);
                values.put(D.C_SERVER_ID, uploadServerId);
                values.put(D.C_SIZE, vaultFile.size);
                values.put(D.C_SET, set);
                values.put(D.C_RETRY_COUNT, retries);

                database.insertWithOnConflict(
                        D.T_MEDIA_FILE_UPLOAD,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        }
    }


    private void scheduledReportUploadDB(FormMediaFile mediaFile, Long serverId) {
        Cursor cursor = null;
        ReportInstance reportInstance = null;

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_REPORT_FORM_INSTANCE +
                            " JOIN " + D.T_TELLA_UPLOAD_SERVER + " ON " +
                            cn(D.T_REPORT_FORM_INSTANCE, D.C_REPORT_SERVER_ID) + " = " + cn(D.T_TELLA_UPLOAD_SERVER, D.C_ID),
                    new String[]{
                            cn(D.T_REPORT_FORM_INSTANCE, D.C_ID, D.A_TELLA_UPLOAD_INSTANCE_ID),
                            D.C_REPORT_SERVER_ID,
                            D.C_STATUS,
                            D.C_UPDATED,
                            D.C_CURRENT_UPLOAD,
                            D.C_DESCRIPTION_TEXT,
                            D.C_TITLE,
                            D.C_REPORT_API_ID,
                            cn(D.T_TELLA_UPLOAD_SERVER, D.C_NAME, D.A_SERVER_NAME),
                            cn(D.T_TELLA_UPLOAD_SERVER, D.C_USERNAME, D.A_SERVER_USERNAME)},
                    D.C_CURRENT_UPLOAD + " = 1",
                    null, null,
                    cn(D.T_REPORT_FORM_INSTANCE, D.C_ID) + " DESC",
                    null
            );
            cursor = database.rawQuery(query, null);

            if (cursor == null || cursor.getColumnCount() == 0) {
                ReportInstance newReportInstance = ReportInstance.getAutoReportReportInstance(serverId, "Auto-report " + DateUtil.getDateTimeString());
                newReportInstance.getWidgetMediaFiles().add(mediaFile);
                dataBaseUtils.updateTellaReportsFormInstance(newReportInstance, D.T_REPORT_FORM_INSTANCE, D.T_REPORT_INSTANCE_VAULT_FILE);
                ;
            } else {
                getLastScheduledReportFromCursor(cursor, mediaFile, serverId);
            }

        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    private ReportInstance getLastScheduledReportFromCursor(Cursor cursor, FormMediaFile mediaFile, Long serverId) {
        List<ReportInstance> instances = new ArrayList<>();
        ReportInstance reportInstance;
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            ReportInstance instance = cursorToReportFormInstance(cursor);
            instances.add(instance);
        }

        if (!instances.isEmpty()) {
            ReportInstance currentInstance = instances.get(0);

            if (Util.currentTimestamp() - currentInstance.getUpdated() > C.UPLOAD_SET_DURATION) {
                currentInstance.setCurrent(0);
                currentInstance.setStatus(EntityStatus.SUBMITTED);
                currentInstance.setWidgetMediaFiles(getReportFiles(currentInstance, null));
                dataBaseUtils.updateTellaReportsFormInstance(currentInstance, D.T_REPORT_FORM_INSTANCE, D.T_REPORT_INSTANCE_VAULT_FILE);
                ReportInstance newReportInstance = ReportInstance.getAutoReportReportInstance(serverId, "Auto-report " + DateUtil.getDateTimeString());
                newReportInstance.getWidgetMediaFiles().add(mediaFile);
                reportInstance = dataBaseUtils.updateTellaReportsFormInstance(newReportInstance, D.T_REPORT_FORM_INSTANCE, D.T_REPORT_INSTANCE_VAULT_FILE);
            } else {
                currentInstance.setWidgetMediaFiles(getReportFiles(currentInstance, mediaFile));
                currentInstance.setStatus(EntityStatus.SCHEDULED);
                reportInstance = dataBaseUtils.updateTellaReportsFormInstance(currentInstance, D.T_REPORT_FORM_INSTANCE, D.T_REPORT_INSTANCE_VAULT_FILE);
            }
        } else {
            ReportInstance newReportInstance = ReportInstance.getAutoReportReportInstance(serverId, "Auto-report " + DateUtil.getDateTimeString());
            newReportInstance.setWidgetMediaFiles(getReportFiles(newReportInstance, mediaFile));
            reportInstance = dataBaseUtils.updateTellaReportsFormInstance(newReportInstance, D.T_REPORT_FORM_INSTANCE, D.T_REPORT_INSTANCE_VAULT_FILE);
        }
        return reportInstance;
    }

    private List<FormMediaFile> getReportFiles(ReportInstance instance, @Nullable FormMediaFile mediaFile) {
        List<FormMediaFile> mediaFiles = dataBaseUtils.getReportMediaFilesDB(instance, D.T_REPORT_FORM_INSTANCE);
        if (mediaFile != null) {
            mediaFiles.add(mediaFile);
        }

        return mediaFiles;
    }

    private void scheduleUploadMediaFilesDb(List<VaultFile> vaultFiles) {
        try {

            long set = calculateCurrentFileUploadSet();

            int metadata = Preferences.isMetadataAutoUpload() ? 1 : 0;
            long uploadServerId = Preferences.getAutoUploadServerId();

            for (VaultFile vaultFile : vaultFiles) {
                ContentValues values = new ContentValues();
                values.put(D.C_MEDIA_FILE_ID, vaultFile.id);
                values.put(D.C_UPDATED, Util.currentTimestamp());
                values.put(D.C_CREATED, Util.currentTimestamp());
                values.put(D.C_STATUS, UploadStatus.SCHEDULED.ordinal());
                values.put(D.C_INCLUDE_METADATA, metadata);
                values.put(D.C_MANUAL_UPLOAD, 0);
                values.put(D.C_SERVER_ID, uploadServerId);
                values.put(D.C_SIZE, vaultFile.size);
                values.put(D.C_SET, set);

                database.insertWithOnConflict(
                        D.T_MEDIA_FILE_UPLOAD,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        }
    }

    private long calculateCurrentFileUploadSet() {
        Cursor cursor = null;
        long lastSet = 0;
        long newSet;
        long lastUpdate = 0;
        int maxStatus = 0;

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_MEDIA_FILE_UPLOAD,
                    new String[]{
                            "MAX (" + D.C_STATUS + ")",
                            "MAX (" + D.C_UPDATED + ")",
                            "MAX (" + D.C_SET + ")"},
                    null,
                    null, null, null, null
            );

            cursor = database.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                lastUpdate = cursor.getLong(cursor.getColumnIndexOrThrow("MAX (" + D.C_UPDATED + ")"));
                lastSet = cursor.getLong(cursor.getColumnIndexOrThrow("MAX (" + D.C_SET + ")"));
                maxStatus = cursor.getInt(cursor.getColumnIndexOrThrow("MAX (" + D.C_STATUS + ")"));
            }
            if (Util.currentTimestamp() - lastUpdate > C.UPLOAD_SET_DURATION && maxStatus < UploadStatus.UPLOADING.ordinal()) {
                newSet = lastSet + 1;
            } else {
                newSet = lastSet;
            }

            return newSet;
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return lastSet;
    }

    private int getMaxRetries() {
        Cursor cursor = null;
        int maxRetries = 1;

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_MEDIA_FILE_UPLOAD,
                    new String[]{
                            "MAX (" + D.C_RETRY_COUNT + ")"},
                    null,
                    null, null, null, null
            );

            cursor = database.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                maxRetries = cursor.getInt(cursor.getColumnIndexOrThrow("MAX (" + D.C_RETRY_COUNT + ")"));
            }

            return maxRetries + 1;
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return maxRetries + 1;
    }

    private void setUploadStatusDb(String mediaFileId, UploadStatus status, long uploadedSize, boolean retry) {
        ContentValues values = new ContentValues();
        values.put(D.C_STATUS, status.ordinal());
        if (status == UploadStatus.UPLOADING || status == UploadStatus.UPLOADED) {
            values.put(D.C_UPLOADED, uploadedSize);
        }
        values.put(D.C_UPDATED, Util.currentTimestamp());
        if (retry) {
            values.put(D.C_RETRY_COUNT, D.C_RETRY_COUNT + 1);
        }
        database.update(D.T_MEDIA_FILE_UPLOAD, values, D.C_MEDIA_FILE_ID + " = ?",
                new String[]{mediaFileId});
    }

    private VaultFile attachMediaFileMetadataDb(String mediaFileId, @Nullable Metadata metadata) throws NotFountException {
        ContentValues values = new ContentValues();
        values.put(D.C_METADATA, new GsonBuilder().create().toJson(new EntityMapper().transform(metadata)));

        database.update(D.T_MEDIA_FILE, values, D.C_ID + " = ?",
                new String[]{mediaFileId});

        return getMediaFileFromDb(mediaFileId);
    }

    private CollectFormInstance getCollectFormInstance(long id) {
        Cursor cursor = null;

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_COLLECT_FORM_INSTANCE + " JOIN " + D.T_COLLECT_SERVER + " ON " +
                            cn(D.T_COLLECT_FORM_INSTANCE, D.C_COLLECT_SERVER_ID) + " = " + cn(D.T_COLLECT_SERVER, D.C_ID),
                    new String[]{
                            cn(D.T_COLLECT_FORM_INSTANCE, D.C_ID, D.A_COLLECT_FORM_INSTANCE_ID),
                            D.C_COLLECT_SERVER_ID,
                            D.C_STATUS,
                            D.C_UPDATED,
                            D.C_FORM_ID,
                            D.C_VERSION,
                            D.C_FORM_NAME,
                            D.C_INSTANCE_NAME,
                            D.C_FORM_DEF,
                            D.C_FORM_PART_STATUS,
                            cn(D.T_COLLECT_SERVER, D.C_NAME, D.A_SERVER_NAME),
                            cn(D.T_COLLECT_SERVER, D.C_USERNAME, D.A_SERVER_USERNAME)},
                    cn(D.T_COLLECT_FORM_INSTANCE, D.C_ID) + "= ?",
                    null, null, null, null
            );

            cursor = database.rawQuery(query, new String[]{Long.toString(id)});

            if (cursor.moveToFirst()) {
                CollectFormInstance instance = cursorToCollectFormInstance(cursor);

                instance.setFormDef(deserializeFormDef(cursor.getBlob(cursor.getColumnIndexOrThrow(D.C_FORM_DEF))));
                // instance.setFormDef(getCollectFormDef(instance.getFormID(),instance.getVersion()));

                HashMap<String, Integer> vaultFileIds = getFormInstanceMediaFilesIdsFromDb(instance.getId());
                instance.setWidgetMediaFilesIds(vaultFileIds);

                return instance;
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return CollectFormInstance.NONE;
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void deleteCollectFormInstance(long id) throws NotFountException {
        int count = database.delete(D.T_COLLECT_FORM_INSTANCE, D.C_ID + " = ?", new String[]{Long.toString(id)});

        if (count != 1) {
            throw new NotFountException();
        }
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void deleteFileUploadInstancesInStatusDB(UploadStatus status) {
        int count = database.delete(D.T_MEDIA_FILE_UPLOAD, D.C_STATUS + " = ?", new String[]{Integer.toString(status.ordinal())});
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void deleteFileUploadInstancesNotInStatusDB(UploadStatus status) {
        int count = database.delete(D.T_MEDIA_FILE_UPLOAD, D.C_STATUS + " <> ?", new String[]{Integer.toString(status.ordinal())});
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void deleteFileUploadInstancesBySetDB(long set) {
        int count = database.delete(D.T_MEDIA_FILE_UPLOAD, D.C_SET + " = ?", new String[]{Long.toString(set)});
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void deleteFileUploadInstanceByIdDB(long id) {
        int count = database.delete(D.T_MEDIA_FILE_UPLOAD, D.C_ID + " = ?", new String[]{Long.toString(id)});
    }

    private List<FileUploadInstance> getFileUploadInstancesDB() {
        List<FileUploadInstance> instances = new ArrayList<>();
        Cursor cursor = null;

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_MEDIA_FILE_UPLOAD,
                    new String[]{
                            D.C_ID,
                            D.C_MEDIA_FILE_ID,
                            D.C_UPDATED,
                            D.C_CREATED,
                            D.C_STATUS,
                            D.C_SIZE,
                            D.C_UPLOADED,
                            D.C_RETRY_COUNT,
                            D.C_SET},
                    null,
                    null, null, D.C_SET + " DESC", null
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                FileUploadInstance instance = cursorToFileUploadInstance(cursor);
                try {
                    VaultFile mediaFile = getMediaFileFromDb(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_MEDIA_FILE_ID)));
                    instance.setMediaFile(mediaFile);
                } catch (NotFountException e) {
                    Timber.e(e, getClass().getName());
                }
                instances.add(instance);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return instances;
    }

    private List<FileUploadInstance> getFileUploadInstancesDB(long set) {
        List<FileUploadInstance> instances = new ArrayList<>();
        Cursor cursor = null;

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_MEDIA_FILE_UPLOAD,
                    new String[]{
                            D.C_ID,
                            D.C_MEDIA_FILE_ID,
                            D.C_UPDATED,
                            D.C_CREATED,
                            D.C_STATUS,
                            D.C_SIZE,
                            D.C_UPLOADED,
                            D.C_RETRY_COUNT,
                            D.C_SET},
                    D.C_SET + "= ?",
                    null, null, D.C_SET + " DESC", null
            );

            cursor = database.rawQuery(query, new String[]{Long.toString(set)});

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                FileUploadInstance instance = cursorToFileUploadInstance(cursor);
                try {
                    VaultFile mediaFile = getMediaFileFromDb(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_MEDIA_FILE_ID)));
                    instance.setMediaFile(mediaFile);
                } catch (NotFountException e) {
                    Timber.e(e, getClass().getName());
                }
                instances.add(instance);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return instances;
    }

    private HashMap<String, Integer> getFormInstanceMediaFilesIdsFromDb(long instanceId) {

        HashMap<String, Integer> ids = new HashMap<>();
        Cursor cursor = null;

        try {

            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_COLLECT_FORM_INSTANCE_VAULT_FILE,
                    new String[]{
                            D.C_VAULT_FILE_ID, D.C_STATUS},
                    D.C_COLLECT_FORM_INSTANCE_ID + "= ?",
                    null, null, D.C_VAULT_FILE_ID + " DESC", null
            );

            cursor = database.rawQuery(query, new String[]{Long.toString(instanceId)});

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                String vaultFileId = cursor.getString(cursor.getColumnIndexOrThrow(D.C_VAULT_FILE_ID));
                Integer fileStatus = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_STATUS));
                ids.put(vaultFileId, fileStatus);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return ids;
    }

    @Nullable
    private VaultFile getThumbnail(long id) {
        Cursor cursor = null;

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_MEDIA_FILE,
                    new String[]{D.C_THUMBNAIL},
                    D.C_ID + "= ?",
                    null, null, null, null
            );

            cursor = database.rawQuery(query, new String[]{Long.toString(id)});

            if (cursor.moveToFirst()) {
                VaultFile vaultFile = new VaultFile();
                vaultFile.thumb = cursor.getBlob(cursor.getColumnIndexOrThrow(D.C_THUMBNAIL));
                if (vaultFile.thumb != null) {
                    return vaultFile;
                }
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    @Nullable
    private VaultFile getThumbnail(String uid) {

        final String query = SQLiteQueryBuilder.buildQueryString(
                false,
                D.T_MEDIA_FILE,
                new String[]{D.C_THUMBNAIL},
                D.C_UID + "= ?",
                null, null, null, null
        );

        try (Cursor cursor = database.rawQuery(query, new String[]{uid})) {

            if (cursor.moveToFirst()) {
                VaultFile vaultFile = new VaultFile();
                vaultFile.thumb = cursor.getBlob(cursor.getColumnIndexOrThrow(D.C_THUMBNAIL));
                if (vaultFile.thumb != null) {
                    return vaultFile;
                }
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        }

        return null;
    }

    private VaultFile updateThumbnail(VaultFile vaultFile) {
        if (vaultFile.thumb == null) {
            return vaultFile;
        }

        try {
            ContentValues values = new ContentValues();
            values.put(D.C_THUMBNAIL, vaultFile.thumb);

            database.update(D.T_MEDIA_FILE, values,
                    D.C_ID + "= ?",
                    new String[]{vaultFile.id});
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        }

        return vaultFile;
    }

    private List<VaultFile> getMediaFiles(final Filter filter, final Sort sort) {
        Cursor cursor = null;
        List<VaultFile> mediaFiles = new ArrayList<>();

        String order = (sort == Sort.OLDEST ? "ASC" : "DESC");

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_MEDIA_FILE,
                    new String[]{
                            cn(D.T_MEDIA_FILE, D.C_ID, D.A_MEDIA_FILE_ID),
                            D.C_PATH,
                            D.C_UID,
                            D.C_FILE_NAME,
                            D.C_METADATA,
                            D.C_CREATED,
                            D.C_DURATION,
                            D.C_ANONYMOUS,
                            D.C_SIZE,
                            D.C_HASH},
                    null, null, null,
                    D.C_CREATED + " " + order,
                    null
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                VaultFile vaultFile = cursorToMediaFile(cursor);
              /*  if (mediaFileFilter(vaultFile, filter)) {
                    mediaFiles.add(vaultFile);
                }*/
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return mediaFiles;
    }

    private List<OldMediaFile> getAllOldMediaFiles() {
        Cursor cursor = null;
        List<OldMediaFile> mediaFiles = new ArrayList<>();

        String order = "DESC";

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_MEDIA_FILE,
                    new String[]{
                            cn(D.T_MEDIA_FILE, D.C_ID, D.A_MEDIA_FILE_ID),
                            D.C_PATH,
                            D.C_UID,
                            D.C_FILE_NAME,
                            D.C_METADATA,
                            D.C_CREATED,
                            D.C_DURATION,
                            D.C_ANONYMOUS,
                            D.C_SIZE,
                            D.C_HASH,
                            D.C_THUMBNAIL},
                    null, null, null,
                    D.C_CREATED + " " + order,
                    null
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                OldMediaFile mediaFile = cursorToOldMediaFile(cursor);
                mediaFiles.add(mediaFile);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return mediaFiles;
    }

    private List<VaultFile> getUploadMediaFilesDB(final UploadStatus status) {
        Cursor cursor = null;
        List<VaultFile> vaultFiles = new ArrayList<>();

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_MEDIA_FILE_UPLOAD +
                            " JOIN " + D.T_MEDIA_FILE + " ON " +
                            cn(D.T_MEDIA_FILE_UPLOAD, D.C_MEDIA_FILE_ID) + " = " + cn(D.T_MEDIA_FILE, D.C_ID),
                    new String[]{
                            cn(D.T_MEDIA_FILE_UPLOAD, D.C_MEDIA_FILE_ID, D.A_MEDIA_FILE_ID),
                            cn(D.T_MEDIA_FILE, D.C_PATH, D.C_PATH),
                            cn(D.T_MEDIA_FILE, D.C_UID, D.C_UID),
                            cn(D.T_MEDIA_FILE, D.C_FILE_NAME, D.C_FILE_NAME),
                            cn(D.T_MEDIA_FILE, D.C_METADATA, D.C_METADATA),
                            cn(D.T_MEDIA_FILE, D.C_CREATED, D.C_CREATED),
                            cn(D.T_MEDIA_FILE, D.C_DURATION, D.C_DURATION),
                            cn(D.T_MEDIA_FILE, D.C_ANONYMOUS, D.C_ANONYMOUS),
                            cn(D.T_MEDIA_FILE, D.C_SIZE, D.C_SIZE),
                            cn(D.T_MEDIA_FILE, D.C_HASH, D.C_HASH)},
                    cn(D.T_MEDIA_FILE_UPLOAD, D.C_STATUS) + "=" + status.ordinal(),
                    null, null,
                    cn(D.T_MEDIA_FILE_UPLOAD, D.C_RETRY_COUNT) + " ASC",
                    null
            );
            cursor = database.rawQuery(query, null);
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                VaultFile vaultFile = cursorToMediaFile(cursor);
                vaultFiles.add(vaultFile);
            }

        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return vaultFiles;
    }

    private List<FileUploadBundle> getFileUploadBundlesDB(final UploadStatus status) {
        Cursor cursor = null;
        List<FileUploadBundle> fileUploadBundles = new ArrayList<>();

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_MEDIA_FILE_UPLOAD +
                            " JOIN " + D.T_MEDIA_FILE + " ON " +
                            cn(D.T_MEDIA_FILE_UPLOAD, D.C_MEDIA_FILE_ID) + " = " + cn(D.T_MEDIA_FILE, D.C_ID),
                    new String[]{
                            cn(D.T_MEDIA_FILE_UPLOAD, D.C_MEDIA_FILE_ID, D.A_MEDIA_FILE_ID),
                            cn(D.T_MEDIA_FILE_UPLOAD, D.C_INCLUDE_METADATA, D.C_INCLUDE_METADATA),
                            cn(D.T_MEDIA_FILE_UPLOAD, D.C_SERVER_ID, D.C_SERVER_ID),
                            cn(D.T_MEDIA_FILE_UPLOAD, D.C_MANUAL_UPLOAD, D.C_MANUAL_UPLOAD),
                            cn(D.T_MEDIA_FILE, D.C_PATH, D.C_PATH),
                            cn(D.T_MEDIA_FILE, D.C_UID, D.C_UID),
                            cn(D.T_MEDIA_FILE, D.C_FILE_NAME, D.C_FILE_NAME),
                            cn(D.T_MEDIA_FILE, D.C_METADATA, D.C_METADATA),
                            cn(D.T_MEDIA_FILE, D.C_CREATED, D.C_CREATED),
                            cn(D.T_MEDIA_FILE, D.C_DURATION, D.C_DURATION),
                            cn(D.T_MEDIA_FILE, D.C_ANONYMOUS, D.C_ANONYMOUS),
                            cn(D.T_MEDIA_FILE, D.C_SIZE, D.C_SIZE),
                            cn(D.T_MEDIA_FILE, D.C_HASH, D.C_HASH)},
                    cn(D.T_MEDIA_FILE_UPLOAD, D.C_STATUS) + "=" + status.ordinal(),
                    null, null,
                    cn(D.T_MEDIA_FILE_UPLOAD, D.C_RETRY_COUNT) + " ASC",
                    null
            );
            cursor = database.rawQuery(query, null);
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                FileUploadBundle fileUploadBundle = cursorToFileUplodBundle(cursor);
                fileUploadBundles.add(fileUploadBundle);
            }

        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return fileUploadBundles;
    }

    private FormDef updateCollectFormDef(CollectForm form, FormDef formDef) {
        try {
            ContentValues values = new ContentValues();
            values.put(D.C_FORM_DEF, serializeFormDef(formDef));
            values.put(D.C_UPDATED, 0);
            values.put(D.C_DOWNLOADED, 1);

            database.update(D.T_COLLECT_BLANK_FORM, values,
                    D.C_FORM_ID + "= ? AND " + D.C_VERSION + " = ?",
                    new String[]{form.getForm().getFormID(), form.getForm().getVersion()});
            form.setDownloaded(true);
            form.setUpdated(false);
        } catch (IOException e) {
            Timber.e(e, getClass().getName());
        }

        return formDef;
    }

    private void updateCollectFormDefinition(CollectForm form, FormDef formDef) {
        try {
            ContentValues values = new ContentValues();
            values.put(D.C_FORM_DEF, serializeFormDef(formDef));
            values.put(D.C_UPDATED, 0);
            values.put(D.C_DOWNLOADED, 1);

            database.update(D.T_COLLECT_BLANK_FORM, values,
                    D.C_FORM_ID + "= ? AND " + D.C_VERSION + " = ?",
                    new String[]{form.getForm().getFormID(), form.getForm().getVersion()});
            form.setDownloaded(true);
            form.setUpdated(false);
        } catch (IOException e) {
            Timber.e(e, getClass().getName());
        }
    }

    private List<CollectFormInstance> getDraftCollectFormInstances() {
        return getCollectFormInstances(new CollectFormInstanceStatus[]{
                CollectFormInstanceStatus.UNKNOWN,
                CollectFormInstanceStatus.DRAFT
        });
    }

    private List<CollectFormInstance> getSubmitCollectFormInstances() {
        return getCollectFormInstances(new CollectFormInstanceStatus[]{
                //CollectFormInstanceStatus.FINALIZED,
                CollectFormInstanceStatus.SUBMITTED//,
                //CollectFormInstanceStatus.SUBMISSION_ERROR,
                // CollectFormInstanceStatus.SUBMISSION_PENDING,
                //CollectFormInstanceStatus.SUBMISSION_PARTIAL_PARTS
        });
    }

    private List<CollectFormInstance> getPendingCollectFormInstances() {
        return getCollectFormInstances(new CollectFormInstanceStatus[]{
                CollectFormInstanceStatus.FINALIZED,
                CollectFormInstanceStatus.SUBMISSION_ERROR,
                CollectFormInstanceStatus.SUBMISSION_PENDING,
                CollectFormInstanceStatus.SUBMISSION_PARTIAL_PARTS
        });
    }

    private List<CollectFormInstance> getCollectFormInstances(CollectFormInstanceStatus[] statuses) {
        Cursor cursor = null;
        List<CollectFormInstance> instances = new ArrayList<>();

        List<String> s = new ArrayList<>(statuses.length);
        for (CollectFormInstanceStatus status : statuses) {
            s.add(Integer.toString(status.ordinal()));
        }
        String selection = "(" + TextUtils.join(", ", s) + ")";

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_COLLECT_FORM_INSTANCE +
                            " JOIN " + D.T_COLLECT_SERVER + " ON " +
                            cn(D.T_COLLECT_FORM_INSTANCE, D.C_COLLECT_SERVER_ID) + " = " + cn(D.T_COLLECT_SERVER, D.C_ID),
                    new String[]{
                            cn(D.T_COLLECT_FORM_INSTANCE, D.C_ID, D.A_COLLECT_FORM_INSTANCE_ID),
                            D.C_COLLECT_SERVER_ID,
                            D.C_STATUS,
                            D.C_UPDATED,
                            D.C_FORM_ID,
                            D.C_VERSION,
                            D.C_FORM_NAME,
                            D.C_INSTANCE_NAME,
                            D.C_FORM_DEF,
                            D.C_FORM_PART_STATUS,
                            cn(D.T_COLLECT_SERVER, D.C_NAME, D.A_SERVER_NAME),
                            cn(D.T_COLLECT_SERVER, D.C_USERNAME, D.A_SERVER_USERNAME)},
                    D.C_STATUS + " IN " + selection,
                    null, null,
                    cn(D.T_COLLECT_FORM_INSTANCE, D.C_ID) + " DESC",
                    null
            );
            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                // todo: this is bad, we need to make this not loading everything in loop
                CollectFormInstance instance = cursorToCollectFormInstance(cursor);

                instance.setFormDef(deserializeFormDef(cursor.getBlob(cursor.getColumnIndexOrThrow(D.C_FORM_DEF))));
                //instance.setFormDef(getCollectFormDef(instance.getFormID(),instance.getVersion()));

               /* List<FormMediaFile> mediaFiles = getFormInstanceMediaFilesFromDb(instance.getId());
                for (FormMediaFile mediaFile : mediaFiles) {
                    instance.setWidgetMediaFile(mediaFile.id, mediaFile);
                }*/
                instances.add(instance);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return instances;
    }

    private CollectFormInstance updateCollectFormInstance(CollectFormInstance instance) {
        try {
            int statusOrdinal;
            ContentValues values = new ContentValues();

            if (instance.getId() > 0) {
                values.put(D.C_ID, instance.getId());
            }

            values.put(D.C_COLLECT_SERVER_ID, instance.getServerId());
            values.put(D.C_FORM_ID, instance.getFormID());
            values.put(D.C_VERSION, instance.getVersion());
            values.put(D.C_FORM_NAME, instance.getFormName());
            values.put(D.C_UPDATED, Util.currentTimestamp());
            values.put(D.C_INSTANCE_NAME, instance.getInstanceName());
            values.put(D.C_FORM_PART_STATUS, instance.getFormPartStatus().ordinal());

            if (instance.getStatus() == CollectFormInstanceStatus.UNKNOWN) {
                statusOrdinal = CollectFormInstanceStatus.DRAFT.ordinal();
            } else {
                statusOrdinal = instance.getStatus().ordinal();
            }
            values.put(D.C_STATUS, statusOrdinal);

            if (instance.getFormDef() != null) {
                values.put(D.C_FORM_DEF, serializeFormDef(instance.getFormDef()));
            }

            database.beginTransaction();

            // insert/update form instance
            long id = database.insertWithOnConflict(
                    D.T_COLLECT_FORM_INSTANCE,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE);
            instance.setId(id);

            // clear FormMediaFiles
            database.delete(
                    D.T_COLLECT_FORM_INSTANCE_VAULT_FILE,
                    D.C_COLLECT_FORM_INSTANCE_ID + " = ?",
                    new String[]{Long.toString(id)});

            // insert FormMediaFiles
            List<FormMediaFile> mediaFiles = instance.getWidgetMediaFiles();
            for (FormMediaFile mediaFile : mediaFiles) {
                values = new ContentValues();
                values.put(D.C_COLLECT_FORM_INSTANCE_ID, id);
                values.put(D.C_VAULT_FILE_ID, mediaFile.id);
                values.put(D.C_STATUS, mediaFile.status.ordinal());

                database.insert(D.T_COLLECT_FORM_INSTANCE_VAULT_FILE, null, values);
            }

            database.setTransactionSuccessful();
        } catch (IOException e) {
            Timber.e(e, getClass().getName());
        } finally {
            database.endTransaction();
        }

        return instance;
    }

    private byte[] serializeFormDef(FormDef formDef) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        formDef.writeExternal(dos);
        dos.flush();
        dos.close();

        return bos.toByteArray();
    }

    private FormDef deserializeFormDef(byte[] data) throws DeserializationException, IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bis);
        FormDef formDef = new FormDef();

        formDef.readExternal(dis, ExtUtil.defaultPrototypes());
        dis.close();

        return formDef;
    }

    private CollectServer updateServer(final CollectServer server) {
        ContentValues values = new ContentValues();
        values.put(D.C_NAME, server.getName());
        values.put(D.C_URL, server.getUrl());
        values.put(D.C_USERNAME, server.getUsername());
        values.put(D.C_PASSWORD, server.getPassword());
        values.put(D.C_CHECKED, server.isChecked() ? 1 : 0);

        database.update(D.T_COLLECT_SERVER, values, D.C_ID + "= ?", new String[]{Long.toString(server.getId())});

        return server;
    }

    private TellaReportServer updateTUServer(final TellaReportServer server) {
        ContentValues values = new ContentValues();
        values.put(D.C_NAME, server.getName());
        values.put(D.C_URL, server.getUrl());
        values.put(D.C_USERNAME, server.getUsername());
        values.put(D.C_PASSWORD, server.getPassword());
        values.put(D.C_CHECKED, server.isChecked() ? 1 : 0);
        values.put(D.C_AUTO_UPLOAD, server.isAutoUpload() ? 1 : 0);
        values.put(D.C_AUTO_DELETE, server.isAutoDelete() ? 1 : 0);
        values.put(D.C_BACKGROUND_UPLOAD, server.isActivatedBackgroundUpload() ? 1 : 0);
        values.put(D.C_ACTIVATED_METADATA, server.isActivatedMetadata() ? 1 : 0);

        database.update(D.T_TELLA_UPLOAD_SERVER, values, D.C_ID + "= ?", new String[]{Long.toString(server.getId())});

        return server;
    }

    private void deleteTellaServerAndResourcesDB(long id) {
        database.delete(D.T_TELLA_UPLOAD_SERVER, D.C_ID + " = ?", new String[]{Long.toString(id)});
        database.delete(D.T_RESOURCES, D.C_SERVER_ID + " = ?", new String[]{Long.toString(id)});
        //deleteTable(D.T_REPORT_FILES_UPLOAD);
    }

    private void removeServer(long id) {
        try {
            database.beginTransaction();

            database.delete(D.T_COLLECT_FORM_INSTANCE, D.C_COLLECT_SERVER_ID + " = ?", new String[]{Long.toString(id)});
            database.delete(D.T_COLLECT_SERVER, D.C_ID + " = ?", new String[]{Long.toString(id)});

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public void removeCachedFormInstances() {
        try {
            database.beginTransaction();

            deleteTable(D.T_COLLECT_BLANK_FORM);
            deleteTable(D.T_COLLECT_FORM_INSTANCE);
            deleteTable(D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE);
            //Preferences.setJavarosa3Upgraded(true);

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private void deleteTable(String table) {
        database.execSQL("DELETE FROM " + table);
    }

    public void deleteDatabase() {
        deleteTable(D.T_COLLECT_BLANK_FORM);
        deleteTable(D.T_COLLECT_FORM_INSTANCE);
        deleteTable(D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE);
        //deleteTable(D.T_MEDIA_FILE);
        deleteTable(D.T_COLLECT_SERVER);
        deleteTable(D.T_TELLA_UPLOAD_SERVER);
        deleteTable(D.T_MEDIA_FILE_UPLOAD);
        deleteTable(D.T_UWAZI_BLANK_TEMPLATES);
        deleteTable(D.T_UWAZI_ENTITY_INSTANCES);
        deleteTable(D.T_UWAZI_ENTITY_INSTANCE_VAULT_FILE);
        deleteTable(D.T_UWAZI_SERVER);
        deleteTable(D.T_REPORT_FORM_INSTANCE);
        deleteTable(D.T_REPORT_FILES_UPLOAD);
        deleteTable(D.T_REPORT_INSTANCE_VAULT_FILE);
        deleteTable(D.T_RESOURCES);

        deleteTable(D.T_GOOGLE_DRIVE);
        deleteTable(D.T_GOOGLE_DRIVE_FORM_INSTANCE);
        deleteTable(D.T_GOOGLE_DRIVE_INSTANCE_VAULT_FILE);

        deleteTable(D.T_DROPBOX);
        deleteTable(D.T_DROPBOX_FORM_INSTANCE);
        deleteTable(D.T_DROPBOX_INSTANCE_VAULT_FILE);
    }

    public void deleteFormsAndRelatedTables() {
        //deleteTable(D.T_COLLECT_BLANK_FORM); // only draft and sent forms are to be deleted
        // Delete ODK form instances
        deleteTable(D.T_COLLECT_FORM_INSTANCE);
        deleteTable(D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE);

        // Delete tables related to Uwazi entity instances
        deleteTable(D.T_UWAZI_ENTITY_INSTANCES);
        deleteTable(D.T_UWAZI_ENTITY_INSTANCE_VAULT_FILE);

        // Delete tables related to Tella Report instances
        deleteTable(D.T_REPORT_FORM_INSTANCE);
        deleteTable(D.T_REPORT_FILES_UPLOAD);
        deleteTable(D.T_REPORT_INSTANCE_VAULT_FILE);

        // Delete resources table
        deleteTable(D.T_RESOURCES);

        // Delete tables related to Google Drive instances
        deleteTable(D.T_GOOGLE_DRIVE_FORM_INSTANCE);
        deleteTable(D.T_GOOGLE_DRIVE_INSTANCE_VAULT_FILE);

        // Delete tables related to DropBox instances
        deleteTable(D.T_DROPBOX_FORM_INSTANCE);
        deleteTable(D.T_DROPBOX_INSTANCE_VAULT_FILE);

    }

    private void deleteAllServersDB() {
        deleteTable(D.T_COLLECT_BLANK_FORM);
        deleteTable(D.T_COLLECT_FORM_INSTANCE);
        deleteTable(D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE);
        deleteTable(D.T_COLLECT_SERVER);
        deleteTable(D.T_UWAZI_SERVER);
        deleteTable(D.T_TELLA_UPLOAD_SERVER);
        deleteTable(D.T_RESOURCES);
        deleteTable(D.T_MEDIA_FILE_UPLOAD);
        deleteTable(D.T_GOOGLE_DRIVE);
        deleteTable(D.T_DROPBOX);

    }

    public void deleteMediaFiles() {
        deleteTable(D.T_MEDIA_FILE);
    }

    private CollectServer cursorToCollectServer(Cursor cursor) {
        CollectServer collectServer = new CollectServer();
        collectServer.setId(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID)));
        collectServer.setName(cursor.getString(cursor.getColumnIndexOrThrow(D.C_NAME)));
        collectServer.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(D.C_URL)));
        collectServer.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(D.C_USERNAME)));
        collectServer.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(D.C_PASSWORD)));
        collectServer.setChecked(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_CHECKED)) > 0);

        return collectServer;
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

    private OdkForm cursorToOdkForm(Cursor cursor) {
        OdkForm odkForm = new OdkForm();
        odkForm.setFormID(cursor.getString(cursor.getColumnIndexOrThrow(D.C_FORM_ID)));
        odkForm.setName(cursor.getString(cursor.getColumnIndexOrThrow(D.C_NAME)));
        odkForm.setVersion(cursor.getString(cursor.getColumnIndexOrThrow(D.C_VERSION)));
        odkForm.setHash(cursor.getString(cursor.getColumnIndexOrThrow(D.C_HASH)));
        odkForm.setDownloadUrl(cursor.getString(cursor.getColumnIndexOrThrow(D.C_DOWNLOAD_URL)));

        return odkForm;
    }

    private CollectFormInstance cursorToCollectFormInstance(Cursor cursor) {
        CollectFormInstance instance = new CollectFormInstance();
        instance.setId(cursor.getLong(cursor.getColumnIndexOrThrow(D.A_COLLECT_FORM_INSTANCE_ID)));
        instance.setServerId(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_COLLECT_SERVER_ID)));
        instance.setServerName(cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_NAME)));
        instance.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_USERNAME)));
        int statusOrdinal = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_STATUS));
        instance.setStatus(CollectFormInstanceStatus.values()[statusOrdinal]);
        instance.setUpdated(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_UPDATED)));
        instance.setFormID(cursor.getString(cursor.getColumnIndexOrThrow(D.C_FORM_ID)));
        instance.setVersion(cursor.getString(cursor.getColumnIndexOrThrow(D.C_VERSION)));
        instance.setFormName(cursor.getString(cursor.getColumnIndexOrThrow(D.C_FORM_NAME)));
        instance.setInstanceName(cursor.getString(cursor.getColumnIndexOrThrow(D.C_INSTANCE_NAME)));
        int formPartStatusOrdinal = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_FORM_PART_STATUS));
        instance.setFormPartStatus(FormMediaFileStatus.values()[formPartStatusOrdinal]);
        return instance;
    }

    private OldMediaFile cursorToOldMediaFile(Cursor cursor) {
        String path = cursor.getString(cursor.getColumnIndexOrThrow(D.C_PATH));
        String uid = cursor.getString(cursor.getColumnIndexOrThrow(D.C_UID));
        String fileName = cursor.getString(cursor.getColumnIndexOrThrow(D.C_FILE_NAME));
        MetadataEntity metadataEntity = new Gson().fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_METADATA)), MetadataEntity.class);

        OldMediaFile mediaFile = new OldMediaFile(path, uid, fileName, FileUtil.getOldMediaFileType(fileName));
        mediaFile.setId(cursor.getLong(cursor.getColumnIndexOrThrow(D.A_MEDIA_FILE_ID)));
        mediaFile.setMetadata(new EntityMapper().transform(metadataEntity));
        mediaFile.setCreated(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_CREATED)));
        mediaFile.setDuration(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_DURATION)));
        mediaFile.setSize(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_SIZE)));
        mediaFile.setAnonymous(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_ANONYMOUS)) == 1);
        mediaFile.setHash(cursor.getString(cursor.getColumnIndexOrThrow(D.C_HASH)));
        mediaFile.setThumb(cursor.getBlob(cursor.getColumnIndexOrThrow(D.C_THUMBNAIL)));

        return mediaFile;
    }


    private VaultFile cursorToMediaFile(Cursor cursor) {
        //  String path = cursor.getString(cursor.getColumnIndexOrThrow(D.C_PATH));
        String uid = cursor.getString(cursor.getColumnIndexOrThrow(D.C_UID));
        String fileName = cursor.getString(cursor.getColumnIndexOrThrow(D.C_FILE_NAME));
        MetadataEntity metadataEntity = new Gson().fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_METADATA)), MetadataEntity.class);

        VaultFile vaultFile = new VaultFile();
        vaultFile.id = cursor.getString(cursor.getColumnIndexOrThrow(D.C_UID));
        //  vaultFile.path = path;
        vaultFile.name = fileName;
        vaultFile.metadata = new EntityMapper().transform(metadataEntity);
        vaultFile.created = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_CREATED));
        vaultFile.duration = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_DURATION));
        vaultFile.size = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_SIZE));
        vaultFile.anonymous = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_ANONYMOUS)) == 1;
        vaultFile.hash = cursor.getString(cursor.getColumnIndexOrThrow(D.C_HASH));

        return vaultFile;
    }

    private FileUploadBundle cursorToFileUplodBundle(Cursor cursor) {
        String path = cursor.getString(cursor.getColumnIndexOrThrow(D.C_PATH));
        String uid = cursor.getString(cursor.getColumnIndexOrThrow(D.C_UID));
        String fileName = cursor.getString(cursor.getColumnIndexOrThrow(D.C_FILE_NAME));
        MetadataEntity metadataEntity = new Gson().fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_METADATA)), MetadataEntity.class);

        VaultFile vaultFile = new VaultFile();
        vaultFile.id = cursor.getString(cursor.getColumnIndexOrThrow(D.C_UID));
        vaultFile.path = path;
        vaultFile.name = fileName;
        vaultFile.metadata = new EntityMapper().transform(metadataEntity);
        vaultFile.created = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_CREATED));
        vaultFile.duration = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_DURATION));
        vaultFile.size = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_SIZE));
        vaultFile.anonymous = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_ANONYMOUS)) == 1;
        vaultFile.hash = cursor.getString(cursor.getColumnIndexOrThrow(D.C_HASH));

        FileUploadBundle fileUploadBundle = new FileUploadBundle(vaultFile);

        fileUploadBundle.setIncludeMetadata(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_INCLUDE_METADATA)) == 1);
        fileUploadBundle.setManualUpload(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_MANUAL_UPLOAD)) == 1);
        fileUploadBundle.setServerId(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_SERVER_ID)));

        return fileUploadBundle;
    }

    private FileUploadInstance cursorToFileUploadInstance(Cursor cursor) {
        FileUploadInstance instance = new FileUploadInstance();
        instance.setId(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID)));
        int statusOrdinal = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_STATUS));
        instance.setStatus(UploadStatus.values()[statusOrdinal]);
        instance.setSize(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_SIZE)));
        instance.setUploaded(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_UPLOADED)));
        instance.setUpdated(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_UPDATED)));
        instance.setStarted(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_CREATED)));
        instance.setRetryCount(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_RETRY_COUNT)));
        instance.setSet(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_SET)));

        return instance;
    }

    private String cn(String table, String column) {
        return table + "." + column;
    }

    private String cn(String table, String column, String as) {
        return table + "." + column + " AS " + as;
    }

    private Setting cursorToSetting(Cursor cursor) {
        int intIndex = cursor.getColumnIndexOrThrow(D.C_INT_VALUE),
                stringIndex = cursor.getColumnIndexOrThrow(D.C_TEXT_VALUE);

        Setting setting = new Setting();

        if (!cursor.isNull(intIndex)) {
            setting.intValue = cursor.getInt(intIndex);
        }

        if (!cursor.isNull(stringIndex)) {
            setting.stringValue = cursor.getString(stringIndex);
        }

        return setting;
    }

    public Single<List<FileUploadBundle>> getFileUploadBundles(final UploadStatus status) {
        return Single.fromCallable(() -> getFileUploadBundlesDB(status))
                .compose(applySchedulers());
    }

    @NonNull
    @Override
    public Single<ReportInstance> saveInstance(@NonNull ReportInstance instance) {
        return Single.fromCallable(() -> dataBaseUtils.updateTellaReportsFormInstance(instance, D.T_REPORT_FORM_INSTANCE, D.T_REPORT_INSTANCE_VAULT_FILE))
                .compose(applySchedulers());
    }

    @NonNull
    public Single<FeedbackInstance> saveFeedbackInstance(@NonNull FeedbackInstance instance) {
        return Single.fromCallable(() -> updateTellaFeedbackInstance(instance)).compose(applySchedulers());
    }

    private FeedbackInstance updateTellaFeedbackInstance(FeedbackInstance instance) {
        try {
            int statusOrdinal;
            ContentValues values = new ContentValues();

            if (instance.getId() > 0) {
                values.put(D.C_ID, instance.getId());
            }
            values.put(D.C_DESCRIPTION_TEXT, instance.getText());
            values.put(D.C_UPDATED, Util.currentTimestamp());
            statusOrdinal = instance.getStatus().ordinal();
            values.put(D.C_STATUS, statusOrdinal);
            database.beginTransaction();

            // insert/update feedback instance
            long id = database.insertWithOnConflict(
                    D.T_FEEDBACK,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE);
            instance.setId(id);
            database.setTransactionSuccessful();
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            database.endTransaction();
        }
        return instance;
    }

    @NonNull
    @Override
    public Completable deleteReportInstance(long id) {
        return Completable.fromCallable((Callable<Void>) () -> {
            dataBaseUtils.deleteReportFormInstance(id, D.T_REPORT_FORM_INSTANCE);
            return null;
        }).compose(applyCompletableSchedulers());
    }


    private ReportInstance cursorToReportFormInstance(Cursor cursor) {
        ReportInstance instance = new ReportInstance();
        instance.setId(cursor.getLong(cursor.getColumnIndexOrThrow(D.A_TELLA_UPLOAD_INSTANCE_ID)));
        instance.setServerId(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_REPORT_SERVER_ID)));
        int statusOrdinal = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_STATUS));
        instance.setStatus(EntityStatus.values()[statusOrdinal]);
        instance.setUpdated(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_UPDATED)));
        instance.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TITLE)));
        instance.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(D.C_DESCRIPTION_TEXT)));
        instance.setReportApiId(cursor.getString(cursor.getColumnIndexOrThrow(D.C_REPORT_API_ID)));
        instance.setCurrent(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_CURRENT_UPLOAD)));
        return instance;
    }

    private List<ReportInstance> getDraftReportInstances() {
        return dataBaseUtils.getReportFormInstances(new EntityStatus[]{
                EntityStatus.UNKNOWN,
                EntityStatus.DRAFT
        }, D.T_REPORT_FORM_INSTANCE, D.T_TELLA_UPLOAD_SERVER);
    }

    @NonNull
    @Override
    public Single<FeedbackInstance> getFeedbackDraft() {
        return Single.fromCallable(() -> getFeedBackInstance(FeedbackStatus.DRAFT))
                .compose(applySchedulers());
    }

    @NonNull
    public FeedbackInstance getFeedBackInstance(FeedbackStatus status) {

        Cursor cursor = null;
        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_FEEDBACK,
                    new String[]{D.C_ID, D.C_DESCRIPTION_TEXT, D.C_STATUS, D.C_UPDATED},
                    D.C_STATUS + " = ?",
                    null, null, null, null
            );
            cursor = database.rawQuery(query, new String[]{String.valueOf(status.ordinal())});
            if (cursor.moveToFirst()) {
                return cursorToFeedbackInstance(cursor);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private FeedbackInstance cursorToFeedbackInstance(Cursor cursor) {
        FeedbackInstance feedbackInstance = new FeedbackInstance();
        feedbackInstance.setId(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID)));
        int statusOrdinal = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_STATUS));
        feedbackInstance.setStatus(FeedbackStatus.values()[statusOrdinal]);
        feedbackInstance.setText(cursor.getString(cursor.getColumnIndexOrThrow(D.C_DESCRIPTION_TEXT)));
        return feedbackInstance;
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
        }, D.T_REPORT_FORM_INSTANCE, D.T_TELLA_UPLOAD_SERVER);
    }

    private List<ReportInstance> getSubmittedReportInstances() {

        return dataBaseUtils.getReportFormInstances(new EntityStatus[]{
                EntityStatus.SUBMITTED
        }, D.T_REPORT_FORM_INSTANCE, D.T_TELLA_UPLOAD_SERVER);
    }

    @NonNull
    @Override
    public Completable deleteFeedbackInstance(long id) {
        return Completable.fromCallable((Callable<Void>) () -> {
            deleteFeedbackFormInstance(id);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    private void deleteFeedbackFormInstance(long id) throws NotFountException {
        int count = database.delete(D.T_FEEDBACK, D.C_ID + " = ?", new String[]{Long.toString(id)});
        if (count != 1) {
            throw new NotFountException();
        }
    }

    @NonNull
    @Override
    public Single<List<ReportInstance>> listDraftReportInstances() {
        return Single.fromCallable(this::getDraftReportInstances)
                .compose(applySchedulers());
    }

    @Nullable
    @Override
    public Single<List<FeedbackInstance>> listFeedBackInstances() {
        return Single.fromCallable(this::getFeedBackInstances)
                .compose(applySchedulers());
    }

    private List<FeedbackInstance> getFeedBackInstances() {
        return getFeedbackInstances(new FeedbackStatus[]{
                FeedbackStatus.SUBMISSION_PENDING,
                FeedbackStatus.SUBMISSION_ERROR
        });
    }

    private List<FeedbackInstance> getFeedbackInstances(FeedbackStatus[] statuses) {
        Cursor cursor = null;
        List<FeedbackInstance> instances = new ArrayList<>();

        List<String> statusOrdinalList = new ArrayList<>(statuses.length);
        for (FeedbackStatus status : statuses) {
            statusOrdinalList.add(Integer.toString(status.ordinal()));
        }
        String selection = "(" + TextUtils.join(", ", statusOrdinalList) + ")";

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_FEEDBACK,
                    new String[]{D.C_ID, D.C_DESCRIPTION_TEXT, D.C_STATUS, D.C_UPDATED},
                    D.C_STATUS + " IN " + selection,
                    null, null, cn(D.T_FEEDBACK, D.C_ID) + " DESC", null

            );
            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                FeedbackInstance instance = cursorToFeedbackInstance(cursor);
                instances.add(instance);
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return instances;
    }

    @Nullable
    @Override
    public Single<List<ReportInstance>> listOutboxReportInstances() {
        return Single.fromCallable(this::getOutboxReportInstances)
                .compose(applySchedulers());
    }

    @Nullable
    @Override
    public Single<List<ReportInstance>> listSubmittedReportInstances() {
        return Single.fromCallable(this::getSubmittedReportInstances)
                .compose(applySchedulers());
    }

    @Nullable
    @Override
    public Single<ReportInstanceBundle> getReportBundle(long id) {
        return Single.fromCallable(() -> dataBaseUtils.getReportInstanceBundle(id, D.T_REPORT_FORM_INSTANCE, D.T_REPORT_INSTANCE_VAULT_FILE))
                .compose(applySchedulers());
    }


    @NonNull
    @Override
    public Single<List<ReportInstance>> listAllReportInstances() {
        return null;
    }

    private static class Setting {
        Integer intValue;
        String stringValue;
    }
}