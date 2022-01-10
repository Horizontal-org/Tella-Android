package rs.readahead.washington.mobile.domain.repository.uwazi

import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import rs.readahead.washington.mobile.data.entity.uwazi.LoginEntity
import rs.readahead.washington.mobile.data.entity.uwazi.TemplateResponse
import rs.readahead.washington.mobile.domain.entity.LoginResponse
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer

interface IUwaziUserRepository {
    suspend fun login(uWaziUploadServer: UWaziUploadServer) : Flow<Response<LoginResponse>>

    suspend fun getTemplates(uWaziUploadServer: UWaziUploadServer) : Flow<TemplateResponse>
}