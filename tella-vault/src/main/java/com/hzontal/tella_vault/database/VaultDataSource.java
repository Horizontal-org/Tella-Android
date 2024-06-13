package com.hzontal.tella_vault.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hzontal.tella_vault.IVaultDatabase;
import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.tella_vault.filter.FilterType;
import com.hzontal.tella_vault.filter.Limits;
import com.hzontal.tella_vault.filter.Sort;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteQueryBuilder;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;


/**
 * Provides database operations for managing vault files.
 */
public class VaultDataSource implements IVaultDatabase {
    public static final String ROOT_UID = "11223344-5566-4777-8899-aabbccddeeffe";

    private static VaultDataSource dataSource;
    private static Gson gson;
    private SQLiteDatabase database = null;

    /**
     * Constructs a new VaultDataSource instance.
     *
     * @param context The application context.
     * @param key     The encryption key for the database.
     */
    public VaultDataSource(Context context, byte[] key) {
        System.loadLibrary("sqlcipher");
        VaultSQLiteOpenHelper sqLiteOpenHelper = new VaultSQLiteOpenHelper(context, new DatabaseSecret(key));
        database = sqLiteOpenHelper.getReadableDatabase();
        //  } catch (SQLException e) {
        // Handle potential errors creating the database or opening a writable connection
        //   Log.e("VaultDataSource", "Error creating database connection", e);
        //    // You might throw an exception here or handle it differently based on your app's needs
        //   }
    }

    /**
     * Returns a singleton instance of VaultDataSource.
     *
     * @param context The application context.
     * @param key     The encryption key for the database.
     * @return The VaultDataSource instance.
     */
    public static synchronized VaultDataSource getInstance(Context context, byte[] key) {
        if (dataSource == null) {
            dataSource = new VaultDataSource(context.getApplicationContext(), key);
            gson = new GsonBuilder().create();
        }

        return dataSource;
    }

    /**
     * Retrieves the root vault file.
     *
     * @return The root VaultFile object.
     */
    @Override
    public VaultFile getRootVaultFile() {
        return get(ROOT_UID);
    }

    /**
     * Creates a new vault file.
     *
     * @param parentId  The ID of the parent vault file.
     * @param vaultFile The VaultFile object to create.
     * @return The created VaultFile object.
     */
    @SuppressLint("TimberArgCount")
    @Override
    public VaultFile create(String parentId, VaultFile vaultFile) {
        if (vaultFile.created == 0) {
            vaultFile.created = System.currentTimeMillis();
        }

        try {
            database.beginTransaction();

            ContentValues values = new ContentValues();
            values.put(D.C_ID, vaultFile.id);
            values.put(D.C_TYPE, vaultFile.type.getValue());
            values.put(D.C_PARENT_ID, parentId);
            values.put(D.C_NAME, vaultFile.name);
            values.put(D.C_CREATED, vaultFile.created);
            values.put(D.C_DURATION, vaultFile.duration);
            values.put(D.C_SIZE, vaultFile.size);
            values.put(D.C_ANONYMOUS, vaultFile.anonymous ? 1 : 0);
            values.put(D.C_HASH, vaultFile.hash);
            values.put(D.C_THUMBNAIL, vaultFile.thumb);
            values.put(D.C_MIME_TYPE, vaultFile.mimeType);
            values.put(D.C_PATH, vaultFile.path);
            values.put(D.C_METADATA, gson.toJson(vaultFile.metadata));

            database.insert(D.T_VAULT_FILE, null, values);

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return vaultFile;
    }

    /**
     * Retrieves a list of vault files based on optional parameters.
     *
     * @param parent     The parent VaultFile object.
     * @param filterType The filter type to apply.
     * @param sort       The sort order to apply.
     * @param limits     The limits for pagination.
     * @return A list of VaultFile objects.
     */
    @Override
    public List<VaultFile> list(@Nullable VaultFile parent, @Nullable FilterType filterType, @Nullable Sort sort, @Nullable Limits limits) {
        List<VaultFile> vaultFiles = new ArrayList<>();

        String limit = null;
        String where;
        Cursor cursor = null;

        if (limits != null) {
            limit = String.valueOf(limits.limit);
        }
        //   where = getFilterQuery(filterType, (parent != null ? parent.id : ROOT_UID));
        // Timber.d("where %s", where);
        try {
            // todo: add support for filter directly in query
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
                    null,
                    null,
                    null,
                    getSortQuery(sort),
                    limit
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                vaultFiles.add(cursorToVaultFile(cursor));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return vaultFiles;
    }

    /**
     * Converts a Sort object to the corresponding sort query.
     *
     * @param sort The Sort object.
     * @return The sort query string.
     */
    private String getSortQuery(Sort sort) {
        if (sort == null)
            return D.C_NAME + " " + Sort.Direction.DESC.name();

        if (sort.type.name().equals(Sort.Type.DATE.name()))
            return D.C_CREATED + " " + sort.direction.name();
        else return D.C_NAME + " " + sort.direction.name();
    }

    /**
     * Updates the metadata of a vault file.
     *
     * @param vaultFile The VaultFile object to update.
     * @param metadata  The new Metadata object.
     * @return The updated VaultFile object.
     */
    public VaultFile updateMetadata(VaultFile vaultFile, Metadata metadata) {
        ContentValues values = new ContentValues();

        values.put(D.C_METADATA, gson.toJson(metadata));

        database.update(D.T_VAULT_FILE, values, D.C_ID + " = ?",
                new String[]{vaultFile.id});

        return get(vaultFile.id);
    }

    /**
     * Completes the vault output stream by updating the hash, size, and duration of a vault file.
     *
     * @param vaultFile The VaultFile object to update.
     * @return The updated VaultFile object.
     */
    public VaultFile completeVaultOutputStream(VaultFile vaultFile) {
        ContentValues values = new ContentValues();

        values.put(D.C_HASH, vaultFile.hash);
        values.put(D.C_SIZE, vaultFile.size);
        values.put(D.C_DURATION, vaultFile.duration);

        database.update(D.T_VAULT_FILE, values, D.C_ID + " = ?",
                new String[]{vaultFile.id});

        return get(vaultFile.id);
    }

    /**
     * Retrieves a vault file by its ID.
     *
     * @param id The ID of the vault file.
     * @return The corresponding VaultFile object, or null if not found.
     */
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
                cn(D.C_ID) + " = ?", new String[]{id},
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

    /**
     * Renames a vault file.
     *
     * @param id   The ID of the vault file to rename.
     * @param name The new name for the vault file.
     * @return The updated VaultFile object.
     */
    @Override
    public VaultFile rename(String id, String name) {
        ContentValues values = new ContentValues();

        values.put(D.C_NAME, name);

        database.update(D.T_VAULT_FILE, values, D.C_ID + " = ?",
                new String[]{id});

        return get(id);
    }

    /**
     * Moves a vault file to a new parent.
     *
     * @param vaultFile The VaultFile object to move.
     * @param newParent The ID of the new parent.
     * @return True if the move operation is successful, false otherwise.
     */
    @Override
    public boolean move(VaultFile vaultFile, String newParent) {
        ContentValues values = new ContentValues();
        values.put(D.C_PARENT_ID, newParent);

        int count = database.update(D.T_VAULT_FILE, values, D.C_ID + " = ?",
                new String[]{vaultFile.id});

        return count == 1;
    }

    /**
     * Retrieves a list of vault files by their IDs.
     *
     * @param ids The array of vault file IDs.
     * @return A list of VaultFile objects.
     */
    @Override
    public List<VaultFile> get(String[] ids) {
        List<VaultFile> files = new ArrayList<>();

        try {for (String id : ids) {
                files.add(get(id));
            }
            return files;
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        }
        return null;
    }

    /**
     * Deletes a vault file.
     *
     * @param vaultFile The VaultFile object to delete.
     * @param deleter   The IVaultFileDeleter implementation for performing the deletion.
     * @return True if the delete operation is successful, false otherwise.
     */
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

    /**
     * Destroys the VaultDataSource instance by deleting all records from the vault file table.
     */
    @Override
    public void destroy() {
        deleteTable(D.T_VAULT_FILE);
    }

    /**
     * Converts a database cursor to a VaultFile object.
     *
     * @param cursor The database cursor.
     * @return The corresponding VaultFile object.
     */
    @SuppressLint("TimberArgCount")
    private VaultFile cursorToVaultFile(Cursor cursor) {
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
        vaultFile.metadata = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_METADATA)), Metadata.class);

        return vaultFile;
    }

    public List<VaultFile> listFilesInRoot(String rootId) {
        List<VaultFile> files = new ArrayList<>();
        String where = D.C_PARENT_ID + " = ?";
        String[] whereArgs = new String[]{rootId};
        Cursor cursor = null;
        try {
            cursor = database.query(
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
                    where,
                    whereArgs,
                    null,
                    null,
                    null
            );
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                files.add(cursorToVaultFile(cursor));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return files;
    }

    public boolean transferFilesToNewRoot(String oldRootId, String newRootId) {
        List<VaultFile> files = listFilesInRoot(oldRootId);
       Log.d("VaultDataSource", ""+files.size());

        boolean success = true;
        for (VaultFile file : files) {
            if (!move(file, newRootId)) {
                success = false;
                break;
            }
        }
        return success;
    }


    /**
     * Constructs a column name with a table alias.
     *
     * @param column The column name.
     * @return The column name with the table alias.
     */
    private String cn(String column) {
        return D.T_VAULT_FILE + "." + column;
    }

    /**
     * Constructs a filter query based on the filter type and parent ID.
     *
     * @param filter   The filter type.
     * @param parentId The parent ID.
     * @return The filter query string.
     */
    private String getFilterQuery(FilterType filter, String parentId) {
        if (filter == null)
            return cn(D.C_PARENT_ID) + " = '" + parentId + "'";

        switch (filter) {
            case AUDIO:
                return cn(D.C_MIME_TYPE) + " LIKE '" + "audio/%" + "'";
            case PDF:
                return cn(D.C_MIME_TYPE) + " LIKE '" + "application/pdf" + "'";
            case VIDEO:
                return cn(D.C_MIME_TYPE) + " LIKE '" + "video/%" + "'";
            case PHOTO:
                return cn(D.C_MIME_TYPE) + " LIKE '" + "image/%" + "'";
            case OTHERS:
                return cn(D.C_MIME_TYPE) + " NOT LIKE '" + "audio/%" + "'"
                        + cn(D.C_MIME_TYPE) + " NOT LIKE '" + "video/%" + "'"
                        + cn(D.C_MIME_TYPE) + " NOT LIKE '" + "image/%" + "'"
                        + cn(D.C_MIME_TYPE) + " NOT LIKE '" + "text/%" + "'"
                        ;
            case AUDIO_VIDEO:
                return cn(D.C_MIME_TYPE) + " LIKE '" + "audio/%" + "' OR " + cn(D.C_MIME_TYPE) + " LIKE '" + "video/%" + "'";
            case DOCUMENTS:
                return cn(D.C_MIME_TYPE) + " LIKE '" + "text/%" + "' OR " + cn(D.C_MIME_TYPE) + " IN " +
                        "(" + "'application/pdf'" + ","
                        + "'application/msword'" + ","
                        + "'application/vnd.ms-excel'" + ""
                        + "'application/mspowerpoint'" + ""
                        + "'audio/flac'" + ""
                        + "'application/zip'" + ""
                        + ")";
            case ALL_WITHOUT_DIRECTORY:
                return cn(D.C_TYPE) + " != '" + VaultFile.Type.DIRECTORY.getValue() + "' AND " + cn(D.C_MIME_TYPE) + " NOT LIKE '" + "resource/%" + "'";
            case PHOTO_VIDEO:
                return cn(D.C_MIME_TYPE) + " LIKE '" + "image/%" + "' OR " + cn(D.C_MIME_TYPE) + " LIKE '" + "video/%" + "'";
            default:
                return cn(D.C_PARENT_ID) + " = '" + parentId + "'";

        }
    }

    /**
     * Deletes all records from a table except the root directory.
     *
     * @param table The table name.
     */
    private void deleteTable(String table) {
        database.delete(table, D.C_ID + " != '" + ROOT_UID + "'", null);
    }
}
