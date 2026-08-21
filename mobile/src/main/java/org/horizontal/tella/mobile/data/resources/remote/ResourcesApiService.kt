package org.horizontal.tella.mobile.data.resources.remote

import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.http.*
import org.horizontal.tella.mobile.data.entity.reports.*
import org.horizontal.tella.mobile.data.reports.utils.ParamsNetwork.AUTHORIZATION_HEADER

@JvmSuppressWildcards
interface ResourcesApiService {

    companion object {
        //DUMMY URL
        const val BASE_URL = "https://www.hzontal.org/"
    }

    @GET
    fun getResources(
        @Url
        url: String,
        @Header(AUTHORIZATION_HEADER) access_token: String
    ): Single<List<ProjectSlugResourceResponse>>

    @GET
    fun getResource(
        @Url
        url: String,
        @Header(AUTHORIZATION_HEADER) access_token: String
    ): Single<ResponseBody>
}