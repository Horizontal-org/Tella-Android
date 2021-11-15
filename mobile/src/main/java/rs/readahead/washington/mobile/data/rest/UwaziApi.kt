package rs.readahead.washington.mobile.data.rest

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import rs.readahead.washington.mobile.data.entity.uwazi.LoginEntity
import rs.readahead.washington.mobile.data.entity.uwazi.Template

interface UwaziApi {

    @GET("templates")
    fun getTemplates(): Template

    @POST("login")
    fun login(@Body loginEntity: LoginEntity)
}

