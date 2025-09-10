/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.hzontal.utils

import android.content.Context
import android.database.Cursor
import android.mtp.MtpConstants
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.util.*


/**
 * MediaScanner helper class.
 *
 * {@hide}
 */
object MediaFile {
    // Audio file types
    private const val FILE_TYPE_MP3 = 1
    private const val FILE_TYPE_M4A = 2
    private const val FILE_TYPE_WAV = 3
    private const val FILE_TYPE_AMR = 4
    private const val FILE_TYPE_AWB = 5
    const val FILE_TYPE_WMA = 6
    private const val FILE_TYPE_OGG = 7
    private const val FILE_TYPE_AAC = 8
    private const val FILE_TYPE_MKA = 9
    private const val FILE_TYPE_FLAC = 10
    private const val FIRST_AUDIO_FILE_TYPE = FILE_TYPE_MP3
    private const val LAST_AUDIO_FILE_TYPE = FILE_TYPE_FLAC

    // MIDI file types
    private const val FILE_TYPE_MID = 11
    private const val FILE_TYPE_SMF = 12
    private const val FILE_TYPE_IMY = 13
    private const val FIRST_MIDI_FILE_TYPE = FILE_TYPE_MID
    private const val LAST_MIDI_FILE_TYPE = FILE_TYPE_IMY

    // Video file types
    private const val FILE_TYPE_MP4 = 21
    private const val FILE_TYPE_M4V = 22
    private const val FILE_TYPE_3GPP = 23
    private const val FILE_TYPE_3GPP2 = 24
    const val FILE_TYPE_WMV = 25
    const val FILE_TYPE_ASF = 26
    private const val FILE_TYPE_MKV = 27
    private const val FILE_TYPE_MP2TS = 28
    private const val FILE_TYPE_AVI = 29
    private const val FILE_TYPE_WEBM = 30
    private const val FIRST_VIDEO_FILE_TYPE = FILE_TYPE_MP4
    private const val LAST_VIDEO_FILE_TYPE = FILE_TYPE_WEBM

    // More video file types
    private const val FILE_TYPE_MP2PS = 200
    private const val FIRST_VIDEO_FILE_TYPE2 = FILE_TYPE_MP2PS
    private const val LAST_VIDEO_FILE_TYPE2 = FILE_TYPE_MP2PS

    // Image file types
    private const val FILE_TYPE_JPEG = 31
    private const val FILE_TYPE_GIF = 32
    private const val FILE_TYPE_PNG = 33
    private const val FILE_TYPE_BMP = 34
    private const val FILE_TYPE_WBMP = 35
    private const val FILE_TYPE_WEBP = 36
    private const val FIRST_IMAGE_FILE_TYPE = FILE_TYPE_JPEG
    private const val LAST_IMAGE_FILE_TYPE = FILE_TYPE_WEBP

    // Playlist file types
    private const val FILE_TYPE_M3U = 41
    private const val FILE_TYPE_PLS = 42
    private const val FILE_TYPE_WPL = 43
    private const val FILE_TYPE_HTTPLIVE = 44
    private const val FIRST_PLAYLIST_FILE_TYPE = FILE_TYPE_M3U
    private const val LAST_PLAYLIST_FILE_TYPE = FILE_TYPE_HTTPLIVE

    // Drm file types
    private const val FILE_TYPE_FL = 51
    private const val FIRST_DRM_FILE_TYPE = FILE_TYPE_FL
    private const val LAST_DRM_FILE_TYPE = FILE_TYPE_FL

    // Other popular file types
    private const val FILE_TYPE_TEXT = 100
    private const val FILE_TYPE_HTML = 101
    private const val FILE_TYPE_PDF = 102
    const val FILE_TYPE_XML = 103
    private const val FILE_TYPE_MS_WORD = 104
    private const val FILE_TYPE_MS_EXCEL = 105
    private const val FILE_TYPE_MS_POWERPOINT = 106
    private const val FILE_TYPE_ZIP = 107
    private const val FILE_TYPE_ODT = 108
    private val sFileTypeMap =
            HashMap<String, MediaFileType>()
    private val sMimeTypeMap =
            HashMap<String, Int>()

    // maps file extension to MTP format code
    private val sFileTypeToFormatMap =
            HashMap<String, Int>()

    // maps mime type to MTP format code
    private val sMimeTypeToFormatMap =
            HashMap<String, Int>()

    // maps MTP format code to mime type
    private val sFormatToMimeTypeMap =
            HashMap<Int, String>()

    fun addFileType(extension: String, fileType: Int, mimeType: String) {
        sFileTypeMap[extension] = MediaFileType(fileType, mimeType)
        sMimeTypeMap[mimeType] = Integer.valueOf(fileType)
    }

    fun addFileType(
            extension: String, fileType: Int, mimeType: String, mtpFormatCode: Int
    ) {
        addFileType(extension, fileType, mimeType)
        sFileTypeToFormatMap[extension] = Integer.valueOf(mtpFormatCode)
        sMimeTypeToFormatMap[mimeType] = Integer.valueOf(mtpFormatCode)
        sFormatToMimeTypeMap[mtpFormatCode] = mimeType
    }

    fun isAudioFileType(mimeType: String): Boolean {
        return isAudioFileType(getFileTypeForMimeType(mimeType))
    }

    fun isAudioFileType(fileType: Int): Boolean {
        return fileType in FIRST_AUDIO_FILE_TYPE..LAST_AUDIO_FILE_TYPE ||
                fileType in FIRST_MIDI_FILE_TYPE..LAST_MIDI_FILE_TYPE
    }

    fun isVideoFileType(fileType: Int): Boolean {
        return (fileType in FIRST_VIDEO_FILE_TYPE..LAST_VIDEO_FILE_TYPE
                || fileType in FIRST_VIDEO_FILE_TYPE2..LAST_VIDEO_FILE_TYPE2)
    }

    fun isVideoFileType(mimeType: String): Boolean {
        return isVideoFileType(getFileTypeForMimeType(mimeType))
    }

    fun isImageFileType(mimeType: String): Boolean {
        return isImageFileType(getFileTypeForMimeType(mimeType))
    }

    fun isTextFileType(mimeType: String): Boolean {
        return isTextFileType(getFileTypeForMimeType(mimeType))
    }

    fun isImageFileType(fileType: Int): Boolean {
        return fileType in FIRST_IMAGE_FILE_TYPE..LAST_IMAGE_FILE_TYPE
    }

    fun isPlayListFileType(mimeType: String): Boolean {
        return isPlayListFileType(getFileTypeForMimeType(mimeType))
    }

    fun isPlayListFileType(fileType: Int): Boolean {
        return fileType in FIRST_PLAYLIST_FILE_TYPE..LAST_PLAYLIST_FILE_TYPE
    }

    fun isDrmFileType(fileType: Int): Boolean {
        return fileType in FIRST_DRM_FILE_TYPE..LAST_DRM_FILE_TYPE
    }

    fun isTextFileType(fileType: Int): Boolean {
        return fileType in FILE_TYPE_TEXT..FILE_TYPE_ODT
    }

    private fun getFileType(path: String?): MediaFileType? {
        val lastDot = path?.lastIndexOf('.')
        if (lastDot != null) {
            return if (lastDot < 0) null else sFileTypeMap[path.substring(lastDot + 1)
                    .toUpperCase(Locale.ROOT)]
        }
        return null
    }

    fun getFileName(uri: Uri?, context: Context): String? {
        var fileName: String? = getFileNameFromCursor(uri, context)
        if (!fileName?.contains(".")!!) {
            val fileExtension: String? = getFileExtension(uri, context)
            fileName = "$fileName.$fileExtension"
        }
        return fileName
    }

    fun getFileExtension(uri: Uri?, context: Context): String? {
        val fileType: String? = uri?.let { context.contentResolver.getType(it) }
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType)
    }

    private fun getFileNameFromCursor(uri: Uri?, context: Context): String? {
        val fileCursor: Cursor? = uri?.let {
            context.contentResolver
                    .query(it, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        }
        var fileName: String? = null
        if (fileCursor != null && fileCursor.moveToFirst()) {
            val cIndex: Int = fileCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cIndex != -1) {
                fileName = fileCursor.getString(cIndex)
            }
        }
        return fileName
    }

    fun isMimeTypeMedia(mimeType: String?): Boolean {
        val fileType = getFileTypeForMimeType(mimeType)
        return (isAudioFileType(fileType) || isVideoFileType(fileType)
                || isImageFileType(fileType) || isPlayListFileType(fileType))
    }

    // generates a title based on file name
    fun getFileTitle(path: String): String {
        // extract file name after last slash
        var path = path
        var lastSlash = path.lastIndexOf('/')
        if (lastSlash >= 0) {
            lastSlash++
            if (lastSlash < path.length) {
                path = path.substring(lastSlash)
            }
        }
        // truncate the file extension (if any)
        val lastDot = path.lastIndexOf('.')
        if (lastDot > 0) {
            path = path.substring(0, lastDot)
        }
        return path
    }

    private fun getFileTypeForMimeType(mimeType: String?): Int {
        val value = sMimeTypeMap[mimeType]
        return value ?: 0
    }

    fun getMimeTypeForFile(path: String?): String? {
        val mediaFileType = getFileType(path)
        return mediaFileType?.mimeType
    }
   private fun isPDFFile(extension: String): Boolean {
        val pdfExtensions = setOf("PDF")
        return pdfExtensions.contains(extension.uppercase())
    }

    // Add this function to the MediaFile object
    fun isPDFFile(fileName: String, mimeType: String?): Boolean {
        if (mimeType != null) {
            return mimeType.equals("application/pdf", ignoreCase = true)
        }

        val lastDot = fileName.lastIndexOf('.')
        if (lastDot > 0) {
            val extension = fileName.substring(lastDot + 1).uppercase(Locale.ROOT)
            return isPDFFile(extension)
        }

        return false
    }

    fun getFormatCode(fileName: String, mimeType: String?): Int {
        if (mimeType != null) {
            val value = sMimeTypeToFormatMap[mimeType]
            if (value != null) {
                return value.toInt()
            }
        }
        val lastDot = fileName.lastIndexOf('.')
        if (lastDot > 0) {
            val extension =
                    fileName.substring(lastDot + 1).toUpperCase(Locale.ROOT)
            val value = sFileTypeToFormatMap[extension]
            if (value != null) {
                return value.toInt()
            }
        }
        return MtpConstants.FORMAT_UNDEFINED
    }

    fun getMimeTypeForFormatCode(formatCode: Int): String? {
        return sFormatToMimeTypeMap[formatCode]
    }

    class MediaFileType internal constructor(val fileType: Int, val mimeType: String)

    init {
        addFileType("MP3", FILE_TYPE_MP3, "audio/mpeg", MtpConstants.FORMAT_MP3)
        addFileType("MPGA", FILE_TYPE_MP3, "audio/mpeg", MtpConstants.FORMAT_MP3)
        addFileType("M4A", FILE_TYPE_M4A, "audio/mp4", MtpConstants.FORMAT_MPEG)
        addFileType("WAV", FILE_TYPE_WAV, "audio/x-wav", MtpConstants.FORMAT_WAV)
        addFileType("AMR", FILE_TYPE_AMR, "audio/amr")
        addFileType("AWB", FILE_TYPE_AWB, "audio/amr-wb")
        /* if (isWMAEnabled()) {
            addFileType("WMA", FILE_TYPE_WMA, "audio/x-ms-wma", MtpConstants.FORMAT_WMA);
        }*/addFileType("OGG", FILE_TYPE_OGG, "audio/ogg", MtpConstants.FORMAT_OGG)
        addFileType("OGG", FILE_TYPE_OGG, "application/ogg", MtpConstants.FORMAT_OGG)
        addFileType("OGA", FILE_TYPE_OGG, "application/ogg", MtpConstants.FORMAT_OGG)
        addFileType("AAC", FILE_TYPE_AAC, "audio/aac", MtpConstants.FORMAT_AAC)
        addFileType("AAC", FILE_TYPE_AAC, "audio/aac-adts", MtpConstants.FORMAT_AAC)
        addFileType("MKA", FILE_TYPE_MKA, "audio/x-matroska")
        addFileType("MID", FILE_TYPE_MID, "audio/midi")
        addFileType("MIDI", FILE_TYPE_MID, "audio/midi")
        addFileType("XMF", FILE_TYPE_MID, "audio/midi")
        addFileType("RTTTL", FILE_TYPE_MID, "audio/midi")
        addFileType("SMF", FILE_TYPE_SMF, "audio/sp-midi")
        addFileType("IMY", FILE_TYPE_IMY, "audio/imelody")
        addFileType("RTX", FILE_TYPE_MID, "audio/midi")
        addFileType("OTA", FILE_TYPE_MID, "audio/midi")
        addFileType("MXMF", FILE_TYPE_MID, "audio/midi")
        addFileType("MPEG", FILE_TYPE_MP4, "video/mpeg", MtpConstants.FORMAT_MPEG)
        addFileType("MPG", FILE_TYPE_MP4, "video/mpeg", MtpConstants.FORMAT_MPEG)
        addFileType("MP4", FILE_TYPE_MP4, "video/mp4", MtpConstants.FORMAT_MPEG)
        addFileType("M4V", FILE_TYPE_M4V, "video/mp4", MtpConstants.FORMAT_MPEG)
        addFileType("3GP", FILE_TYPE_3GPP, "video/3gpp", MtpConstants.FORMAT_3GP_CONTAINER)
        addFileType("3GPP", FILE_TYPE_3GPP, "video/3gpp", MtpConstants.FORMAT_3GP_CONTAINER)
        addFileType("3G2", FILE_TYPE_3GPP2, "video/3gpp2", MtpConstants.FORMAT_3GP_CONTAINER)
        addFileType("3GPP2", FILE_TYPE_3GPP2, "video/3gpp2", MtpConstants.FORMAT_3GP_CONTAINER)
        addFileType("MKV", FILE_TYPE_MKV, "video/x-matroska")
        addFileType("WEBM", FILE_TYPE_WEBM, "video/webm")
        addFileType("TS", FILE_TYPE_MP2TS, "video/mp2ts")
        addFileType("AVI", FILE_TYPE_AVI, "video/avi")

        /* if (isWMVEnabled()) {
            addFileType("WMV", FILE_TYPE_WMV, "video/x-ms-wmv", MtpConstants.FORMAT_WMV);
            addFileType("ASF", FILE_TYPE_ASF, "video/x-ms-asf");
        }*/addFileType("JPG", FILE_TYPE_JPEG, "image/jpeg", MtpConstants.FORMAT_EXIF_JPEG)

        addFileType("JPEG", FILE_TYPE_JPEG, "image/jpeg", MtpConstants.FORMAT_EXIF_JPEG)
        addFileType("GIF", FILE_TYPE_GIF, "image/gif", MtpConstants.FORMAT_GIF)
        addFileType("PNG", FILE_TYPE_PNG, "image/png", MtpConstants.FORMAT_PNG)
        addFileType("BMP", FILE_TYPE_BMP, "image/x-ms-bmp", MtpConstants.FORMAT_BMP)
        addFileType("WBMP", FILE_TYPE_WBMP, "image/vnd.wap.wbmp")
        addFileType("WEBP", FILE_TYPE_WEBP, "image/webp")
        addFileType("M3U", FILE_TYPE_M3U, "audio/x-mpegurl", MtpConstants.FORMAT_M3U_PLAYLIST)
        addFileType(
                "M3U",
                FILE_TYPE_M3U,
                "application/x-mpegurl",
                MtpConstants.FORMAT_M3U_PLAYLIST
        )
        addFileType(
                "PLS",
                FILE_TYPE_PLS,
                "audio/x-scpls",
                MtpConstants.FORMAT_PLS_PLAYLIST
        )
        addFileType(
                "WPL",
                FILE_TYPE_WPL,
                "application/vnd.ms-wpl",
                MtpConstants.FORMAT_WPL_PLAYLIST
        )
        addFileType("M3U8", FILE_TYPE_HTTPLIVE, "application/vnd.apple.mpegurl")
        addFileType("M3U8", FILE_TYPE_HTTPLIVE, "audio/mpegurl")
        addFileType("M3U8", FILE_TYPE_HTTPLIVE, "audio/x-mpegurl")
        addFileType("FL", FILE_TYPE_FL, "application/x-android-drm-fl")
        addFileType(
                "TXT",
                FILE_TYPE_TEXT,
                "text/plain",
                MtpConstants.FORMAT_TEXT
        )
        addFileType(
                "HTM",
                FILE_TYPE_HTML,
                "text/html",
                MtpConstants.FORMAT_HTML
        )
        addFileType(
                "HTML",
                FILE_TYPE_HTML,
                "text/html",
                MtpConstants.FORMAT_HTML
        )
        addFileType("PDF", FILE_TYPE_PDF, "application/pdf")
        addFileType(
                "DOC",
                FILE_TYPE_MS_WORD,
                "application/msword",
                MtpConstants.FORMAT_MS_WORD_DOCUMENT
        )
        addFileType(
                "XLS",
                FILE_TYPE_MS_EXCEL,
                "application/vnd.ms-excel",
                MtpConstants.FORMAT_MS_EXCEL_SPREADSHEET
        )
        addFileType(
                "PPT",
                FILE_TYPE_MS_POWERPOINT,
                "application/mspowerpoint",
                MtpConstants.FORMAT_MS_POWERPOINT_PRESENTATION
        )
        addFileType(
                "FLAC",
                FILE_TYPE_FLAC,
                "audio/flac",
                MtpConstants.FORMAT_FLAC
        )
        addFileType(
            "ODT",
            FILE_TYPE_ODT,
            "application/vnd.oasis.opendocument.text"
        )
        addFileType("DOCX", FILE_TYPE_MS_WORD, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        addFileType("ZIP", FILE_TYPE_ZIP, "application/zip")
        addFileType("MPG", FILE_TYPE_MP2PS, "video/mp2p")
        addFileType("MPEG", FILE_TYPE_MP2PS, "video/mp2p")
    }

}