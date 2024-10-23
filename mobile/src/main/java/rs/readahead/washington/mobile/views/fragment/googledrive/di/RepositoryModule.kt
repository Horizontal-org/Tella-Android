package rs.readahead.washington.mobile.views.fragment.googledrive.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.GoogleDriveDataSource
import rs.readahead.washington.mobile.domain.entity.googledrive.Config
import rs.readahead.washington.mobile.data.googledrive.GoogleDriveRepository
import rs.readahead.washington.mobile.domain.repository.googledrive.GoogleDriveRepositoryInterface
import rs.readahead.washington.mobile.domain.repository.googledrive.ITellaGoogleDriveRepository
import rs.readahead.washington.mobile.domain.repository.reports.ITellaReportsRepository
import java.io.InputStreamReader
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
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager {
        return CredentialManager.create(context)
    }

    @Provides
    @Singleton
    @JvmSuppressWildcards
    fun provideDriveServiceProvider(@ApplicationContext context: Context): (String) -> Drive {
        return { email ->
            val googleAccountCredential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE)
            ).apply {
                selectedAccountName = email
            }

            Drive.Builder(
                NetHttpTransport(),
                GsonFactory(),
                googleAccountCredential
            ).setApplicationName("Tella").build()
        }
    }

    @Provides
    @Singleton
    fun provideGoogleDriveRepository(
        credentialManager: CredentialManager,
        driveServiceProvider: DriveServiceProvider
    ): GoogleDriveRepositoryInterface {
        return GoogleDriveRepository(credentialManager, driveServiceProvider)
    }

    @Provides
    @Singleton
    fun provideGoogleDriveDataSource(): GoogleDriveDataSource {
        return MyApplication.getKeyDataSource().googleDriveDataSource.blockingFirst()
    }

    @Provides
    @Singleton
    fun provideServerGoogleDriveDataSource(): ITellaGoogleDriveRepository {
        return MyApplication.getKeyDataSource().googleDriveDataSource.blockingFirst()
    }

    @Provides
    @Singleton
    @GoogleDrive
    fun provideGoogleDriveDataSourceRepository(): ITellaReportsRepository {
        return MyApplication.getKeyDataSource().googleDriveDataSource.blockingFirst()
    }

    @Provides
    @Singleton
    fun provideGoogleDriveId(@ApplicationContext context: Context, gson: Gson): Config {
        val inputStream = context.assets.open("config.json")
        val reader = InputStreamReader(inputStream)
        return gson.fromJson(reader, Config::class.java)
    }
}