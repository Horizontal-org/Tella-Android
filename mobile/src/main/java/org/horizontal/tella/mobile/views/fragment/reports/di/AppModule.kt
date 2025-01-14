package org.horizontal.tella.mobile.views.fragment.reports.di

import androidx.fragment.app.Fragment
import com.hzontal.tella_vault.rx.RxVault
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.database.KeyDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    fun provideNavControllerProvider(fragment: Fragment): NavControllerProvider {
        return NavControllerProvider(fragment)
    }

    @Provides
    @Singleton
    fun provideKeyDataSource(): KeyDataSource {
        return MyApplication.getKeyDataSource()
    }

    @Provides
    @Singleton
    fun providesRxVault(): RxVault {
        return MyApplication.rxVault
    }

}