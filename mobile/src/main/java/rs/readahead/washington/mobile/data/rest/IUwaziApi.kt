package rs.readahead.washington.mobile.data.rest

import retrofit2.http.*
import rs.readahead.washington.mobile.data.entity.uwazi.LoginEntity
import rs.readahead.washington.mobile.data.entity.uwazi.TemplateResponse
import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntity
import rs.readahead.washington.mobile.domain.entity.LoginResponse

interface IUwaziApi  {
    @GET
    suspend fun getTemplates(
        @Url url : String,
        @Header("Cookie") cookie : String
    ): TemplateResponse

    @POST
    suspend fun login(
        @Body loginEntity: LoginEntity,
        @Url url : String
     ) : LoginResponse

    @GET
    suspend fun submitEntity(@Body uwaziEntity: UwaziEntity)

}

