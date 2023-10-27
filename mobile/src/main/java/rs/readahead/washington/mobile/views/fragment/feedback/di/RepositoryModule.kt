package rs.readahead.washington.mobile.views.fragment.feedback.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.feedback.remote.FeedbackApiService
import rs.readahead.washington.mobile.data.feedback.repository.FeedbackRepositoryImp
import rs.readahead.washington.mobile.domain.repository.feedback.FeedbackRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {


    @Provides
    @Singleton
    fun provideFeedbackRepository(
            service: FeedbackApiService,
            dataSource: DataSource
          //  statusProvider: StatusProvider
    ): FeedbackRepository {
        return FeedbackRepositoryImp(service, dataSource)
    }

//    @Provides
//    @Singleton
//    fun provideDataSource(): DataSource {
//        return MyApplication.getKeyDataSource().dataSource.blockingFirst()
//    }

}