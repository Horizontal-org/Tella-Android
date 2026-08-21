package org.horizontal.tella.mobile.data.nextcloud

import java.io.File

object TempFileManager {
    private val tempFiles = mutableListOf<File>()

    fun addFile(file: File) {
        tempFiles.add(file)
    }

    fun deleteAllFiles() {
        for (file in tempFiles) {
            if (file.exists()) {
                file.delete()
            }
        }
        tempFiles.clear()
    }
}