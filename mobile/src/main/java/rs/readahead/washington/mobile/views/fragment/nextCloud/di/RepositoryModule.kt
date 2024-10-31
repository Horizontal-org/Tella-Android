package rs.readahead.washington.mobile.views.fragment.nextCloud.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.NextCloudDataSource
import rs.readahead.washington.mobile.domain.repository.nextcloud.ITellaNextCloudRepository
import rs.readahead.washington.mobile.domain.repository.reports.ITellaReportsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @JvmStatic
    fun init(context: Context) {
        System.loadLibrary("sqlcipher")
    }

    @Provides
    @Singleton
    fun provideNextCloudDataSource(): NextCloudDataSource {
        return MyApplication.getKeyDataSource().nextCloudDataSource.blockingFirst()
    }


    @Provides
    @Singleton
    fun provideServerNextCloudDataSource(): ITellaNextCloudRepository {
        return MyApplication.getKeyDataSource().nextCloudDataSource.blockingFirst()
    }

    @Provides
    @Singleton
    @NextCloud
    fun provideNextCloudDataSourceRepository(): ITellaReportsRepository {
        return MyApplication.getKeyDataSource().nextCloudDataSource.blockingFirst()
    }

}