package rs.readahead.washington.mobile.views.fragment.feedback.di
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import rs.readahead.washington.mobile.data.feedback.remote.FeedbackApiService
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {
    @Singleton
    @Provides
    fun providesFeedbackApiService(retrofit: Retrofit): FeedbackApiService =
            retrofit.create(FeedbackApiService::class.java)

}