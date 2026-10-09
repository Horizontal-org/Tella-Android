package com.hzontal.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFileCsvTest {

    @Test
    fun csvMimeTypesAndExtensionAreCsvFiles() {
        assertTrue(MediaFile.isCsvFile("export.csv", "text/csv"))
        assertTrue(MediaFile.isCsvFile("export.csv", "application/csv"))
        assertTrue(MediaFile.isCsvFile("export.csv", "text/comma-separated-values"))
        assertTrue(MediaFile.isCsvFile("export.csv", "text/csv; charset=utf-8"))
        assertTrue(MediaFile.isCsvFile("export.CSV", "application/octet-stream"))
        assertTrue(MediaFile.isCsvFile(null, "text/csv"))
    }

    @Test
    fun otherDocumentsAreNotCsvFiles() {
        assertFalse(MediaFile.isCsvFile("notes.txt", "text/plain"))
        assertFalse(MediaFile.isCsvFile("form.pdf", "application/pdf"))
        assertFalse(MediaFile.isCsvFile("photo.jpg", "image/jpeg"))
        assertFalse(MediaFile.isCsvFile(null, null))
    }
}
