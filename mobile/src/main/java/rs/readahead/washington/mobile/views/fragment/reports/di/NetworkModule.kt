package rs.readahead.washington.mobile.views.fragment.reports.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import rs.readahead.washington.mobile.data.reports.remote.ReportsApiService
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {

    @Singleton
    @Provides
    fun providesApiService(retrofit: Retrofit) : ReportsApiService = retrofit.create(ReportsApiService::class.java)

    @Provides
    fun providesOkHttpClient(): OkHttpClient {
        val client =  OkHttpClient()
            .newBuilder()

        return client.build()
    }

    @Singleton
    @Provides
    fun providesRetrofit(httpClient: OkHttpClient): Retrofit = Retrofit.Builder().baseUrl(ReportsApiService.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()
}