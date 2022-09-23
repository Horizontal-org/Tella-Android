package rs.readahead.washington.mobile.data.reports.remote

import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url
import rs.readahead.washington.mobile.data.entity.reports.LoginEntity
import rs.readahead.washington.mobile.data.entity.reports.ReportsLoginResponse

interface ReportsApiService {
    companion object {
        const val BASE_URL = ""
    }

    @POST
    fun login(
        @Body
        loginEntity: LoginEntity,
        @Url
        url: String
    ): Single<ReportsLoginResponse>
}