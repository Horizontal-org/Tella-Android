package rs.readahead.washington.mobile.data.rest

import io.reactivex.Single
import retrofit2.http.*
import rs.readahead.washington.mobile.data.entity.uwazi.LoginEntity
import rs.readahead.washington.mobile.data.entity.uwazi.TemplateResponse
import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntity
import rs.readahead.washington.mobile.domain.entity.LoginResponse

interface IUwaziApi  {
     @GET
     fun getTemplates(
        @Url url : String,
        @Header("Cookie") cookie : String
    ): Single<TemplateResponse>

    @POST
    fun login(
        @Body loginEntity: LoginEntity,
        @Url url : String
     ) : Single<LoginResponse>

    @GET
    suspend fun submitEntity(@Body uwaziEntity: UwaziEntity)

}

