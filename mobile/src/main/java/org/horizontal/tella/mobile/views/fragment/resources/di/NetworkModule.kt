package org.horizontal.tella.mobile.views.fragment.resources.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import org.horizontal.tella.mobile.data.resources.remote.ResourcesApiService
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {

    @Singleton
    @Provides
    fun providesApiService(retrofit: Retrofit): ResourcesApiService =
            retrofit.create(ResourcesApiService::class.java)
}