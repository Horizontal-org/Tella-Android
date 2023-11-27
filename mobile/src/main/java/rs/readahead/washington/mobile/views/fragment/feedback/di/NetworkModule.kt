package rs.readahead.washington.mobile.views.fragment.feedback.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.feedback.remote.FeedbackApiService
import rs.readahead.washington.mobile.data.feedback.repository.FeedbackRepositoryImp
import rs.readahead.washington.mobile.domain.repository.feedback.FeedBackRepository
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