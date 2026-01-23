package org.horizontal.tella.mobile.views.fragment.dropbox.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.database.DropBoxDataSource
import org.horizontal.tella.mobile.data.dropbox.DropBoxRepository
import org.horizontal.tella.mobile.domain.repository.dropbox.IDropBoxRepository
import org.horizontal.tella.mobile.domain.repository.dropbox.ITellaDropBoxRepository
import org.horizontal.tella.mobile.domain.repository.reports.ITellaReportsRepository
import org.horizontal.tella.mobile.views.dialog.dropbox.utils.DropboxAppConfig
import org.horizontal.tella.mobile.views.dialog.dropbox.utils.DropboxOAuthUtil
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideServerDropBoxDataSource(): DropBoxDataSource {
        return MyApplication.getKeyDataSource().dropBoxDataSource.blockingFirst()
    }

    @Provides
    @Singleton
    fun provideServeDropBoxDataSource(): ITellaDropBoxRepository {
        return MyApplication.getKeyDataSource().dropBoxDataSource.blockingFirst()
    }


    @Provides
    @Singleton
    @DropBox
    fun provideDropBoxDataSourceRepository(): ITellaReportsRepository {
        return MyApplication.getKeyDataSource().dropBoxDataSource.blockingFirst()
    }

    @Provides
    @Singleton
    fun provideDropBoxRepository(
    ): IDropBoxRepository {
        return DropBoxRepository()
    }

    @Provides
    @Singleton
    fun providesDropboxDropboxAppConfig(): DropboxAppConfig {
        return DropboxAppConfig()
    }

    @Provides
    @Singleton
    fun providesDropboxDropboxDropboxOAuthUtil(
        dropboxAppConfig: DropboxAppConfig
    ): DropboxOAuthUtil {
        return DropboxOAuthUtil(dropboxAppConfig)
    }
}