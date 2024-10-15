package rs.readahead.washington.mobile.views.fragment.dropbox.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.DropBoxDataSource
import rs.readahead.washington.mobile.domain.repository.dropbox.IDropBoxRepository
import rs.readahead.washington.mobile.domain.repository.googledrive.IGoogleDriveRepository
import rs.readahead.washington.mobile.domain.repository.reports.ITellaReportsRepository
import rs.readahead.washington.mobile.views.fragment.googledrive.di.GoogleDrive
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
    fun provideServeDropBoxDataSource(): IDropBoxRepository {
        return MyApplication.getKeyDataSource().dropBoxDataSource.blockingFirst()
    }


    @Provides
    @Singleton
    @DropBox
    fun provideDropBoxDataSourceRepository(): ITellaReportsRepository {
        return MyApplication.getKeyDataSource().dropBoxDataSource.blockingFirst()
    }

}