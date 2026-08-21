package org.horizontal.tella.mobile.views.fragment.reports.di

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.ProcessLifecycleOwner
import com.hzontal.tella_vault.BaseVault.Config
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.KeyRxVault
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.sharedpref.Preferences
import org.horizontal.tella.mobile.util.C
import org.hzontal.tella.keys.key.LifecycleMainKey
import java.io.File
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
    fun provideVaultConfig(@ApplicationContext context: Context): Config {
        val vaultConfig = Config()
        vaultConfig.root = File(context.filesDir, C.MEDIA_DIR)
        return vaultConfig
    }

    @Provides
    @Singleton
    fun provideLifecycleMainKey(): LifecycleMainKey {
        return LifecycleMainKey(
            ProcessLifecycleOwner.get().lifecycle,
            Preferences.getLockTimeout()
        )
    }

    @Provides
    @Singleton
    fun provideKeyRxVault(
        @ApplicationContext context: Context,
        mainKeyHolder: LifecycleMainKey,
        vaultConfig: Config
    ): KeyRxVault {
        return KeyRxVault(context, mainKeyHolder, vaultConfig)
    }

}