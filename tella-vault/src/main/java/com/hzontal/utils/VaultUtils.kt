package com.hzontal.utils

import com.hzontal.tella_vault.VaultFile
import com.hzontal.utils.C.MEDIA_DIR
import java.util.*

object VaultUtils {

    fun newPng(): VaultFile {
        val uid = UUID.randomUUID().toString()
        val vaultFile = VaultFile()
        vaultFile.id = uid
        vaultFile.name = "$uid.png"
        vaultFile.type = VaultFile.Type.FILE
        vaultFile.mimeType = "image/png"
        return vaultFile
    }

    fun newJpeg(): VaultFile {
        val uid = UUID.randomUUID().toString()
        val vaultFile = VaultFile()
        vaultFile.id = uid
        vaultFile.name = "$uid.png"
        vaultFile.type = VaultFile.Type.FILE
        vaultFile.mimeType = "image/jpeg"
        return vaultFile
    }

    fun newMp4(): VaultFile {
        val uid = UUID.randomUUID().toString()
        val vaultFile = VaultFile()
        vaultFile.id = uid
        vaultFile.name = "$uid.mp4"
        vaultFile.type = VaultFile.Type.FILE
        vaultFile.path = MEDIA_DIR
        vaultFile.mimeType = "video/mp4"
        return vaultFile
    }

 /*   fun newAac(): MediaFile? {
        val uid = UUID.randomUUID().toString()
        return MediaFile(rs.readahead.washington.mobile.util.C.MEDIA_DIR, uid, "$uid.aac", MediaFile.Type.AUDIO)
    }

    fun newMp4(): MediaFile? {
        val uid = UUID.randomUUID().toString()
        return MediaFile(rs.readahead.washington.mobile.util.C.MEDIA_DIR, uid, "$uid.mp4", MediaFile.Type.VIDEO)
    }*/

    fun fromFilename(filename: String): VaultFile {
        val vaultFile = VaultFile()
        vaultFile.name = filename
        vaultFile.id = FileUtil.getBaseName(filename)
        vaultFile.path = C.MEDIA_DIR
        return vaultFile
    }
}