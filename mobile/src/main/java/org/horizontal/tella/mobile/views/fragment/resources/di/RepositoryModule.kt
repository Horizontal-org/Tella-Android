package org.horizontal.tella.mobile.views.fragment.resources.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.resources.remote.ResourcesApiService
import org.horizontal.tella.mobile.data.resources.repository.ResourcesRepositoryImp
import org.horizontal.tella.mobile.domain.repository.resources.ITellaResourcesRepository
import org.horizontal.tella.mobile.domain.repository.resources.ResourcesRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideResourcesRepository(
        service: ResourcesApiService
    ): ResourcesRepository {
        return ResourcesRepositoryImp(service)
    }

    @Provides
    @Singleton
    fun provideResourcesDataSource(): ITellaResourcesRepository {
        return MyApplication.getKeyDataSource().resourceDataSource.blockingFirst()
    }
}