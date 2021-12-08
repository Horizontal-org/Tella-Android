package rs.readahead.washington.mobile.data.rest

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import rs.readahead.washington.mobile.data.URL_ENTITIES
import rs.readahead.washington.mobile.data.URL_LOGIN
import rs.readahead.washington.mobile.data.URL_TEMPLATES
import rs.readahead.washington.mobile.data.entity.uwazi.LoginEntity
import rs.readahead.washington.mobile.data.entity.uwazi.TemplateResponse
import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntity
import rs.readahead.washington.mobile.domain.entity.LoginResponse

interface IUwaziApi  {

    @GET(URL_TEMPLATES)
    suspend fun getTemplates(): TemplateResponse

    @POST(URL_LOGIN)
    suspend fun login(@Body loginEntity: LoginEntity) : LoginResponse

    @GET(URL_ENTITIES)
    suspend fun submitEntity(@Body uwaziEntity: UwaziEntity)

}

