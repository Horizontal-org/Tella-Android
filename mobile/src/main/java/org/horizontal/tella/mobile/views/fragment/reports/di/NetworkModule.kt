package org.horizontal.tella.mobile.views.fragment.reports.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import org.horizontal.tella.mobile.data.reports.remote.ReportsApiService
import org.horizontal.tella.mobile.data.reports.remote.ReportsApiService.Companion.BASE_URL
import org.horizontal.tella.mobile.data.uwazi.UvCookieJar
import org.horizontal.tella.mobile.util.StatusProvider
import org.horizontal.tella.mobile.util.StatusProviderImpl
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {

    @Singleton
    @Provides
    fun providesApiService(retrofit: Retrofit): ReportsApiService =
        retrofit.create(ReportsApiService::class.java)

    @Provides
    fun providesOkHttpClient(): OkHttpClient {
        val client = OkHttpClient()
            .newBuilder()
            .cookieJar(UvCookieJar())
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)

        return client.build()
    }

    @Provides
    @Singleton
    fun providesRetrofit(
        gsonConverterFactory: GsonConverterFactory,
        rxJava2CallAdapterFactory: RxJava2CallAdapterFactory,
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(gsonConverterFactory)
            .addCallAdapterFactory(rxJava2CallAdapterFactory)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun providesGson() = Gson()

    @Provides
    @Singleton
    fun providesGsonConverterFactory(): GsonConverterFactory {
        return GsonConverterFactory.create()
    }

    @Provides
    @Singleton
    fun providesRxJavaCallAdapterFactory(): RxJava2CallAdapterFactory {
        return RxJava2CallAdapterFactory.create()
    }

    @Provides
    @Singleton
    fun providesStatusProvider(@ApplicationContext context: Context): StatusProvider {
        return StatusProviderImpl(context)
    }

}