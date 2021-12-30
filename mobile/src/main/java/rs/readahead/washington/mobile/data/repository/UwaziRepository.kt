package rs.readahead.washington.mobile.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import rs.readahead.washington.mobile.data.ParamsNetwork
import rs.readahead.washington.mobile.data.entity.uwazi.LoginEntity
import rs.readahead.washington.mobile.data.entity.uwazi.TemplateResponse
import rs.readahead.washington.mobile.data.uwazi.UwaziService
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.repository.uwazi.IUwaziUserRepository
import rs.readahead.washington.mobile.util.StringUtils

class UwaziRepository : IUwaziUserRepository {

    private val uwaziApi by lazy {UwaziService.newInstance().services}

    override suspend fun login(uWaziUploadServer: UWaziUploadServer) =
            flow {
               val loginResponse =
                   uwaziApi.login(
                       loginEntity = LoginEntity(uWaziUploadServer.username,uWaziUploadServer.password),
                       url = StringUtils.append(
                           '/',
                           uWaziUploadServer.url,
                           ParamsNetwork.URL_LOGIN
                       )

                   )
                emit(loginResponse)
            }.flowOn(Dispatchers.IO)


    override suspend fun getTemplates(): Flow<TemplateResponse> =
        flow {
            val templateResponse = uwaziApi.getTemplates()
            emit(templateResponse)
        }.flowOn(Dispatchers.IO)


}