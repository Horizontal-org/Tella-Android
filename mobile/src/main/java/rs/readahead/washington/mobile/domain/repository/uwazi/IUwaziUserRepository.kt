package rs.readahead.washington.mobile.domain.repository.uwazi

import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import rs.readahead.washington.mobile.data.entity.uwazi.*
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.uwazi.*

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
        documents: List<MultipartBody.Part?>
    ): Single<UwaziEntityRow>
}