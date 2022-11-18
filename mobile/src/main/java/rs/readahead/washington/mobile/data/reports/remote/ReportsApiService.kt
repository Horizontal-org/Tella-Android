package rs.readahead.washington.mobile.data.reports.remote

import io.reactivex.Single
import okhttp3.MultipartBody
import retrofit2.http.*
import rs.readahead.washington.mobile.data.ParamsNetwork.COOKIE
import rs.readahead.washington.mobile.data.entity.reports.LoginEntity
import rs.readahead.washington.mobile.data.entity.reports.ReportBodyEntity
import rs.readahead.washington.mobile.data.entity.reports.ReportsLoginResponse
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
        @Query("limit")
        limit: Int,
        @Query("offset")
        offset: Int,
        @Url
        url: String,
        @Header(COOKIE) access_token: String
    ): Single<ReportPostResult>

    @Multipart
    @PUT
    fun putFile(
        @Part("file") file: MultipartBody.Part?,
        @Url url: String,
        @Header(COOKIE) access_token: String
    ): Single<ReportPostResult>
}