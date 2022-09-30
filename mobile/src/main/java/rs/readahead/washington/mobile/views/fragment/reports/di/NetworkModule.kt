package rs.readahead.washington.mobile.views.fragment.reports.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import rs.readahead.washington.mobile.BuildConfig
import rs.readahead.washington.mobile.data.reports.remote.ReportsApiService
import rs.readahead.washington.mobile.data.reports.remote.ReportsApiService.Companion.BASE_URL
import rs.readahead.washington.mobile.data.uwazi.UvCookieJar
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
            .cookieJar(UvCookieJar());
        if (BuildConfig.DEBUG) {
            client.addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))
        }
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
    fun providesGson(): Gson {
        return Gson()
    }

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
}