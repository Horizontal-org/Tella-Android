package rs.readahead.washington.mobile.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;


import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteQueryBuilder;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.CompletableTransformer;
import io.reactivex.MaybeTransformer;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.domain.entity.EntityStatus;
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus;
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate;
import rs.readahead.washington.mobile.domain.entity.uwazi.EntityInstanceBundle;
import rs.readahead.washington.mobile.domain.entity.uwazi.ListTemplateResult;
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance;
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziRow;
import rs.readahead.washington.mobile.domain.exception.NotFountException;
import rs.readahead.washington.mobile.domain.repository.IUWAZIServersRepository;
import rs.readahead.washington.mobile.domain.repository.uwazi.ICollectUwaziTemplatesRepository;
import rs.readahead.washington.mobile.util.Util;
import timber.log.Timber;

public class UwaziDataSource implements IUWAZIServersRepository, ICollectUwaziTemplatesRepository {

    private static UwaziDataSource dataSource;
    private final SQLiteDatabase database;

    final private SingleTransformer schedulersTransformer =
            observable -> observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());

    final private CompletableTransformer schedulersCompletableTransformer =
            observable -> observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());

    final private MaybeTransformer schedulersMaybeTransformer =
            observable -> observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());

    private UwaziDataSource(Context context, byte[] key) {
        WashingtonSQLiteOpenHelper sqLiteOpenHelper = new WashingtonSQLiteOpenHelper(context);
        //SQLiteDatabase.loadLibs(context);
        database = sqLiteOpenHelper.getWritableDatabase(key);
    }

    public static synchronized UwaziDataSource getInstance(Context context, byte[] key) {
        if (dataSource == null) {
            dataSource = new UwaziDataSource(context.getApplicationContext(), key);
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

    @Override
    public Single<EntityInstanceBundle> getBundle(final long id) {
        return Single.fromCallable(() -> getEntityInstanceBundle(id))
                .compose(applySchedulers());
    }

    @Override
    public Single<List<UWaziUploadServer>> listUwaziServers() {
        return Single.fromCallable(() -> dataSource.getUwaziServers())
                .compose(applySchedulers());
    }

    @Override
    public Single<UWaziUploadServer> createUWAZIServer(UWaziUploadServer server) {
        return Single.fromCallable(() -> dataSource.createUZIServer(server))
                .compose(applySchedulers());
    }

    @Override
    public Completable removeUwaziServer(long id) {
        return Completable.fromCallable((Callable<Void>) () -> {
            removeUzServer(id);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @Override
    public Single<UWaziUploadServer> updateUwaziServer(UWaziUploadServer server) {
        return Single.fromCallable(() -> dataSource.updateUzServer(server))
                .compose(applySchedulers());
    }

    @Override
    public Single<Long> countUwaziServers() {
        return Single.fromCallable(() -> dataSource.countDBUwaziServers())
                .compose(applySchedulers());
    }

    @Override
    public Single<List<CollectTemplate>> listBlankTemplates() {
        return Single.fromCallable(() -> dataSource.getBlankCollectTemplates());
    }

    @Override
    public Single<List<UwaziEntityInstance>> listDraftInstances() {
        return Single.fromCallable(this::getDraftUwaziEntityInstances)
                .compose(applySchedulers());
    }

    @Override
    public Single<List<UwaziEntityInstance>> listOutboxInstances() {
        return Single.fromCallable(this::getOutboxUwaziEntityInstances)
                .compose(applySchedulers());
    }

    @Override
    public Single<List<UwaziEntityInstance>> listSubmittedInstances() {
        return Single.fromCallable(this::getSubmittedUwaziEntityInstances)
                .compose(applySchedulers());
    }

    private List<UwaziEntityInstance> getDraftUwaziEntityInstances() {
        return getUwaziEntityInstances(new EntityStatus[]{
                EntityStatus.UNKNOWN,
                EntityStatus.DRAFT
        });
    }

    private List<UwaziEntityInstance> getSubmittedUwaziEntityInstances() {
        return getUwaziEntityInstances(new EntityStatus[]{
                EntityStatus.SUBMITTED
        });
    }

    private List<UwaziEntityInstance> getOutboxUwaziEntityInstances() {
        return getUwaziEntityInstances(new EntityStatus[]{
                EntityStatus.FINALIZED,
                EntityStatus.SUBMISSION_ERROR,
                EntityStatus.SUBMISSION_PENDING,
                EntityStatus.SUBMISSION_PARTIAL_PARTS
        });
    }

    @Override
    public Single<List<CollectTemplate>> listFavoriteTemplates() {
        return Single.fromCallable(() -> dataSource.getFavoriteCollectTemplates())
                .compose(applySchedulers());
    }

    @Override
    public Single<ListTemplateResult> updateBlankTemplates(ListTemplateResult listTemplateResult) {
        return Single.fromCallable(() -> {
            dataSource.updateUBlankTemplates(listTemplateResult,dataSource.getBlankCollectTemplates());
            listTemplateResult.setTemplates(dataSource.getBlankCollectTemplates());
            return listTemplateResult;
        }).compose(applySchedulers());
    }

    @Override
    public Single<ListTemplateResult> updateBlankTemplatesIfNeeded(ListTemplateResult listTemplateResult) {
        return Single.fromCallable(() -> {
            dataSource.updateUBlankTemplates(listTemplateResult,dataSource.getBlankCollectTemplates());
            listTemplateResult.setTemplates(dataSource.getBlankCollectTemplatesAndUpdate(listTemplateResult));
            return listTemplateResult;
        }).compose(applySchedulers());
    }

    @Override
    public Single<CollectTemplate> updateBlankTemplate(CollectTemplate template) {
        return Single.fromCallable(() -> {
            dataSource.updateUBlankTemplateIfNeeded(template);
            return template;
        }).compose(applySchedulers());
    }

    @Override
    public Single<CollectTemplate> saveBlankTemplate(CollectTemplate template) {
        return Single.fromCallable(() -> dataSource.saveUBlankTemplate(template)).compose(applySchedulers());
    }

    @Override
    public Single<UwaziEntityInstance> saveEntityInstance(UwaziEntityInstance instance) {
        return Single.fromCallable(() -> dataSource.updateEntityInstanceDB(instance)).compose(applySchedulers());
    }

    @Override
    public Single<CollectTemplate> getBlankCollectTemplateById(String templateID) {
        return Single.fromCallable(() -> dataSource.getBlankTemplate(templateID))
                .compose(applySchedulers());
    }

    @Override
    public Single<UWaziUploadServer> getUwaziServerById(Long serverID) {
        return Single.fromCallable(() -> dataSource.getUwaziServer(serverID))
                .compose(applySchedulers());
    }

    @Override
    public Completable deleteTemplate(final long id) {
        return Completable.fromCallable((Callable<Void>) () -> {
            deleteCollectFormInstance(id);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @Override
    public Completable deleteEntityInstance(final long id) {
        return Completable.fromCallable((Callable<Void>) () -> {
            deleteUwaziEntityInstance(id);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    @Override
    public Single<CollectTemplate> toggleFavorite(CollectTemplate template) {
        return Single.fromCallable(() -> dataSource.toggleFavoriteCollectTemplate(template))
                .compose(applySchedulers());
    }

    //TODO ask djodje what isChecked state for ?
    private List<UWaziUploadServer> getUwaziServers() {
        Cursor cursor = null;
        List<UWaziUploadServer> servers = new ArrayList<>();

        try {
            cursor = database.query(
                    D.T_UWAZI_SERVER,
                    new String[]{D.C_ID, D.C_NAME, D.C_URL, D.C_USERNAME, D.C_PASSWORD, D.C_CONNECT_COOKIES, D.C_LOCALE_COOKIES},
                    null,
                    null,
                    null, null,
                    D.C_ID + " ASC",
                    null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                UWaziUploadServer server = cursorToUwaziServer(cursor);
                servers.add(server);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return servers;
    }

    private UWaziUploadServer getUwaziServer(Long serverId) {
        Cursor cursor = null;

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_UWAZI_SERVER,
                    new String[]{D.C_ID, D.C_NAME, D.C_URL, D.C_USERNAME, D.C_PASSWORD, D.C_CONNECT_COOKIES, D.C_LOCALE_COOKIES},
                    D.C_ID + " = ?",
                    null, null, null, null
            );

            cursor = database.rawQuery(query, new String[]{Long.toString(serverId)});

            if (cursor.moveToFirst()) {
                return cursorToUwaziServer(cursor);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    private UWaziUploadServer cursorToUwaziServer(Cursor cursor) {
        UWaziUploadServer server = new UWaziUploadServer();
        server.setId(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID)));
        server.setName(cursor.getString(cursor.getColumnIndexOrThrow(D.C_NAME)));
        server.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(D.C_URL)));
        server.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(D.C_USERNAME)));
        server.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(D.C_PASSWORD)));
        server.setConnectCookie(cursor.getString(cursor.getColumnIndexOrThrow(D.C_CONNECT_COOKIES)));
        server.setLocaleCookie(cursor.getString(cursor.getColumnIndexOrThrow(D.C_LOCALE_COOKIES)));

        return server;
    }

    private UWaziUploadServer createUZIServer(final UWaziUploadServer server) {
        ContentValues values = new ContentValues();
        values.put(D.C_NAME, server.getName());
        values.put(D.C_URL, server.getUrl());
        values.put(D.C_USERNAME, server.getUsername());
        values.put(D.C_PASSWORD, server.getPassword());
        values.put(D.C_LOCALE_COOKIES, server.getLocaleCookie());
        values.put(D.C_CONNECT_COOKIES, server.getConnectCookie());
        server.setId(database.insert(D.T_UWAZI_SERVER, null, values));

        return server;
    }

    private void removeUzServer(long id) {
        database.delete(D.T_UWAZI_SERVER, D.C_ID + " = ?", new String[]{Long.toString(id)});
    }

    private UWaziUploadServer updateUzServer(final UWaziUploadServer server) {
        ContentValues values = new ContentValues();
        values.put(D.C_NAME, server.getName());
        values.put(D.C_URL, server.getUrl());
        values.put(D.C_USERNAME, server.getUsername());
        values.put(D.C_PASSWORD, server.getPassword());
        values.put(D.C_CONNECT_COOKIES, server.getConnectCookie());
        values.put(D.C_LOCALE_COOKIES, server.getLocaleCookie());
        database.update(D.T_UWAZI_SERVER, values, D.C_ID + "= ?", new String[]{Long.toString(server.getId())});

        return server;
    }

    private long countDBUwaziServers() {
        return net.sqlcipher.DatabaseUtils.queryNumEntries(database, D.T_UWAZI_SERVER);
    }

    private List<CollectTemplate> getBlankCollectTemplates() {
        Cursor cursor = null;
        List<CollectTemplate> templates = new ArrayList<>();
        Gson gson = new Gson();

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_UWAZI_BLANK_TEMPLATES + " JOIN " + D.T_UWAZI_SERVER + " ON " +
                            D.T_UWAZI_BLANK_TEMPLATES + "." + D.C_UWAZI_SERVER_ID + " = " + D.T_UWAZI_SERVER + "." + D.C_ID,
                    new String[]{
                            cn(D.T_UWAZI_BLANK_TEMPLATES, D.C_ID),
                            D.C_UWAZI_SERVER_ID,
                            D.C_TEMPLATE_ENTITY,
                            D.C_DOWNLOADED,
                            D.C_UPDATED,
                            D.C_FAVORITE,
                            D.C_DOWNLOAD_URL,
                            cn(D.T_UWAZI_SERVER, D.C_NAME, D.A_SERVER_NAME),
                            cn(D.T_UWAZI_SERVER, D.C_USERNAME, D.A_SERVER_USERNAME)},
                    null, null, null,
                    cn(D.T_UWAZI_BLANK_TEMPLATES, D.C_FAVORITE) + " DESC, " + cn(D.T_UWAZI_BLANK_TEMPLATES, D.C_ID) + " DESC",
                    null
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                UwaziRow entity = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TEMPLATE_ENTITY)), UwaziRow.class);
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID));
                long serverId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_UWAZI_SERVER_ID));
                boolean downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_DOWNLOADED)) == 1;
                boolean favorite = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_FAVORITE)) == 1;
                boolean updated = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_UPDATED)) == 1;
                String serverName = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_NAME));
                String username = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_USERNAME));

                CollectTemplate collectTemplate = new CollectTemplate(id, serverId, serverName, username, entity, downloaded, favorite, updated);


                templates.add(collectTemplate);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return templates;
    }

    private List<CollectTemplate> getBlankCollectTemplatesAndUpdate(ListTemplateResult result) {
        Cursor cursor = null;
        List<CollectTemplate> templates = new ArrayList<>();
        List<CollectTemplate> resultTemplates = result.getTemplates();
        Gson gson = new Gson();
        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_UWAZI_BLANK_TEMPLATES + " JOIN " + D.T_UWAZI_SERVER + " ON " +
                            D.T_UWAZI_BLANK_TEMPLATES + "." + D.C_UWAZI_SERVER_ID + " = " + D.T_UWAZI_SERVER + "." + D.C_ID,
                    new String[]{
                            cn(D.T_UWAZI_BLANK_TEMPLATES, D.C_ID, D.A_COLLECT_BLANK_FORM_ID),
                            D.C_UWAZI_SERVER_ID,
                            D.C_TEMPLATE_ENTITY,
                            D.C_DOWNLOADED,
                            D.C_UPDATED,
                            D.C_FAVORITE,
                            D.C_DOWNLOAD_URL,
                            cn(D.T_UWAZI_SERVER, D.C_NAME, D.A_SERVER_NAME),
                            cn(D.T_UWAZI_SERVER, D.C_USERNAME, D.A_SERVER_USERNAME)},
                    null, null, null,
                    cn(D.T_UWAZI_BLANK_TEMPLATES, D.C_FAVORITE) + " DESC, " + cn(D.T_UWAZI_BLANK_TEMPLATES, D.C_ID) + " DESC",
                    null
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                UwaziRow entity = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TEMPLATE_ENTITY)), new TypeToken<UwaziRow>() {
                }.getType());

                // todo: implement cursorToCollectForm
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(D.A_COLLECT_BLANK_FORM_ID));
                long serverId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_UWAZI_SERVER_ID));
                boolean downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_DOWNLOADED)) == 1;
                boolean favorite = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_FAVORITE)) == 1;
                boolean updated = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_UPDATED)) == 1;
                String serverName = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_NAME));
                String username = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_USERNAME));
                CollectTemplate collectTemplate = new CollectTemplate(id, serverId, serverName, username, entity, downloaded, favorite, updated);
                templates.add(collectTemplate);
            }

            resultTemplates.replaceAll
                    (oldTemplate ->
                            templates.stream()
                                    .filter(template -> template.getEntityRow().get_id().equals(oldTemplate.getEntityRow().get_id()))
                                    .findFirst()
                                    .orElse(oldTemplate)
                    );


        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return resultTemplates;

    }

    private void updateUBlankTemplates(ListTemplateResult result,List<CollectTemplate> oldList) {

        List<CollectTemplate> templates = result.getTemplates();

        for (CollectTemplate template : templates) {
            ContentValues values = new ContentValues();
            for (CollectTemplate oldTemplate : oldList){
                if (oldTemplate.getEntityRow().get_id().equals(template.getEntityRow().get_id())){
                    values.put(D.C_UWAZI_SERVER_ID, template.getServerId());
                    values.put(D.C_TEMPLATE_ENTITY, new GsonBuilder().create().toJson(template.getEntityRow()));
                    values.put(D.C_DOWNLOADED, true);
                    values.put(D.C_UPDATED, true);

                    int num = database.update(D.T_UWAZI_BLANK_TEMPLATES, values, D.C_ID + " = ?",
                            new String[]{Long.toString(oldTemplate.getId())});
                    if (num > 0) {
                        template.setUpdated(true);
                    }
                }
            }
        }

    }


    @Nullable
    private CollectTemplate getBlankTemplate(String templateID) {
        Cursor cursor = null;
        Gson gson = new Gson();

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_UWAZI_BLANK_TEMPLATES + " JOIN " + D.T_UWAZI_SERVER + " ON " +
                            D.T_UWAZI_BLANK_TEMPLATES + "." + D.C_UWAZI_SERVER_ID + " = " + D.T_UWAZI_SERVER + "." + D.C_ID,
                    new String[]{
                            cn(D.T_UWAZI_BLANK_TEMPLATES, D.C_ID, D.A_COLLECT_BLANK_FORM_ID),
                            D.C_UWAZI_SERVER_ID,
                            D.C_TEMPLATE_ENTITY,
                            D.C_DOWNLOADED,
                            D.C_FAVORITE,
                            D.C_UPDATED,
                            cn(D.T_UWAZI_SERVER, D.C_NAME, D.A_SERVER_NAME),
                            cn(D.T_UWAZI_SERVER, D.C_USERNAME, D.A_SERVER_USERNAME)},
                    D.C_FORM_ID + " = ?",
                    null, null, null, null
            );

            cursor = database.rawQuery(query, new String[]{templateID});

            if (cursor.moveToFirst()) {

                long id = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID));
                long serverId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_COLLECT_SERVER_ID));
                boolean downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_DOWNLOADED)) == 1;
                boolean favorite = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_FAVORITE)) == 1;
                boolean updated = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_UPDATED)) == 1;
                String serverName = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_NAME));
                String username = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_USERNAME));
                UwaziRow entity = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TEMPLATE_ENTITY)), new TypeToken<UwaziRow>() {
                }.getType());

                return new CollectTemplate(id, serverId, serverName, username, entity, downloaded, favorite, updated);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    private void updateUBlankTemplateIfNeeded(CollectTemplate collectTemplate) {

        CollectTemplate current = getBlankTemplate(String.valueOf(collectTemplate.getId()));
        ContentValues values = new ContentValues();

        if (current != null) {
            values.put(D.C_UPDATED, true);
            values.put(D.C_TEMPLATE_ENTITY, new GsonBuilder().create().toJson(collectTemplate.getEntityRow()));
            int num = database.update(D.T_UWAZI_BLANK_TEMPLATES, values, D.C_ID + " = ?",
                    new String[]{Long.toString(current.getId())});
            if (num > 0) {
                collectTemplate.setUpdated(true);
            }
        }
    }

    private CollectTemplate saveUBlankTemplate(CollectTemplate template) {
        ContentValues values = new ContentValues();
        values.put(D.C_UWAZI_SERVER_ID, template.getServerId());
        values.put(D.C_TEMPLATE_ENTITY, new GsonBuilder().create().toJson(template.getEntityRow()));
        values.put(D.C_DOWNLOADED, true);
        values.put(D.C_UPDATED, true);
        long id = database.insert(D.T_UWAZI_BLANK_TEMPLATES, null, values);
        if (id != -1) {
            template.setId(id);
            template.setUpdated(true);
            template.setDownloaded(true);
        }
        return template;
    }

    private UwaziEntityInstance   updateEntityInstanceDB(UwaziEntityInstance instance) {
        try {
            ContentValues values = new ContentValues();
            long updated = Util.currentTimestamp();
            int statusOrdinal;

            if (instance.getId() > 0) {
                values.put(D.C_ID, instance.getId());
            }

            values.put(D.C_UWAZI_SERVER_ID, instance.getCollectTemplate().getServerId());
            values.put(D.C_TEMPLATE_ENTITY, new GsonBuilder().create().toJson(instance.getCollectTemplate()));
            values.put(D.C_METADATA, new GsonBuilder().create().toJson(instance.getMetadata()));
            values.put(D.C_STATUS, instance.getStatus().ordinal());
            values.put(D.C_UPDATED, updated);
            values.put(D.C_TEMPLATE, instance.getTemplate());
            values.put(D.C_TITLE, instance.getTitle());
            values.put(D.C_TYPE, instance.getType());
            values.put(D.C_FORM_PART_STATUS, instance.getFormPartStatus().ordinal());
            instance.setUpdated(updated);

            if (instance.getStatus() == EntityStatus.UNKNOWN) {
                statusOrdinal =  EntityStatus.DRAFT.ordinal();
            } else {
                statusOrdinal = instance.getStatus().ordinal();
            }
            values.put(D.C_STATUS, statusOrdinal);

            database.beginTransaction();

            // insert/update form instance
            long id = database.insertWithOnConflict(
                    D.T_UWAZI_ENTITY_INSTANCES,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE);
            instance.setId(id);

            // clear FormMediaFiles
            database.delete(
                    D.T_UWAZI_ENTITY_INSTANCE_VAULT_FILE,
                    D.C_UWAZI_ENTITY_INSTANCE_ID + " = ?",
                    new String[]{Long.toString(id)});

            // insert FormMediaFiles
            List<FormMediaFile> mediaFiles = instance.getWidgetMediaFiles();
            for (FormMediaFile mediaFile : mediaFiles) {
                values = new ContentValues();
                values.put(D.C_UWAZI_ENTITY_INSTANCE_ID, id);
                values.put(D.C_VAULT_FILE_ID, mediaFile.id);
                values.put(D.C_STATUS, mediaFile.status.ordinal());

                database.insert(D.T_UWAZI_ENTITY_INSTANCE_VAULT_FILE, null, values);
            }

            database.setTransactionSuccessful();

        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            database.endTransaction();
        }

        return instance;
    }

    private void deleteUwaziEntityInstance(long id) throws NotFountException {
        int count = database.delete(D.T_UWAZI_ENTITY_INSTANCES, D.C_ID + " = ?", new String[]{Long.toString(id)});

        if (count != 1) {
            throw new NotFountException();
        }
    }

    private EntityInstanceBundle getEntityInstanceBundle(long id) {
        Cursor cursor = null;
        Gson gson = new Gson();
        EntityInstanceBundle bundle = new EntityInstanceBundle();

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_UWAZI_ENTITY_INSTANCES,
                    new String[]{
                            cn(D.T_UWAZI_ENTITY_INSTANCES, D.C_ID, D.A_UWAZI_ENTITY_INSTANCE_ID),
                            D.C_UWAZI_SERVER_ID,
                            D.C_TEMPLATE_ENTITY,
                            D.C_METADATA,
                            D.C_STATUS,
                            D.C_UPDATED,
                            D.C_TEMPLATE,
                            D.C_TITLE,
                            D.C_TYPE,
                            D.C_FORM_PART_STATUS},
                    cn(D.T_UWAZI_ENTITY_INSTANCES, D.C_ID) + "= ?",
                    null, null, null, null
            );

            cursor = database.rawQuery(query, new String[]{Long.toString(id)});

            if (cursor.moveToFirst()) {
                UwaziEntityInstance instance = cursorToUwaziEntityInstance(cursor);

                CollectTemplate collectTemplate = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TEMPLATE_ENTITY)), CollectTemplate.class);
                instance.setCollectTemplate(collectTemplate);
                Map<String,ArrayList<Object>> metadata = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_METADATA)), new TypeToken<Map<String,ArrayList<Object>>>() {}.getType());
                instance.setMetadata(metadata);
                bundle.setInstance(instance);

                List<String> vaultFileIds = getEntityInstanceFileIds(instance.getId());
                String[] iDs = new String[vaultFileIds.size()];
                vaultFileIds.toArray(iDs);
                bundle.setFileIds(iDs);

                return bundle;
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return new EntityInstanceBundle();
    }

    private List<String> getEntityInstanceFileIds(long instanceId) {
        List<String> ids = new ArrayList<>();
        Cursor cursor = null;
        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_UWAZI_ENTITY_INSTANCE_VAULT_FILE,
                    new String[]{
                            D.C_VAULT_FILE_ID},
                    D.C_UWAZI_ENTITY_INSTANCE_ID + "= ?",
                    null, null, D.C_VAULT_FILE_ID + " DESC", null
            );
            cursor = database.rawQuery(query, new String[]{Long.toString(instanceId)});
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                String vaultFileId = cursor.getString(cursor.getColumnIndexOrThrow(D.C_VAULT_FILE_ID));
                ids.add(vaultFileId);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return ids;
    }

    private List<UwaziEntityInstance> getUwaziEntityInstances(EntityStatus[] statuses) {
        Gson gson = new Gson();
        Cursor cursor = null;
        List<UwaziEntityInstance> instances = new ArrayList<>();

        List<String> s = new ArrayList<>(statuses.length);
        for (EntityStatus status : statuses) {
            s.add(Integer.toString(status.ordinal()));
        }
        String selection = "(" + TextUtils.join(", ", s) + ")";

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_UWAZI_ENTITY_INSTANCES +
                            " JOIN " + D.T_UWAZI_SERVER + " ON " +
                            cn(D.T_UWAZI_ENTITY_INSTANCES, D.C_UWAZI_SERVER_ID) + " = " + cn(D.T_UWAZI_SERVER, D.C_ID),
                    new String[]{
                            cn(D.T_UWAZI_ENTITY_INSTANCES, D.C_ID, D.A_UWAZI_ENTITY_INSTANCE_ID),
                            D.C_UWAZI_SERVER_ID,
                            D.C_STATUS,
                            D.C_UPDATED,
                            D.C_TEMPLATE_ENTITY,
                            D.C_METADATA,
                            D.C_TEMPLATE,
                            D.C_TITLE,
                            D.C_TYPE,
                            D.C_FORM_PART_STATUS,
                            cn(D.T_UWAZI_SERVER, D.C_NAME, D.A_SERVER_NAME),
                            cn(D.T_UWAZI_SERVER, D.C_USERNAME, D.A_SERVER_USERNAME)},
                    D.C_STATUS + " IN " + selection,
                    null, null,
                    cn(D.T_UWAZI_ENTITY_INSTANCES, D.C_ID) + " DESC",
                    null
            );
            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                // todo: this is bad, we need to make this not loading everything in loop
                UwaziEntityInstance instance = cursorToUwaziEntityInstance(cursor);
                CollectTemplate collectTemplate = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TEMPLATE_ENTITY)), CollectTemplate.class);
                instance.setCollectTemplate(collectTemplate);
                Map<String,ArrayList<Object>> metadata = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_METADATA)), new TypeToken<Map<String,ArrayList<Object>>>() {}.getType());
                instance.setMetadata(metadata);

                instances.add(instance);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return instances;
    }

    private UwaziEntityInstance cursorToUwaziEntityInstance(Cursor cursor) {
        UwaziEntityInstance instance = new UwaziEntityInstance();
        instance.setId(cursor.getLong(cursor.getColumnIndexOrThrow(D.A_UWAZI_ENTITY_INSTANCE_ID)));
        /*instance.setServerId(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_UWAZI_SERVER_ID)));
        instance.setServerName(cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_NAME)));
        instance.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_USERNAME)));*/
        int statusOrdinal = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_STATUS));
        instance.setStatus(EntityStatus.values()[statusOrdinal]);
        int formPartStatusOrdinal = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_FORM_PART_STATUS));
        instance.setFormPartStatus(FormMediaFileStatus.values()[formPartStatusOrdinal]);
        instance.setUpdated(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_UPDATED)));
        instance.setTemplate(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TEMPLATE)));
        instance.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TITLE)));
        instance.setType(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TYPE)));

        return instance;
    }

    private void deleteCollectFormInstance(long id) throws NotFountException {
        int count = database.delete(D.T_UWAZI_BLANK_TEMPLATES, D.C_ID + " = ?", new String[]{Long.toString(id)});

        if (count != 1) {
            throw new NotFountException();
        }
    }

    private List<CollectTemplate> getFavoriteCollectTemplates() {
        Cursor cursor = null;
        List<CollectTemplate> templates = new ArrayList<>();
        Gson gson = new Gson();

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_UWAZI_BLANK_TEMPLATES + " JOIN " + D.T_UWAZI_SERVER + " ON " +
                            D.T_UWAZI_BLANK_TEMPLATES + "." + D.C_UWAZI_SERVER_ID + " = " + D.T_UWAZI_SERVER + "." + D.C_ID,
                    new String[]{
                            cn(D.T_UWAZI_BLANK_TEMPLATES, D.C_ID),
                            D.C_UWAZI_SERVER_ID,
                            D.C_TEMPLATE_ENTITY,
                            D.C_DOWNLOADED,
                            D.C_UPDATED,
                            D.C_FAVORITE,
                            D.C_DOWNLOAD_URL,
                            cn(D.T_UWAZI_SERVER, D.C_NAME, D.A_SERVER_NAME),
                            cn(D.T_UWAZI_SERVER, D.C_USERNAME, D.A_SERVER_USERNAME)},
                    D.C_FAVORITE + " =1 ", null, null,
                    cn(D.T_UWAZI_BLANK_TEMPLATES, D.C_FAVORITE) + " DESC, " + cn(D.T_UWAZI_BLANK_TEMPLATES, D.C_ID) + " DESC",
                    null
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                UwaziRow entity = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TEMPLATE_ENTITY)), UwaziRow.class);
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID));
                long serverId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_UWAZI_SERVER_ID));
                boolean downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_DOWNLOADED)) == 1;
                boolean favorite = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_FAVORITE)) == 1;
                boolean updated = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_UPDATED)) == 1;
                String serverName = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_NAME));
                String username = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_USERNAME));

                CollectTemplate collectTemplate = new CollectTemplate(id, serverId, serverName, username, entity, downloaded, favorite, updated);


                templates.add(collectTemplate);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return templates;
    }

    private CollectTemplate toggleFavoriteCollectTemplate(CollectTemplate template) {
        ContentValues values = new ContentValues();
        values.put(D.C_FAVORITE, !template.isFavorite());

        int num = database.update(D.T_UWAZI_BLANK_TEMPLATES, values, D.C_ID + "= ?", new String[]{Long.toString(template.getId())});
        if (num > 0) {
            template.setFavorite(!template.isFavorite());
        }

        return template;
    }

    private String cn(String table, String column) {
        return table + "." + column;
    }

    private String cn(String table, String column, String as) {
        return table + "." + column + " AS " + as;
    }
}
