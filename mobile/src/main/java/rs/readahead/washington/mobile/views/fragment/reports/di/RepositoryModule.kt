package rs.readahead.washington.mobile.views.fragment.reports.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rs.readahead.washington.mobile.data.reports.remote.ReportsApiService
import rs.readahead.washington.mobile.data.reports.repository.ReportsRepositoryImp
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideProductsRepository(service: ReportsApiService): ReportsRepository {
        return ReportsRepositoryImp(service)
    }
}