package org.horizontal.tella.mobile.views.fragment.reports.di

import android.content.Context
import com.hzontal.tella_vault.BaseVault.Config
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.KeyRxVault
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.data.nextcloud.NextCloudRepositoryImp
import org.horizontal.tella.mobile.data.reports.remote.ReportsApiService
import org.horizontal.tella.mobile.data.reports.repository.ReportsRepositoryImp
import org.horizontal.tella.mobile.domain.repository.ITellaUploadServersRepository
import org.horizontal.tella.mobile.domain.repository.nextcloud.NextCloudRepository
import org.horizontal.tella.mobile.domain.repository.reports.ITellaReportsRepository
import org.horizontal.tella.mobile.domain.repository.reports.ReportsRepository
import org.horizontal.tella.mobile.util.StatusProvider
import org.hzontal.tella.keys.key.LifecycleMainKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideReportsRepository(
        service: ReportsApiService,
        dataSource: DataSource,
        statusProvider: StatusProvider
    ): ReportsRepository {
        return ReportsRepositoryImp(service, dataSource, statusProvider)
    }

    @Provides
    @Singleton
    fun provideReportsServerRepository(): ITellaUploadServersRepository {
        return MyApplication.getKeyDataSource().dataSource.blockingFirst()
    }

    @Provides
    @Singleton
    @DefaultReports
    fun provideReportsDataSource(): ITellaReportsRepository {
        return MyApplication.getKeyDataSource().dataSource.blockingFirst()
    }

    @Provides
    @Singleton
    fun provideDataSource(): DataSource {
        return MyApplication.getKeyDataSource().dataSource.blockingFirst()
    }

    @Provides
    @Singleton
    fun provideNextCloudRepository(@ApplicationContext context: Context): NextCloudRepository {
        return NextCloudRepositoryImp(context)
    }

}