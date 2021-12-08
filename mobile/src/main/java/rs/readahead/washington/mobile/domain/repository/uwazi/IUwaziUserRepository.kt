package rs.readahead.washington.mobile.domain.repository.uwazi

import kotlinx.coroutines.flow.Flow
import rs.readahead.washington.mobile.data.entity.uwazi.LoginEntity
import rs.readahead.washington.mobile.domain.entity.LoginResponse

interface IUwaziUserRepository {

    suspend fun login(loginEntity: LoginEntity) : Flow<LoginResponse>
}