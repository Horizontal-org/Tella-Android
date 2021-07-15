package com.hzontal.tella_vault.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.Nullable;

import com.google.gson.GsonBuilder;
import com.hzontal.data.MetadataEntity;
import com.hzontal.mappers.EntityMapper;
import com.hzontal.tella_vault.IVaultDatabase;
import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.VaultFile;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteQueryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import timber.log.Timber;


public class VaultDataSource implements IVaultDatabase {
    public static final int ROOT_ID = 1;
    public static final String ROOT_UID = UUID.randomUUID().toString();

    private static VaultDataSource dataSource;
    private final SQLiteDatabase database;

    public static synchronized VaultDataSource getInstance(Context context, byte[] key) {
        if (dataSource == null) {
            dataSource = new VaultDataSource(context.getApplicationContext(), key);
        }

        return dataSource;
    }

    public VaultDataSource(Context context, byte[] key) {
        VaultSQLiteOpenHelper sqLiteOpenHelper = new VaultSQLiteOpenHelper(context);
        SQLiteDatabase.loadLibs(context);

        database = sqLiteOpenHelper.getWritableDatabase(key);
    }

    @Override
    public VaultFile getRootVaultFile() {
        return get(ROOT_UID);
    }

    @SuppressLint("TimberArgCount")
    @Override
    public VaultFile create(VaultFile vaultFile) {
        if (vaultFile.created == 0) {
            vaultFile.created = System.currentTimeMillis();
        }

        try {
            database.beginTransaction();
            // todo: get parent id for vaultFile.parent
            ContentValues values = new ContentValues();
            values.put(D.C_ID, vaultFile.id);
            values.put(D.C_TYPE, vaultFile.type.getValue());
            values.put(D.C_PARENT_ID, (long) VaultDataSource.ROOT_ID);
            values.put(D.C_NAME, vaultFile.name);
            values.put(D.C_CREATED, vaultFile.created);
            values.put(D.C_DURATION, vaultFile.duration);
            values.put(D.C_SIZE, vaultFile.size);
            values.put(D.C_ANONYMOUS, vaultFile.anonymous ? 1 : 0);
            values.put(D.C_HASH, vaultFile.hash);
            values.put(D.C_THUMBNAIL, vaultFile.thumb);
            values.put(D.C_MIME_TYPE, vaultFile.mimeType);
            values.put(D.C_PATH, vaultFile.path);
            values.put(D.C_METADATA, new GsonBuilder().create().toJson(new EntityMapper().transform(vaultFile.metadata)));

            database.insert(D.T_VAULT_FILE, null, values);

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        Timber.d("VaultFile", vaultFile.toString());
        return vaultFile;
    }

    @Override
    public List<VaultFile> list(VaultFile parent, @Nullable Filter filter, @Nullable Sort sort, @Nullable Limits limits) {
        List<VaultFile> vaultFiles = new ArrayList<>();
        Sort.Direction direction = Sort.Direction.ASC;
        String limit = null;
        Cursor cursor = null;

        if (sort != null) {
            direction = sort.direction;
        }

        if (limits != null) {
            limit = String.valueOf(limits.limit);
        }

        //TODO: CHECK WHERE THE PARENT IS APPLIED
        try {
            // todo: add safe where clause if parent != null
            // todo: add support for limit
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_VAULT_FILE,
                    new String[]{
                            D.C_ID,
                            D.C_TYPE,
                            D.C_PARENT_ID,
                            D.C_NAME,
                            D.C_CREATED,
                            D.C_DURATION,
                            D.C_SIZE,
                            D.C_HASH,
                            D.C_ANONYMOUS,
                            D.C_THUMBNAIL,
                            D.C_MIME_TYPE,
                            D.C_PATH,
                            D.C_METADATA
                    },
                    null, null, null,
                    D.C_CREATED + " " + direction, limit
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                VaultFile vaultFile = cursorToVaultFile(cursor);
                if (filter.applyFilter(vaultFile)) {
                    vaultFiles.add(cursorToVaultFile(cursor));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return vaultFiles;
    }

    @Override
    public VaultFile update(VaultFile vaultFile) {
        ContentValues values = new ContentValues();
        values.put(D.C_METADATA, new GsonBuilder().create().toJson(new EntityMapper().transform(vaultFile.metadata)));
        values.put(D.C_TYPE, vaultFile.type.getValue());
        values.put(D.C_PARENT_ID, (long) VaultDataSource.ROOT_ID);
        values.put(D.C_NAME, vaultFile.name);
        values.put(D.C_ANONYMOUS, vaultFile.anonymous ? 1 : 0);
        values.put(D.C_HASH, vaultFile.hash);
        values.put(D.C_PATH, vaultFile.path);
        database.update(D.T_VAULT_FILE, values, D.C_ID + " = ?",
                new String[]{vaultFile.id});

        return get(vaultFile.id);
    }

    @Override
    public VaultFile get(String id) {
        try (Cursor cursor = database.query(
                D.T_VAULT_FILE,
                new String[]{
                        D.C_ID,
                        D.C_PATH,
                        D.C_NAME,
                        D.C_METADATA,
                        D.C_CREATED,
                        D.C_DURATION,
                        D.C_ANONYMOUS,
                        D.C_SIZE,
                        D.C_HASH,
                        D.C_MIME_TYPE,
                        D.C_TYPE,
                        D.C_THUMBNAIL
                },
                cn(D.T_VAULT_FILE, D.C_ID) + " = ?",
                new String[]{id},
                null, null, null, null
        )) {

            if (cursor.moveToFirst()) {
                return cursorToVaultFile(cursor);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        }
        return null;
    }

    @Override
    public List<VaultFile> get(String[] ids) {
        return null;
    }

    @Override
    public boolean delete(VaultFile vaultFile, IVaultFileDeleter deleter) {
        try {
            database.beginTransaction();

            int count = database.delete(D.T_VAULT_FILE, D.C_ID + " = ?", new String[]{vaultFile.id});

            if (count != 1) {
                return false;
            }

            if (deleter.delete(vaultFile)) {
                database.setTransactionSuccessful();
            }

            return true;
        } finally {
            database.endTransaction();
        }
    }

    @Override
    public void destroy() {
    }

    @SuppressLint("TimberArgCount")
    private VaultFile cursorToVaultFile(Cursor cursor) {
        MetadataEntity metadataEntity = new GsonBuilder().create().fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_METADATA)), MetadataEntity.class);
        VaultFile vaultFile = new VaultFile();
        vaultFile.id = cursor.getString(cursor.getColumnIndexOrThrow(D.C_ID));
        vaultFile.type = VaultFile.Type.fromValue(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_TYPE)));
        vaultFile.name = cursor.getString(cursor.getColumnIndexOrThrow(D.C_NAME));
        vaultFile.created = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_CREATED));
        vaultFile.duration = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_DURATION));
        vaultFile.size = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_SIZE));
        vaultFile.anonymous = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_ANONYMOUS)) == 1;
        vaultFile.hash = cursor.getString(cursor.getColumnIndexOrThrow(D.C_HASH));
        vaultFile.thumb = cursor.getBlob(cursor.getColumnIndexOrThrow(D.C_THUMBNAIL));
        vaultFile.mimeType = cursor.getString(cursor.getColumnIndexOrThrow(D.C_MIME_TYPE));
        vaultFile.path = cursor.getString(cursor.getColumnIndexOrThrow(D.C_PATH));
        vaultFile.metadata = new EntityMapper().transform(metadataEntity);
        return vaultFile;
    }

    private String cn(String table, String column) {
        return table + "." + column;
    }

    private String cn(String table, String column, String as) {
        return table + "." + column + " AS " + as;
    }
}
