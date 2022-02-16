package rs.readahead.washington.mobile.domain.repository.uwazi

import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import rs.readahead.washington.mobile.data.entity.uwazi.*
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.uwazi.ListTemplateResult
import rs.readahead.washington.mobile.domain.entity.uwazi.RowDictionary
import rs.readahead.washington.mobile.domain.entity.uwazi.TranslationRow
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziRow

interface IUwaziUserRepository {

    fun login(server: UWaziUploadServer): Single<LoginResult>

    fun getTemplatesResult(server: UWaziUploadServer) :  Single<ListTemplateResult>

    fun getTemplates(server: UWaziUploadServer): Single<List<UwaziRow>>

    fun getSettings(server: UWaziUploadServer): Single<List<Language>>

    fun getDictionary(server: UWaziUploadServer): Single<List<RowDictionary>>

    fun getTranslation(server: UWaziUploadServer): Single<List<TranslationRow>>

    fun submitEntity(
        server: UWaziUploadServer,
        entity: RequestBody,
        attachments: List<MultipartBody.Part?>
    ): Single<UwaziEntityRow>
}