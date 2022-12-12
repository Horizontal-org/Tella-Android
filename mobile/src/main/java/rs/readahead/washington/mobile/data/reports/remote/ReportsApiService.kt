package rs.readahead.washington.mobile.data.reports.remote

import io.reactivex.Single
import okhttp3.MultipartBody
import retrofit2.http.*
import rs.readahead.washington.mobile.data.ParamsNetwork.COOKIE
import rs.readahead.washington.mobile.data.entity.reports.LoginEntity
import rs.readahead.washington.mobile.data.entity.reports.ProjectSlugResponse
import rs.readahead.washington.mobile.data.entity.reports.ReportBodyEntity
import rs.readahead.washington.mobile.data.entity.reports.ReportsLoginResponse
import rs.readahead.washington.mobile.domain.entity.reports.ProjectResult
import rs.readahead.washington.mobile.domain.entity.reports.ProjectSlugResult
import rs.readahead.washington.mobile.domain.entity.reports.ReportPostResult

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
        @Header(COOKIE) access_token: String
    ): Single<ProjectSlugResponse>

    @POST
    fun submitReport(
        @Body
        reportBodyEntity: ReportBodyEntity,
        @Url
        url: String,
        @Header(COOKIE) access_token: String
    ): Single<ReportPostResult>

    //TODO AHLEM REMOVE ALL HARDCODED :)
    @GET
    fun getProjects(
        @Url
        url: String,
        @Query("limit")
        limit: Int,
        @Query("offset")
        offset: Int,
        @Header(COOKIE) access_token: String
    ): Single<List<ProjectResult>>

    @Multipart
    @PUT
    fun putFile(
        @Part("file") file: MultipartBody.Part?,
        @Url url: String,
        @Header(COOKIE) access_token: String
    ): Single<ReportPostResult>
}