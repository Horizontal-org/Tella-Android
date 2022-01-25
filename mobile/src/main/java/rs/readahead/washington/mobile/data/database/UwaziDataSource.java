package rs.readahead.washington.mobile.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteQueryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.CompletableTransformer;
import io.reactivex.MaybeTransformer;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntityRow;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer;
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate;
import rs.readahead.washington.mobile.domain.entity.uwazi.ListTemplateResult;
import rs.readahead.washington.mobile.domain.exception.NotFountException;
import rs.readahead.washington.mobile.domain.repository.IUWAZIServersRepository;
import rs.readahead.washington.mobile.domain.repository.uwazi.ICollectUwaziTemplatesRepository;
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
        SQLiteDatabase.loadLibs(context);
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
    public Single<List<CollectTemplate>> listFavoriteTemplates() {
        return null;
    }

    @Override
    public Single<ListTemplateResult> updateBlankTemplates(ListTemplateResult listTemplateResult) {
        return Single.fromCallable(() -> {
            dataSource.updateUBlankTemplates(listTemplateResult);
            listTemplateResult.setTemplates(dataSource.getBlankCollectTemplates());
            return listTemplateResult;
        }).compose(applySchedulers());
    }

    @Override
    public Single<ListTemplateResult> updateBlankTemplatesIfNeeded(ListTemplateResult listTemplateResult) {
        return Single.fromCallable(() -> {
            dataSource.updateBlankTemplatesIfNeeded(listTemplateResult);
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
    public Completable deleteTemplate(final long id) {
        return Completable.fromCallable((Callable<Void>) () -> {
            deleteCollectFormInstance(id);
            return null;
        }).compose(applyCompletableSchedulers());
    }

    //TODO ask djodje what isChecked state for ?
    private List<UWaziUploadServer> getUwaziServers() {
        Cursor cursor = null;
        List<UWaziUploadServer> servers = new ArrayList<>();

        try {
            cursor = database.query(
                    D.T_UWAZI_SERVER,
                    new String[]{D.C_ID, D.C_NAME, D.C_URL, D.C_USERNAME, D.C_PASSWORD, D.C_COOKIES},
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

    private UWaziUploadServer cursorToUwaziServer(Cursor cursor) {
        UWaziUploadServer server = new UWaziUploadServer();
        server.setId(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID)));
        server.setName(cursor.getString(cursor.getColumnIndexOrThrow(D.C_NAME)));
        server.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(D.C_URL)));
        server.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(D.C_USERNAME)));
        server.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(D.C_PASSWORD)));
        server.setCookies(cursor.getString(cursor.getColumnIndexOrThrow(D.C_COOKIES)));

        return server;
    }

    private UWaziUploadServer createUZIServer(final UWaziUploadServer server) {
        ContentValues values = new ContentValues();
        values.put(D.C_NAME, server.getName());
        values.put(D.C_URL, server.getUrl());
        values.put(D.C_USERNAME, server.getUsername());
        values.put(D.C_PASSWORD, server.getPassword());
        values.put(D.C_COOKIES, server.getCookies());

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
        values.put(D.C_COOKIES, server.getCookies());

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
                UwaziEntityRow entity =  gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TEMPLATE_ENTITY)),UwaziEntityRow.class);
             //   long id = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID));
                long serverId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_UWAZI_SERVER_ID));
                boolean downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_DOWNLOADED)) == 1;
                boolean favorite = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_FAVORITE)) == 1;
                boolean updated = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_UPDATED)) == 1;
                String serverName = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_NAME));
                String username = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_USERNAME));

                CollectTemplate collectForm = new CollectTemplate(0, serverId, serverName, username, entity, downloaded, favorite, updated);
                //TODO CHECK THIS
                collectForm.setId(0);
                collectForm.setServerName(serverName);
                collectForm.setUsername(username);
                collectForm.setDownloaded(downloaded);
                collectForm.setFavorite(favorite);
                collectForm.setUpdated(updated);

                templates.add(collectForm);
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
                UwaziEntityRow entity =  gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TEMPLATE_ENTITY)),new TypeToken<UwaziEntityRow>(){}.getType());

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

    private void updateUBlankTemplates(ListTemplateResult result) {

        List<CollectTemplate> templates = result.getTemplates();
        List<IErrorBundle> errors = result.getErrors();

        List<String> templatesIDs = new ArrayList<>(templates.size());
        List<String> errorServerIDs = new ArrayList<>(errors.size());

        for (CollectTemplate template : templates) {

            CollectTemplate current = getBlankTemplate("" + template.getId());
            ContentValues values = new ContentValues();

            if (current != null) {

                values.put(D.C_UPDATED, true);

                int num = database.update(D.T_UWAZI_BLANK_TEMPLATES, values, D.C_ID + " = ?",
                        new String[]{Long.toString(current.getId())});
                if (num > 0) {
                    template.setUpdated(true);
                }
            } else {
                values.put(D.C_UWAZI_SERVER_ID, template.getServerId());
                values.put(D.C_VERSION, template.getEntityRow().get__v());
                values.put(D.C_NAME, template.getEntityRow().getName());

                long id = database.insert(D.T_UWAZI_BLANK_TEMPLATES, null, values);
                if (id != -1) {
                    template.setId(id);
                }
            }
        }

    }

    private void updateUBlankTemplatesIfNeeded(ListTemplateResult result) {

        List<CollectTemplate> templates = result.getTemplates();
        List<IErrorBundle> errors = result.getErrors();

        List<String> templatesIDs = new ArrayList<>(templates.size());
        List<String> errorServerIDs = new ArrayList<>(errors.size());

        for (CollectTemplate template : templates) {

            CollectTemplate current = getBlankTemplate("" + template.getId());
            ContentValues values = new ContentValues();

            if (current != null) {

                values.put(D.C_UPDATED, true);

                int num = database.update(D.T_UWAZI_BLANK_TEMPLATES, values, D.C_ID + " = ?",
                        new String[]{Long.toString(current.getId())});
                if (num > 0) {
                    template.setUpdated(true);
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
                UwaziEntityRow entity =  gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TEMPLATE_ENTITY)),new TypeToken<UwaziEntityRow>(){}.getType());


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
        Gson gson = new Gson();

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
        Gson gson = new Gson();
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

    private void deleteCollectFormInstance(long id) throws NotFountException {
        int count = database.delete(D.T_UWAZI_BLANK_TEMPLATES, D.C_ID + " = ?", new String[]{Long.toString(id)});

        if (count != 1) {
            throw new NotFountException();
        }
    }

    private String cn(String table, String column) {
        return table + "." + column;
    }

    private String cn(String table, String column, String as) {
        return table + "." + column + " AS " + as;
    }
}
