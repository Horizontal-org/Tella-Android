package rs.readahead.washington.mobile.views.common_di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import rs.readahead.washington.mobile.util.divviup.DivviupUtils
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object CommonAppModule {

    @Singleton
    @Provides
    fun providesDivviupUtils(@ApplicationContext context: Context): DivviupUtils {
        return DivviupUtils(context)
    }
}