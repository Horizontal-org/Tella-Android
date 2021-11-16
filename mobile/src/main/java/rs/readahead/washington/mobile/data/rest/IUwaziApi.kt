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

interface IUwaziApi  {

    @GET(URL_TEMPLATES)
    fun getTemplates(): TemplateResponse

    @POST(URL_LOGIN)
    fun login(@Body loginEntity: LoginEntity)

    @GET(URL_ENTITIES)
    fun submitEntity(@Body uwaziEntity: UwaziEntity)

}

