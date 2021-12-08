package rs.readahead.washington.mobile.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import rs.readahead.washington.mobile.data.entity.uwazi.LoginEntity
import rs.readahead.washington.mobile.data.uwazi.UwaziService
import rs.readahead.washington.mobile.domain.entity.LoginResponse
import rs.readahead.washington.mobile.domain.repository.uwazi.IUwaziUserRepository

class UwaziRepository (val api : UwaziService) : IUwaziUserRepository {

    override suspend fun login(loginEntity: LoginEntity) =
            flow {
               val loginResponse =  api.services.getApi().login(loginEntity)
                emit(loginResponse)
            }.flowOn(Dispatchers.IO)



}