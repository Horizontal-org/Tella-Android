package org.horizontal.tella.mobile.util

import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.database.VaultDataSource
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultFolderPathTest {

    @Test
    fun format_rootWhenThereAreNoFolders() {
        assertEquals("/", VaultFolderPath.format(emptyList()))
    }

    @Test
    fun format_singleFolder() {
        assertEquals("/video", VaultFolderPath.format(listOf("video")))
    }

    @Test
    fun format_nestedFolders() {
        assertEquals("/folder1/folder2", VaultFolderPath.format(listOf("folder1", "folder2")))
    }

    @Test
    fun resolve_rootFile() {
        val files = mapOf(
            "file" to vault("file", VaultDataSource.ROOT_UID, "photo.jpg")
        )
        assertEquals("/", VaultFolderPath.resolve("file") { files[it] })
    }

    @Test
    fun resolve_fileInOneFolder() {
        val files = mapOf(
            "file" to vault("file", "video", "clip.mp4"),
            "video" to vault("video", VaultDataSource.ROOT_UID, "video")
        )
        assertEquals("/video", VaultFolderPath.resolve("file") { files[it] })
    }

    @Test
    fun resolve_nestedFolders() {
        val files = mapOf(
            "file" to vault("file", "folder2", "photo.jpg"),
            "folder2" to vault("folder2", "folder1", "folder2"),
            "folder1" to vault("folder1", VaultDataSource.ROOT_UID, "folder1")
        )
        assertEquals("/folder1/folder2", VaultFolderPath.resolve("file") { files[it] })
    }

    @Test
    fun fileLocation_joinsFolderAndName() {
        assertEquals("/photo.csv", VaultFolderPath.fileLocation("/", "photo.csv"))
        assertEquals("/video/photo.csv", VaultFolderPath.fileLocation("/video", "photo.csv"))
        assertEquals(
            "/folder1/folder2/photo.csv",
            VaultFolderPath.fileLocation("/folder1/folder2", "photo.csv")
        )
        assertEquals("/", VaultFolderPath.fileLocation("/", null))
        assertEquals("/video", VaultFolderPath.fileLocation("/video", "  "))
    }

    @Test
    fun folderPath_includesTheFolderName() {
        val files = mapOf(
            "folder2" to vault("folder2", "folder1", "folder2"),
            "folder1" to vault("folder1", VaultDataSource.ROOT_UID, "folder1")
        )
        assertEquals("/", VaultFolderPath.folderPath(VaultDataSource.ROOT_UID) { files[it] })
        assertEquals("/folder1", VaultFolderPath.folderPath("folder1") { files[it] })
        assertEquals("/folder1/folder2", VaultFolderPath.folderPath("folder2") { files[it] })
    }

    private fun vault(id: String, parentId: String, name: String): VaultFile {
        val file = VaultFile()
        file.id = id
        file.parentId = parentId
        file.name = name
        return file
    }
}
