package rs.readahead.washington.mobile.data.rest

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url
import rs.readahead.washington.mobile.data.ParamsNetwork.URL_TEMPLATES
import rs.readahead.washington.mobile.data.entity.uwazi.LoginEntity
import rs.readahead.washington.mobile.data.entity.uwazi.TemplateResponse
import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntity
import rs.readahead.washington.mobile.domain.entity.LoginResponse

interface IUwaziApi  {

    @GET(URL_TEMPLATES)
    suspend fun getTemplates(
        @Url url : String = "https://horizontal.uwazi.io/api/"
    ): TemplateResponse

    @POST
    suspend fun login(
        @Body loginEntity: LoginEntity,
        @Url url : String
     ) : LoginResponse

    @GET
    suspend fun submitEntity(@Body uwaziEntity: UwaziEntity)

}

