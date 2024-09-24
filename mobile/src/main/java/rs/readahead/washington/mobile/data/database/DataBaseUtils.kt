package rs.readahead.washington.mobile.data.database

import android.content.ContentValues
import android.database.Cursor
import android.text.TextUtils
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteQueryBuilder
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.domain.exception.NotFountException
import rs.readahead.washington.mobile.util.Util
import timber.log.Timber


open class DataBaseUtils(private val database: SQLiteDatabase) {
    protected fun updateTellaReportsFormInstance(
        instance: ReportInstance,
        reportTable: String,
        vaultTable: String
    ): ReportInstance? {
        try {
            var values = ContentValues()
            if (instance.id > 0) {
                values.put(D.C_ID, instance.id)
            }
            values.put(D.C_REPORT_SERVER_ID, instance.serverId)
            values.put(D.C_TITLE, instance.title)
            values.put(D.C_DESCRIPTION_TEXT, instance.description)
            values.put(D.C_UPDATED, Util.currentTimestamp())
            values.put(D.C_CURRENT_UPLOAD, instance.current)
            values.put(D.C_REPORT_API_ID, instance.reportApiId)

            //TODO CHECK FILES IMPLEMENTATION AND ADD FILES STATUS
            values.put(D.C_FORM_PART_STATUS, instance.formPartStatus.ordinal)
            val statusOrdinal: Int = if (instance.status == EntityStatus.UNKNOWN) {
                EntityStatus.DRAFT.ordinal
            } else {
                instance.status.ordinal
            }
            values.put(D.C_STATUS, statusOrdinal)
            database.beginTransaction()

            // insert/update form instance
            val id: Long = database.insertWithOnConflict(
                reportTable,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            instance.id = id

            // clear FormMediaFiles
            database.delete(
                vaultTable,
                D.C_REPORT_INSTANCE_ID + " = ?", arrayOf<String>(java.lang.Long.toString(id))
            )

            // insert FormMediaFiles
            val mediaFiles = instance.widgetMediaFiles
            for (mediaFile in mediaFiles) {
                values = ContentValues()
                values.put(D.C_REPORT_INSTANCE_ID, id)
                values.put(D.C_VAULT_FILE_ID, mediaFile.id)
                values.put(D.C_STATUS, mediaFile.status.ordinal)
                values.put(D.C_UPLOADED_SIZE, mediaFile.uploadedSize)
                database.insert(vaultTable, null, values)
            }
            database.setTransactionSuccessful()
        } catch (e: Exception) {
            Timber.e(e, javaClass.name)
        } finally {
            database.endTransaction()
        }
        return instance
    }


    protected fun getReportFormInstances(statuses: Array<EntityStatus>,reportTable: String,
                                         serverTable: String): List<ReportInstance>? {
        var cursor: Cursor? = null
        val instances: MutableList<ReportInstance> = ArrayList()
        val s: MutableList<String?> = ArrayList(statuses.size)
        for (status: EntityStatus in statuses) {
            s.add(Integer.toString(status.ordinal))
        }
        val selection = "(" + TextUtils.join(", ", s) + ")"
        try {
            val query = SQLiteQueryBuilder.buildQueryString(
                false,
                reportTable +
                        " JOIN " + serverTable + " ON " +
                        cn(
                            reportTable,
                            D.C_REPORT_SERVER_ID
                        ) + " = " + cn(serverTable, D.C_ID), arrayOf<String>(
                    cn(reportTable, D.C_ID, D.A_TELLA_UPLOAD_INSTANCE_ID),
                    D.C_REPORT_SERVER_ID,
                    D.C_STATUS,
                    D.C_UPDATED,
                    D.C_CURRENT_UPLOAD,  //   D.C_METADATA,
                    D.C_DESCRIPTION_TEXT,
                    D.C_TITLE,
                    D.C_REPORT_API_ID,  //  D.C_FORM_PART_STATUS,
                    cn(serverTable, D.C_NAME, D.A_SERVER_NAME),
                    cn(serverTable, D.C_USERNAME, D.A_SERVER_USERNAME)
                ),
                D.C_STATUS + " IN " + selection,
                null, null,
                cn(reportTable, D.C_ID) + " DESC",
                null
            )
            cursor = database.rawQuery(query, null)
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val instance: ReportInstance = cursorToReportFormInstance(cursor)
                instances.add(instance)
                cursor.moveToNext()
            }
        } catch (e: java.lang.Exception) {
            Timber.e(e, javaClass.name)
        } finally {
            cursor?.close()
        }
        return instances
    }

    private fun cursorToReportFormInstance(cursor: Cursor): ReportInstance {
        val instance = ReportInstance()
        instance.id = cursor.getLong(cursor.getColumnIndexOrThrow(D.A_TELLA_UPLOAD_INSTANCE_ID))
        instance.serverId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_REPORT_SERVER_ID))
        val statusOrdinal = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_STATUS))
        instance.status = EntityStatus.entries[statusOrdinal]
        instance.updated = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_UPDATED))
        instance.title = cursor.getString(cursor.getColumnIndexOrThrow(D.C_TITLE))
        instance.description = cursor.getString(cursor.getColumnIndexOrThrow(D.C_DESCRIPTION_TEXT))
        instance.reportApiId = cursor.getString(cursor.getColumnIndexOrThrow(D.C_REPORT_API_ID))
        instance.current = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_CURRENT_UPLOAD))
        return instance
    }

    private fun cn(table: String, column: String): String {
        return "$table.$column"
    }

    private fun cn(table: String, column: String, cast: String): String {
        return "$table.$column AS $cast"
    }

    @Throws(NotFountException::class)
    protected fun deleteReportFormInstance(id: Long,reportTable: String) {
        val count = database.delete(
            reportTable,
            D.C_ID + " = ?",
            arrayOf(java.lang.Long.toString(id))
        )
        if (count != 1) {
            throw NotFountException()
        }
    }
}