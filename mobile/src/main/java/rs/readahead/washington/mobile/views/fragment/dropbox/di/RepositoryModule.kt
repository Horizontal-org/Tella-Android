package rs.readahead.washington.mobile.views.fragment.dropbox.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.DropBoxDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideServerDropBoxDataSource(): DropBoxDataSource {
        return MyApplication.getKeyDataSource().dropBoxDataSource.blockingFirst()
    }

}