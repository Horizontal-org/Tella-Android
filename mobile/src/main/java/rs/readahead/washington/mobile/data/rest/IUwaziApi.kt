package rs.readahead.washington.mobile.data.rest

import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import rs.readahead.washington.mobile.data.entity.uwazi.*
import rs.readahead.washington.mobile.domain.entity.LoginResponse
import rs.readahead.washington.mobile.presentation.uwazi.SendEntityRequest

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
     ) : Single<Response<LoginResponse>>


    @GET
    fun getSettings(
        @Url url : String,
        @Header("Cookie") cookie : String
    ) : Single<SettingsResponse>

    @POST
    fun updateDefaultLanguage(
        @Body languageSettingsEntity: LanguageSettingsEntity,
        @Url url : String,
        @Header("Cookie") cookie : String,
        @Header("X-Requested-With") requested: String = "XMLHttpRequest"
    ) : Single<SettingsResponse>

    @Multipart
    @POST
    fun submitEntity(
        @Part attachments : List<MultipartBody.Part?>,
        @Part("title") title: RequestBody,
        @Part("template") template: RequestBody,
        @Part("type") type: RequestBody,
        @Part("metadata")  metadata : RequestBody?,
        @Url url: String,
        @Header("Cookie") cookie: String,
        @Header("X-Requested-With") requested: String = "XMLHttpRequest"
    ) : Single<UwaziEntityRow>

}

