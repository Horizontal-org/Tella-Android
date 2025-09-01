package org.horizontal.tella.mobile.data.rest

import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import org.horizontal.tella.mobile.data.ParamsNetwork.BYPASS_CAPTCHA_HEADER
import org.horizontal.tella.mobile.data.ParamsNetwork.COOKIE
import org.horizontal.tella.mobile.data.ParamsNetwork.X_REQUESTED_WITH
import org.horizontal.tella.mobile.data.entity.uwazi.*
import org.horizontal.tella.mobile.domain.entity.LoginResponse

interface IUwaziApi {
    @GET
    fun getTemplates(
        @Url url: String,
        @Header(COOKIE) cookies: List<String>
    ): Single<TemplateResponse>

    @POST
    fun login(
        @Body loginEntity: LoginEntity,
        @Url url: String,
        @Header(X_REQUESTED_WITH) requested: String = "XMLHttpRequest"
    ): Single<Response<LoginResponse>>

    @GET
    fun getRelationShipEntities(
        @Url url: String,
        @Header(COOKIE) cookies: String
    ): Single<RelationShipEntitiesResponse>

    @GET
    fun getSettings(
        @Url url: String,
        @Header(COOKIE) cookies: List<String>
    ): Single<SettingsResponse>

    @GET
    fun getDictionary(
        @Url url: String,
        @Header(COOKIE) cookies: List<String>
    ): Single<DictionaryResponse>


    @GET
    fun getTranslations(
        @Url url: String,
        @Header(COOKIE) cookies: List<String>
    ): Single<TranslationResponse>


    @Multipart
    @POST
    fun submitEntity(
        @Part attachments: List<MultipartBody.Part?>,
        @Part documents: List<MultipartBody.Part?>,
        @Part("attachments_originalname") attachmentsOriginalName: List<String>,
        @Part("entity") entity: RequestBody,
        @Url url: String,
        @Header(COOKIE) cookies: List<String>,
        @Header(X_REQUESTED_WITH) requested: String = "XMLHttpRequest"
    ): Single<UwaziEntityRow>

    @Multipart
    @POST
    fun submitWhiteListedEntity(
        @Part attachments: List<MultipartBody.Part?>,
        @Part documents: List<MultipartBody.Part?>,
        @Part("entity") entity: RequestBody,
        @Url url: String,
        @Header(COOKIE) cookies: List<String>,
        @Header(X_REQUESTED_WITH) requested: String = "XMLHttpRequest",
        @Header(BYPASS_CAPTCHA_HEADER) bypassCaptcha: Boolean = true
    ):  Single<Response<Void>>
}

