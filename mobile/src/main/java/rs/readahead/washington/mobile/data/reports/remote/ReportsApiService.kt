package rs.readahead.washington.mobile.data.reports.remote

import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url
import rs.readahead.washington.mobile.data.entity.reports.LoginEntity
import rs.readahead.washington.mobile.data.entity.reports.ReportsLoginResponse

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
}