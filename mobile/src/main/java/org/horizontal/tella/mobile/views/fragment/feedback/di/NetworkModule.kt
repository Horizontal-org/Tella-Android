package org.horizontal.tella.mobile.views.fragment.feedback.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.data.feedback.remote.FeedbackApiService
import org.horizontal.tella.mobile.data.feedback.repository.FeedbackRepositoryImp
import org.horizontal.tella.mobile.domain.repository.feedback.FeedBackRepository
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {
    @Singleton
    @Provides
    fun providesFeedbackApiService(retrofit: Retrofit): FeedbackApiService = retrofit.create(FeedbackApiService::class.java)

    @Provides
    @Singleton
    fun provideFeedbackRepository(service: FeedbackApiService, dataSource: DataSource): FeedBackRepository {
        return FeedbackRepositoryImp(service, dataSource)
    }
}