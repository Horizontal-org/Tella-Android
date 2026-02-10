package org.horizontal.tella.mobile.domain.repository.googledrive

import android.content.Context
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import io.reactivex.Single
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
     * Returns true if the folder is on a Shared Drive (driveId is non-empty in Drive API).
     * Use this as the source of truth instead of a stored flag.
     */
    fun isFolderOnSharedDrive(folderId: String, email: String): Single<Boolean>
}