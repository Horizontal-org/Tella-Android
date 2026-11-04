package org.horizontal.tella.mobile.data.database

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class DataSourceDeleteTableTest {
    private val database = mock(SupportSQLiteDatabase::class.java)
    private val cursor = mock(Cursor::class.java)

    private val quickDeleteTables = listOf(
        D.T_COLLECT_BLANK_FORM,
        D.T_COLLECT_FORM_INSTANCE,
        D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE,
        D.T_COLLECT_SERVER,
        D.T_TELLA_UPLOAD_SERVER,
        D.T_MEDIA_FILE_UPLOAD,
        D.T_UWAZI_BLANK_TEMPLATES,
        D.T_UWAZI_ENTITY_INSTANCES,
        D.T_UWAZI_ENTITY_INSTANCE_VAULT_FILE,
        D.T_UWAZI_SERVER,
        D.T_REPORT_FORM_INSTANCE,
        D.T_REPORT_FILES_UPLOAD,
        D.T_REPORT_INSTANCE_VAULT_FILE,
        D.T_RESOURCES,
        D.T_GOOGLE_DRIVE,
        D.T_GOOGLE_DRIVE_FORM_INSTANCE,
        D.T_GOOGLE_DRIVE_INSTANCE_VAULT_FILE,
        D.T_DROPBOX,
        D.T_DROPBOX_FORM_INSTANCE,
        D.T_DROPBOX_INSTANCE_VAULT_FILE,
        D.T_NEXT_CLOUD,
        D.T_NEXT_CLOUD_FORM_INSTANCE,
        D.T_NEXT_CLOUD_INSTANCE_VAULT_FILE,
    )

    @Test
    fun `missing flavor table is skipped`() {
        stubTable(D.T_GOOGLE_DRIVE, false)

        DataSource.deleteTable(database, D.T_GOOGLE_DRIVE)

        verify(database, never()).execSQL("DELETE FROM ${D.T_GOOGLE_DRIVE}")
        verify(cursor).close()
    }

    @Test
    fun `existing table is emptied`() {
        stubTable(D.T_NEXT_CLOUD, true)

        DataSource.deleteTable(database, D.T_NEXT_CLOUD)

        verify(database).execSQL("DELETE FROM ${D.T_NEXT_CLOUD}")
        verify(cursor).close()
    }

    @Test
    fun `all quick delete tables are emptied when present`() {
        quickDeleteTables.forEach { stubTable(it, true) }

        quickDeleteTables.forEach { DataSource.deleteTable(database, it) }

        quickDeleteTables.forEach { verify(database).execSQL("DELETE FROM $it") }
    }

    private fun stubTable(table: String, exists: Boolean) {
        doReturn(cursor).`when`(database).query(TABLE_EXISTS_QUERY, arrayOf<Any>(table))
        `when`(cursor.moveToFirst()).thenReturn(exists)
    }

    private companion object {
        const val TABLE_EXISTS_QUERY =
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1"
    }
}
