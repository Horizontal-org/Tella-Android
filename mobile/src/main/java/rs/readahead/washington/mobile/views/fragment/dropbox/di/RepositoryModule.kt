package rs.readahead.washington.mobile.views.fragment.dropbox.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.DropBoxDataSource
import rs.readahead.washington.mobile.data.dropbox.DropBoxRepository
import rs.readahead.washington.mobile.domain.repository.dropbox.IDropBoxRepository
import rs.readahead.washington.mobile.domain.repository.dropbox.ITellaDropBoxRepository
import rs.readahead.washington.mobile.domain.repository.reports.ITellaReportsRepository
import rs.readahead.washington.mobile.views.dialog.dropbox.utils.DropboxAppConfig
import rs.readahead.washington.mobile.views.dialog.dropbox.utils.DropboxOAuthUtil
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