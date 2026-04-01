package org.horizontal.tella.mobile.data.reports.remote

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.*
import org.horizontal.tella.mobile.data.entity.reports.*
import org.horizontal.tella.mobile.data.reports.utils.ParamsNetwork.FILE_INFO
import org.horizontal.tella.mobile.data.reports.utils.ParamsNetwork.CONTENT_TYPE
import org.horizontal.tella.mobile.data.reports.utils.ParamsNetwork.CONTENT_LENGTH
import org.horizontal.tella.mobile.data.reports.utils.ParamsNetwork.AUTHORIZATION_HEADER
import org.horizontal.tella.mobile.data.reports.utils.ParamsNetwork.CONTENT_RANGE
import org.horizontal.tella.mobile.data.repository.SkippableMediaFileRequestBody

interface ReportsApiService {

    companion object {
        //DUMMY URL
        const val BASE_URL = "https://www.hzontal.org/"
    }

    @POST
    fun login(
        @Body
        loginEntity: LoginEntity,
        @Url
        url: String
    ): Single<ReportsLoginResponse>

    @GET
    fun getProjectSlug(
        @Url
        url: String,
        @Header(AUTHORIZATION_HEADER) access_token: String
    ): Single<ProjectSlugResponse>

    @POST
    fun submitReport(
        @Url
        url: String,
        @Header(AUTHORIZATION_HEADER) access_token: String,
        @Body
        reportBodyEntity: ReportBodyEntity,
    ): Single<ReportPostResponse>

    @PUT
    fun putFileV2(
        @Url url: String,
        @Header(AUTHORIZATION_HEADER) accessToken: String,
        @Header(CONTENT_RANGE) contentRange: String,
        @Header(CONTENT_LENGTH) contentLength: Long,
        @Header(CONTENT_TYPE) contentType: String?,
        @Header(FILE_INFO) fileInfo: String?,
        @Body body: SkippableMediaFileRequestBody
    ): Single<Response<Void>>

    @PUT
    fun putFile(
        @Body file: SkippableMediaFileRequestBody,
        @Url url: String,
        @Header(AUTHORIZATION_HEADER) access_token: String,
    ): Single<Response<Void>>

    @POST
    fun postFile(
        @Url url: String,
        @Header(AUTHORIZATION_HEADER) access_token: String
    ): Single<Response<Void>>

    @HEAD
    fun getStatus(
        @Url url: String,
        @Header(AUTHORIZATION_HEADER) access_token: String
    ): Single<Response<Void>>
}