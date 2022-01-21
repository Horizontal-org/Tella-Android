package rs.readahead.washington.mobile.domain.repository.uwazi

import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.LoginResponse
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.uwazi.ListTemplateResult

interface IUwaziUserRepository {
     fun login(server: UWaziUploadServer) : Single<LoginResponse>

     fun getTemplates(server: UWaziUploadServer) : Single<ListTemplateResult>
}