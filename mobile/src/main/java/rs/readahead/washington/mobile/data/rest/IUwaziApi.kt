package rs.readahead.washington.mobile.data.rest

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.*
import rs.readahead.washington.mobile.data.entity.uwazi.*
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

    @POST
    fun submitEntity(
        @Body uwaziEntityRow: UwaziEntityRow,
        @Url url: String,
        @Header("Cookie") cookie: String,
        @Header("X-Requested-With") requested: String = "XMLHttpRequest"
    ) : Single<UwaziEntityRow>

}

