package org.horizontal.tella.mobile.views.fragment.dropbox.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.horizontal.tella.mobile.domain.repository.dropbox.IDropBoxRepository
import org.horizontal.tella.mobile.domain.repository.dropbox.ITellaDropBoxRepository
import org.horizontal.tella.mobile.domain.repository.reports.ITellaReportsRepository
import org.horizontal.tella.mobile.views.dialog.dropbox.utils.DropboxAppConfig
import org.horizontal.tella.mobile.views.dialog.dropbox.utils.DropboxOAuthUtil
import javax.inject.Singleton

/**
 * Stub Hilt module for Dropbox in F-Droid builds.
 * 
 * Provides empty/stub implementations since Dropbox is not available
 * in F-Droid builds. All providers throw UnsupportedOperationException.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideServerDropBoxDataSource(): org.horizontal.tella.mobile.data.database.DropBoxDataSource {
        throw UnsupportedOperationException("Dropbox is not available in F-Droid builds")
    }

    @Provides
    @Singleton
    fun provideServeDropBoxDataSource(): ITellaDropBoxRepository {
        throw UnsupportedOperationException("Dropbox is not available in F-Droid builds")
    }

    @Provides
    @Singleton
    @DropBox
    fun provideDropBoxDataSourceRepository(): ITellaReportsRepository {
        throw UnsupportedOperationException("Dropbox is not available in F-Droid builds")
    }

    @Provides
    @Singleton
    fun provideDropBoxRepository(): IDropBoxRepository {
        throw UnsupportedOperationException("Dropbox is not available in F-Droid builds")
    }

    @Provides
    @Singleton
    fun providesDropboxDropboxAppConfig(): DropboxAppConfig {
        throw UnsupportedOperationException("Dropbox is not available in F-Droid builds")
    }

    @Provides
    @Singleton
    fun providesDropboxDropboxDropboxOAuthUtil(
        dropboxAppConfig: DropboxAppConfig
    ): DropboxOAuthUtil {
        throw UnsupportedOperationException("Dropbox is not available in F-Droid builds")
    }
}




