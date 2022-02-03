package rs.readahead.washington.mobile.domain.repository.uwazi

import com.hzontal.tella_vault.VaultFile
import io.reactivex.Flowable
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Part
import rs.readahead.washington.mobile.data.entity.uwazi.Language
import rs.readahead.washington.mobile.data.entity.uwazi.LanguageSettingsEntity
import rs.readahead.washington.mobile.data.entity.uwazi.SettingsResponse
import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntityRow
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.uwazi.ListTemplateResult
import rs.readahead.washington.mobile.domain.entity.uwazi.LoginResult
import rs.readahead.washington.mobile.presentation.uwazi.SendEntityRequest

interface IUwaziUserRepository {

     fun login(server: UWaziUploadServer) : Single<LoginResult>

     fun getTemplates(server: UWaziUploadServer) : Single<ListTemplateResult>

     fun getSettings(server: UWaziUploadServer) : Single<List<Language>>

     fun updateDefaultLanguage(languageSettingsEntity: LanguageSettingsEntity, server: UWaziUploadServer) : Single<SettingsResponse>

     fun submitEntity(
                      server: UWaziUploadServer,
                      title: RequestBody,
                      template: RequestBody,
                      type: RequestBody,
                      metadata : RequestBody,
                      attachments : List<MultipartBody.Part?> ) : Flowable<UwaziEntityRow>
}