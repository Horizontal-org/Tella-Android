package rs.readahead.washington.mobile.views.fragment.reports.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.reports.remote.ReportsApiService
import rs.readahead.washington.mobile.data.reports.repository.ReportsRepositoryImp
import rs.readahead.washington.mobile.domain.repository.ITellaUploadServersRepository
import rs.readahead.washington.mobile.domain.repository.googledrive.GoogleDriveRepository
import rs.readahead.washington.mobile.domain.repository.googledrive.GoogleDriveRepositoryInterface
import rs.readahead.washington.mobile.domain.repository.reports.ITellaReportsRepository
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository
import rs.readahead.washington.mobile.util.StatusProvider
import rs.readahead.washington.mobile.views.fragment.googledrive.di.DriveServiceProvider
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

}