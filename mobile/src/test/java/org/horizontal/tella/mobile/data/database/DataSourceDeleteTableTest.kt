package org.horizontal.tella.mobile.data.database

import net.zetetic.database.sqlcipher.SQLiteDatabase
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import android.database.Cursor

class DataSourceDeleteTableTest {
    private val database = mock(SQLiteDatabase::class.java)
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
        `when`(database.rawQuery(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(cursor)
        `when`(cursor.moveToFirst()).thenReturn(false)

        DataSource.deleteTable(database, D.T_GOOGLE_DRIVE)

        verify(database, never()).execSQL("DELETE FROM ${D.T_GOOGLE_DRIVE}")
        verify(cursor).close()
    }

    @Test
    fun `existing table is emptied`() {
        `when`(database.rawQuery(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(cursor)
        `when`(cursor.moveToFirst()).thenReturn(true)

        DataSource.deleteTable(database, D.T_NEXT_CLOUD)

        verify(database).execSQL("DELETE FROM ${D.T_NEXT_CLOUD}")
        verify(cursor).close()
    }

    @Test
    fun `all quick delete tables are emptied when present`() {
        `when`(database.rawQuery(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(cursor)
        `when`(cursor.moveToFirst()).thenReturn(true)

        quickDeleteTables.forEach { DataSource.deleteTable(database, it) }

        quickDeleteTables.forEach { verify(database).execSQL("DELETE FROM $it") }
    }

    private fun anyString(): String = org.mockito.ArgumentMatchers.anyString()
}
