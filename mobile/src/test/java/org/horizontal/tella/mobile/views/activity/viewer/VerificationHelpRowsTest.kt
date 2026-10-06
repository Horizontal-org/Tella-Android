package org.horizontal.tella.mobile.views.activity.viewer

import org.horizontal.tella.mobile.R
import org.junit.Assert.assertEquals
import org.junit.Test

class VerificationHelpRowsTest {

    @Test
    fun fileHelp_matchesVerificationFileFields() {
        val rows = VerificationHelpRows.rows(VerificationCategory.FILE)
        assertEquals(R.string.verification_help_file_bar, VerificationHelpRows.titleRes(VerificationCategory.FILE))
        assertEquals(5, rows.size)
        assertEquals(R.string.verification_info_field_filename, rows[0].labelRes)
        assertEquals(R.string.verification_info_field_file_path, rows[1].labelRes)
        assertEquals(R.string.verification_info_field_file_created, rows[3].labelRes)
        assertEquals(R.string.verification_info_field_file_modified, rows[4].labelRes)
    }

    @Test
    fun deviceHelp_matchesVerificationDeviceFields() {
        val rows = VerificationHelpRows.rows(VerificationCategory.DEVICE)
        assertEquals(R.string.verification_help_device_bar, VerificationHelpRows.titleRes(VerificationCategory.DEVICE))
        assertEquals(7, rows.size)
        assertEquals(R.string.verification_info_field_manufacturer, rows[0].labelRes)
        assertEquals(R.string.verification_info_field_locale, rows[6].labelRes)
    }

    @Test
    fun networkHelp_includesConnectionAndWifi() {
        val rows = VerificationHelpRows.rows(VerificationCategory.NETWORK)
        assertEquals(7, rows.size)
        assertEquals(R.string.verification_info_field_connection_status, rows[0].labelRes)
        assertEquals(R.string.verification_info_wifi, rows[6].labelRes)
    }

    @Test
    fun locationHelp_includesProviderAndCoordinates() {
        val rows = VerificationHelpRows.rows(VerificationCategory.LOCATION)
        assertEquals(7, rows.size)
        assertEquals(R.string.verification_info_field_location_provider, rows[0].labelRes)
        assertEquals(R.string.verification_info_field_location_time_value, rows[6].labelRes)
    }

    @Test
    fun otherHelp_includesTemperatureAndLight() {
        val rows = VerificationHelpRows.rows(VerificationCategory.OTHER)
        assertEquals(2, rows.size)
        assertEquals(R.string.verification_info_field_ambient_temperature, rows[0].labelRes)
        assertEquals(R.string.verification_info_field_light, rows[1].labelRes)
    }
}
