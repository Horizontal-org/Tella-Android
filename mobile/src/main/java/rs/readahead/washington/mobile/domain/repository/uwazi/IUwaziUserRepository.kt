package rs.readahead.washington.mobile.domain.repository.uwazi

import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import rs.readahead.washington.mobile.data.entity.uwazi.Language
import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntityRow
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.uwazi.ListTemplateResult
import rs.readahead.washington.mobile.domain.entity.uwazi.LoginResult

interface IUwaziUserRepository {

     fun login(server: UWaziUploadServer) : Single<LoginResult>

     fun getTemplates(server: UWaziUploadServer) : Single<ListTemplateResult>

     fun getSettings(server: UWaziUploadServer) : Single<List<Language>>

    fun submitEntity(
        server: UWaziUploadServer,
        entity: RequestBody,
        attachments : List<MultipartBody.Part?> ) : Single<UwaziEntityRow>
}