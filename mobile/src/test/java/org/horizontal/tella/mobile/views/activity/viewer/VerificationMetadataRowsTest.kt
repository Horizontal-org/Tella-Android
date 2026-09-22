package org.horizontal.tella.mobile.views.activity.viewer

import com.hzontal.tella_vault.Metadata
import com.hzontal.tella_vault.MyLocation
import com.hzontal.tella_vault.VaultFile
import org.horizontal.tella.mobile.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificationMetadataRowsTest {

    @Test
    fun fileRows_useNameHashPathAndTimestamps() {
        val vaultFile = vaultFileWithMetadata()
        vaultFile.name = "clip.mp4"
        vaultFile.hash = "abc123"
        vaultFile.created = 1_520_000_000_000L

        val rows = VerificationMetadataRows.rows(
            vaultFile,
            VerificationCategory.FILE,
            "/video"
        )

        assertEquals(R.string.verification_info_field_filename, rows[0].labelRes)
        assertEquals("clip.mp4", rows[0].value)
        assertEquals(R.string.verification_info_field_file_path, rows[1].labelRes)
        assertEquals("/video", rows[1].value)
        assertEquals(R.string.verification_info_field_hash, rows[2].labelRes)
        assertEquals("abc123", rows[2].value)
        assertEquals(R.string.verification_info_field_file_created, rows[3].labelRes)
        assertTrue(rows[3].value!!.isNotBlank())
        assertEquals(R.string.verification_info_field_file_modified, rows[4].labelRes)
        assertEquals(rows[3].value, rows[4].value)
        assertEquals(5, rows.size)
    }

    @Test
    fun fileRows_pathIsRootWhenLocationMissing() {
        val vaultFile = VaultFile()
        vaultFile.name = "photo.jpg"

        val rows = VerificationMetadataRows.rows(vaultFile, VerificationCategory.FILE)

        assertEquals("/", rows[1].value)
    }

    @Test
    fun deviceRows_mapHardwareAndIdentifiers() {
        val metadata = Metadata()
        metadata.hardware = "Google Pixel"
        metadata.manufacturer = "Google"
        metadata.deviceID = "device-id"
        metadata.wifiMac = "ac:37:43:4f:35:a7"
        metadata.screenSize = "4.72"
        val vaultFile = VaultFile()
        vaultFile.metadata = metadata

        val rows = VerificationMetadataRows.rows(vaultFile, VerificationCategory.DEVICE)

        assertEquals("Google", rows[0].value)
        assertEquals("Google Pixel", rows[1].value)
        assertEquals("device-id", rows[2].value)
        assertEquals("4.72\"", rows[3].value)
        assertEquals("ac:37:43:4f:35:a7", rows[4].value)
    }

    @Test
    fun networkRows_includeConnectionAndAddresses() {
        val metadata = Metadata()
        metadata.network = "Connected"
        metadata.networkType = "Mobile Data LTE"
        metadata.dataType = "Mobile Data LTE"
        metadata.cells = listOf("{\"cellId\":1}")
        metadata.setIPv4("192.168.0.61")
        metadata.setIPv6("FE80::1")
        val vaultFile = VaultFile()
        vaultFile.metadata = metadata

        val rows = VerificationMetadataRows.rows(vaultFile, VerificationCategory.NETWORK)

        assertEquals("Connected", rows[0].value)
        assertEquals("Mobile Data LTE", rows[1].value)
        assertEquals("Mobile Data LTE", rows[2].value)
        assertEquals("192.168.0.61", rows[3].value)
        assertEquals("FE80::1", rows[4].value)
        assertTrue(rows[5].value!!.contains("cellId"))
    }

    @Test
    fun locationRows_mapCoordinates() {
        val location = MyLocation()
        location.provider = "gps"
        location.latitude = 40.8630502
        location.longitude = -73.93083805
        location.accuracy = 21f
        location.speed = 0f
        location.altitude = -1.0
        location.timestamp = 1_521_672_748_000L
        val metadata = Metadata()
        metadata.myLocation = location
        val vaultFile = VaultFile()
        vaultFile.metadata = metadata

        val rows = VerificationMetadataRows.rows(vaultFile, VerificationCategory.LOCATION)

        assertEquals("GPS", rows[0].value)
        assertEquals("40.8630502", rows[1].value)
        assertEquals("-73.93083805", rows[2].value)
        assertEquals("-1 m", rows[3].value)
        assertEquals("21 m", rows[4].value)
        assertEquals("0 m/s", rows[5].value)
        assertTrue(rows[6].value!!.isNotBlank())
    }

    @Test
    fun locationRows_withoutFix_areEmpty() {
        val vaultFile = VaultFile()
        vaultFile.metadata = Metadata()

        val rows = VerificationMetadataRows.rows(vaultFile, VerificationCategory.LOCATION)

        rows.forEach { assertNull(it.value) }
    }

    @Test
    fun otherRows_mapTemperatureAndLight() {
        val metadata = Metadata()
        metadata.ambientTemperature = 21f
        metadata.light = 120f
        val vaultFile = VaultFile()
        vaultFile.metadata = metadata

        val rows = VerificationMetadataRows.rows(vaultFile, VerificationCategory.OTHER)

        assertEquals("21", rows[0].value)
        assertEquals("120", rows[1].value)
    }

    private fun vaultFileWithMetadata(): VaultFile {
        val vaultFile = VaultFile()
        vaultFile.metadata = Metadata()
        return vaultFile
    }
}
