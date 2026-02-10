package org.horizontal.tella.mobile.domain.repository.googledrive

import android.content.Context
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.googledrive.DriveFolderLocation
import org.horizontal.tella.mobile.domain.entity.googledrive.Folder
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer

interface GoogleDriveRepositoryInterface {
    suspend fun getCredential(
        request: GetCredentialRequest,
        context: Context
    ): GetCredentialResponse

    suspend fun fetchSharedDrives(email: String): List<Folder>
    suspend fun createFolder(googleDriveServer: GoogleDriveServer): String
    fun createFolder(
        googleDriveServer: GoogleDriveServer,
        parentFile: String,
        title: String,
        folderDescription: String
    ): Single<String>

    /**
     * Returns where the folder lives. With DRIVE_FILE scope, API errors (403/404) mean
     * "not app-accessible" and are returned as UNKNOWN, not as SHARED_DRIVE.
     */
    fun getFolderLocation(folderId: String, email: String): Single<DriveFolderLocation>
}