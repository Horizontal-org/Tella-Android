package org.horizontal.tella.mobile.media

import com.hzontal.tella_vault.Metadata
import com.hzontal.tella_vault.MyLocation
import com.hzontal.tella_vault.VaultFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificationMetadataCsvTest {

    @Test
    fun fileNameFor_replacesExtensionWithCsv() {
        assertEquals("evidence.csv", VerificationMetadataCsv.fileNameFor("evidence.mp4"))
        assertEquals("photo.csv", VerificationMetadataCsv.fileNameFor("photo.jpg"))
    }

    @Test
    fun fileNameFor_appendsCsvWhenNoExtension() {
        assertEquals("notes.csv", VerificationMetadataCsv.fileNameFor("notes"))
    }

    @Test
    fun fileNameFor_usesFallbackWhenNameMissing() {
        assertEquals("file.csv", VerificationMetadataCsv.fileNameFor(null))
        assertEquals("file.csv", VerificationMetadataCsv.fileNameFor("  "))
    }

    @Test
    fun fileNameFor_keepsLeadingDotNames() {
        assertEquals(".hidden.csv", VerificationMetadataCsv.fileNameFor(".hidden"))
    }

    @Test
    fun toCsvBytes_includesOriginalFileNameAndHash() {
        val vaultFile = VaultFile()
        vaultFile.name = "clip.mp4"
        vaultFile.path = "/vault/clip.mp4"
        vaultFile.hash = "abc123"
        val metadata = Metadata()
        metadata.fileName = "clip.mp4"
        metadata.fileHashSHA256 = "abc123"
        metadata.timestamp = 1_700_000_000_000L
        metadata.manufacturer = "TestCo"
        val location = MyLocation()
        location.setLatitude(1.23)
        location.setLongitude(4.56)
        metadata.myLocation = location
        vaultFile.metadata = metadata

        val csv = String(VerificationMetadataCsv.toCsvBytes(vaultFile), Charsets.UTF_8)
        val lines = csv.trim().split('\n')
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("File name"))
        assertTrue(lines[0].contains("File hash"))
        assertTrue(lines[1].contains("clip.mp4"))
        assertTrue(lines[1].contains("abc123"))
        assertTrue(lines[1].contains("1.23"))
    }
}
