package rs.readahead.washington.mobile.domain.repository.uwazi

import rs.readahead.washington.mobile.data.entity.uwazi.LoginEntity

interface IUwaziUserRepository {

    fun login(loginEntity: LoginEntity)
}