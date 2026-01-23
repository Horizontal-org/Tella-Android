package org.horizontal.tella.mobile.views.fragment.googledrive.di

import android.content.Context
import androidx.credentials.CredentialManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.horizontal.tella.mobile.domain.entity.googledrive.Config
import org.horizontal.tella.mobile.domain.repository.googledrive.GoogleDriveRepositoryInterface
import org.horizontal.tella.mobile.domain.repository.googledrive.ITellaGoogleDriveRepository
import org.horizontal.tella.mobile.domain.repository.reports.ITellaReportsRepository
import javax.inject.Singleton

/**
 * Stub Hilt module for Google Drive in F-Droid builds.
 * 
 * Provides empty/stub implementations since Google Drive is not available
 * in F-Droid builds. All providers throw UnsupportedOperationException.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager {
        // CredentialManager is available in both flavors (we made it implementation, not playstoreImplementation)
        return CredentialManager.create(context)
    }

    @Provides
    @Singleton
    fun provideDriveServiceProvider(@ApplicationContext context: Context): org.horizontal.tella.mobile.views.fragment.googledrive.di.DriveServiceProvider {
        return org.horizontal.tella.mobile.views.fragment.googledrive.di.DriveServiceProvider(context)
    }

    @Provides
    @Singleton
    fun provideGoogleDriveRepository(
        credentialManager: CredentialManager,
        driveServiceProvider: org.horizontal.tella.mobile.views.fragment.googledrive.di.DriveServiceProvider
    ): GoogleDriveRepositoryInterface {
        return org.horizontal.tella.mobile.data.googledrive.GoogleDriveRepository(
            credentialManager,
            driveServiceProvider
        )
    }

    @Provides
    @Singleton
    fun provideGoogleDriveDataSource(): org.horizontal.tella.mobile.data.database.GoogleDriveDataSource {
        throw UnsupportedOperationException("Google Drive is not available in F-Droid builds")
    }

    @Provides
    @Singleton
    fun provideServerGoogleDriveDataSource(): ITellaGoogleDriveRepository {
        throw UnsupportedOperationException("Google Drive is not available in F-Droid builds")
    }

    @Provides
    @Singleton
    @GoogleDrive
    fun provideGoogleDriveDataSourceRepository(): ITellaReportsRepository {
        throw UnsupportedOperationException("Google Drive is not available in F-Droid builds")
    }

    @Provides
    @Singleton
    fun provideGoogleDriveId(): Config {
        // Return a stub Config with empty values for F-Droid builds
        // This is needed because HomeVaultViewModel and HomeVaultFragment inject Config
        return Config(googleClientId = "")
    }
}




