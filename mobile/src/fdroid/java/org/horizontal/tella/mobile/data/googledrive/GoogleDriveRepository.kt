package org.horizontal.tella.mobile.data.googledrive

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.googledrive.Folder
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer
import org.horizontal.tella.mobile.domain.repository.googledrive.GoogleDriveRepositoryInterface
import org.horizontal.tella.mobile.views.fragment.googledrive.di.DriveServiceProvider
import javax.inject.Inject

/**
 * Stub implementation of GoogleDriveRepository for F-Droid builds.
 * 
 * This class exists to satisfy compile-time dependencies but throws
 * UnsupportedOperationException since Google Drive is not available in F-Droid builds.
 */
class GoogleDriveRepository @Inject constructor(
    private val credentialManager: CredentialManager,
    private val driveServiceProvider: DriveServiceProvider
) : GoogleDriveRepositoryInterface {

    override suspend fun getCredential(
        request: GetCredentialRequest,
        context: Context
    ): GetCredentialResponse {
        throw UnsupportedOperationException("Google Drive is not available in F-Droid builds")
    }

    override suspend fun fetchSharedDrives(email: String): List<Folder> {
        throw UnsupportedOperationException("Google Drive is not available in F-Droid builds")
    }

    override suspend fun createFolder(googleDriveServer: GoogleDriveServer): String {
        throw UnsupportedOperationException("Google Drive is not available in F-Droid builds")
    }

    override fun createFolder(
        googleDriveServer: GoogleDriveServer,
        parentFile: String,
        title: String,
        folderDescription: String
    ): Single<String> {
        return Single.error(UnsupportedOperationException("Google Drive is not available in F-Droid builds"))
    }
}




