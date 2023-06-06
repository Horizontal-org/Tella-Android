package rs.readahead.washington.mobile.views.fragment.reports.di

import androidx.fragment.app.Fragment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    fun provideNavControllerProvider(fragment: Fragment): NavControllerProvider {
        return NavControllerProvider(fragment)
    }
}