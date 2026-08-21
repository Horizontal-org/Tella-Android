package org.horizontal.tella.mobile.domain.repository.uwazi

import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.horizontal.tella.mobile.data.entity.uwazi.*
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.domain.entity.uwazi.*
import retrofit2.Response

interface IUwaziUserRepository {

    fun login(server: UWaziUploadServer): Single<LoginResult>

    fun getTemplatesResult(server: UWaziUploadServer) :  Single<ListTemplateResult>

    fun getTemplates(server: UWaziUploadServer): Single<List<UwaziRow>>

    fun getTemplates(url: String): Single<List<UwaziRow>>

    fun getSettings(server: UWaziUploadServer): Single<List<LanguageEntity>>

    fun getSettings(url: String): Single<Settings>

    fun getFullSettings(server: UWaziUploadServer): Single<Settings>

    fun getDictionary(server: UWaziUploadServer): Single<List<RowDictionary>>

    fun getTranslation(server: UWaziUploadServer): Single<List<TranslationRow>>

    fun submitEntity(
        server: UWaziUploadServer,
        entity: RequestBody,
        attachments: List<MultipartBody.Part?>,
        attachmentsOriginalName: List<String>,
        documents: List<MultipartBody.Part?>
    ): Single<UwaziEntityRow>

    fun submitWhiteListedEntity(
        server: UWaziUploadServer,
        entity: RequestBody,
        attachments: List<MultipartBody.Part?>,
        attachmentsOriginalName: List<String>,
        documents: List<MultipartBody.Part?>
    ): Single<Response<Void>>

    fun getRelationShipEntities(server: UWaziUploadServer): Single<List<RelationShipRow>>
}