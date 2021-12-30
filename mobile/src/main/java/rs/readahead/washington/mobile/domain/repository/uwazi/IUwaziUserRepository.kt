package rs.readahead.washington.mobile.domain.repository.uwazi

import kotlinx.coroutines.flow.Flow
import rs.readahead.washington.mobile.data.entity.uwazi.LoginEntity
import rs.readahead.washington.mobile.data.entity.uwazi.TemplateResponse
import rs.readahead.washington.mobile.domain.entity.LoginResponse
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer

interface IUwaziUserRepository {
    suspend fun login(uWaziUploadServer: UWaziUploadServer) : Flow<LoginResponse>

    suspend fun getTemplates() : Flow<TemplateResponse>
}