package rs.readahead.washington.mobile.data.database

import android.content.ContentValues
import net.zetetic.database.sqlcipher.SQLiteDatabase
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.util.Util
import timber.log.Timber


open class DataBaseUtils(private val database: SQLiteDatabase) {
    protected fun updateTellaReportsFormInstance(instance: ReportInstance,reportTable: String, vaultTable:String): ReportInstance? {
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

}