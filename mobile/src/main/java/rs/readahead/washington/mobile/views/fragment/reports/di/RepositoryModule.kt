package rs.readahead.washington.mobile.views.fragment.reports.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.reports.remote.ReportsApiService
import rs.readahead.washington.mobile.data.reports.repository.ReportsRepositoryImp
import rs.readahead.washington.mobile.domain.repository.ITellaUploadServersRepository
import rs.readahead.washington.mobile.domain.repository.reports.ITellaReportsRepository
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideProductsRepository(
        service: ReportsApiService,
        client: OkHttpClient
    ): ReportsRepository {
        return ReportsRepositoryImp(service, client)
    }

    @Provides
    @Singleton
    fun provideReportsServerRepository(): ITellaUploadServersRepository {
        return MyApplication.getKeyDataSource().dataSource.blockingFirst()
    }

    @Provides
    @Singleton
    fun provideReportsRepository(): ITellaReportsRepository {
        return MyApplication.getKeyDataSource().dataSource.blockingFirst()
    }

}