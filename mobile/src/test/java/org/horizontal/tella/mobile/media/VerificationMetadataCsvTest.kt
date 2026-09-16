package org.horizontal.tella.mobile.media

import com.hzontal.tella_vault.Metadata
import com.hzontal.tella_vault.MyLocation
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.database.VaultDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
    fun zipNameFor_replacesExtensionWithZip() {
        assertEquals("evidence.zip", VerificationMetadataCsv.zipNameFor("evidence.mp4"))
        assertEquals("photo.zip", VerificationMetadataCsv.zipNameFor("photo.jpg"))
        assertEquals("notes.zip", VerificationMetadataCsv.zipNameFor("notes"))
        assertEquals("file.zip", VerificationMetadataCsv.zipNameFor(null))
    }

    @Test
    fun zipNameFor_usesFileNameForSingleFileAndFallbackForMany() {
        val photo = VaultFile()
        photo.name = "photo.jpg"
        photo.type = VaultFile.Type.FILE
        val video = VaultFile()
        video.name = "clip.mp4"
        video.type = VaultFile.Type.FILE
        assertEquals("photo.zip", VerificationMetadataCsv.zipNameFor(listOf(photo)))
        assertEquals("tella.zip", VerificationMetadataCsv.zipNameFor(listOf(photo, video)))
    }

    @Test
    fun parentFolderId_prefersExplicitParentThenFileParentThenRoot() {
        assertEquals("folder", VerificationMetadataCsv.parentFolderId("folder", "other"))
        assertEquals("file-parent", VerificationMetadataCsv.parentFolderId(null, "file-parent"))
        assertEquals("file-parent", VerificationMetadataCsv.parentFolderId("  ", "file-parent"))
        assertEquals(
            VaultDataSource.ROOT_UID,
            VerificationMetadataCsv.parentFolderId(null, null)
        )
    }

    @Test
    fun existingIn_findsCsvNamedAfterOriginalFile() {
        val csv = VaultFile()
        csv.name = "photo.csv"
        val other = VaultFile()
        other.name = "photo.jpg"
        assertSame(csv, VerificationMetadataCsv.existingIn(listOf(other, csv), "photo.jpg"))
        assertNull(VerificationMetadataCsv.existingIn(listOf(other), "photo.jpg"))
        assertNull(VerificationMetadataCsv.existingIn(null, "photo.jpg"))
    }

    @Test
    fun existingCsv_prefersLinkedFileOverSameFolderName() {
        val linked = VaultFile()
        linked.id = "csv-id"
        linked.name = "photo.csv"
        linked.parentId = "other-folder"
        val siblingCsv = VaultFile()
        siblingCsv.name = "photo.csv"
        assertSame(
            linked,
            VerificationMetadataCsv.existingCsv(linked, listOf(siblingCsv), "photo.jpg")
        )
        assertSame(
            siblingCsv,
            VerificationMetadataCsv.existingCsv(null, listOf(siblingCsv), "photo.jpg")
        )
        assertNull(VerificationMetadataCsv.existingCsv(null, emptyList(), "photo.jpg"))
    }

    @Test
    fun needsVerificationSharePrompt_skipsWhenCsvAlreadySelected() {
        val photo = VaultFile()
        photo.id = "photo-id"
        photo.name = "photo.jpg"
        photo.metadata = Metadata()
        val csv = VaultFile()
        csv.id = "csv-id"
        csv.name = "photo.csv"
        csv.sourceFileId = "photo-id"
        assertTrue(VerificationMetadataCsv.needsVerificationSharePrompt(listOf(photo)))
        assertFalse(VerificationMetadataCsv.needsVerificationSharePrompt(listOf(photo, csv)))
        val csvByName = VaultFile()
        csvByName.name = "photo.csv"
        assertFalse(VerificationMetadataCsv.needsVerificationSharePrompt(listOf(photo, csvByName)))
        assertFalse(VerificationMetadataCsv.needsVerificationSharePrompt(listOf(csv)))
    }

    @Test
    fun needsVerificationSharePrompt_whenOnlySomeFilesHaveCsvSelected() {
        val photo = VaultFile()
        photo.id = "photo-id"
        photo.name = "photo.jpg"
        photo.metadata = Metadata()
        val photoCsv = VaultFile()
        photoCsv.id = "photo-csv"
        photoCsv.name = "photo.csv"
        photoCsv.sourceFileId = "photo-id"
        val video = VaultFile()
        video.id = "video-id"
        video.name = "clip.mp4"
        video.metadata = Metadata()
        assertTrue(
            VerificationMetadataCsv.needsVerificationSharePrompt(listOf(photo, photoCsv, video))
        )
        val videoCsv = VaultFile()
        videoCsv.name = "clip.csv"
        assertFalse(
            VerificationMetadataCsv.needsVerificationSharePrompt(
                listOf(photo, photoCsv, video, videoCsv)
            )
        )
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

        val csv = String(VerificationMetadataCsv.toCsvBytes(vaultFile, "/video"), Charsets.UTF_8)
        val lines = csv.trim().split('\n')
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("File name"))
        assertTrue(lines[0].contains("File path"))
        assertTrue(lines[0].contains("File hash"))
        assertTrue(lines[1].contains("clip.mp4"))
        assertTrue(lines[1].contains("/video"))
        assertTrue(lines[1].contains("abc123"))
        assertTrue(lines[1].contains("1.23"))
    }

    @Test
    fun toCsvBytes_pathIsRootWhenLocationMissing() {
        val vaultFile = VaultFile()
        vaultFile.name = "photo.jpg"
        vaultFile.hash = "abc123"

        val csv = String(VerificationMetadataCsv.toCsvBytes(vaultFile), Charsets.UTF_8)
        assertTrue(csv.contains("/"))
    }

    @Test
    fun asVerificationMetadataAlreadySaved_unwrapsCause() {
        val alreadySaved = VerificationMetadataAlreadySavedException("/video/photo.csv")
        assertSame(alreadySaved, alreadySaved.asVerificationMetadataAlreadySaved())
        assertSame(
            alreadySaved,
            RuntimeException(alreadySaved).asVerificationMetadataAlreadySaved()
        )
        assertNull(IllegalStateException("other").asVerificationMetadataAlreadySaved())
    }
}
