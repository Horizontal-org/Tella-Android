package rs.readahead.washington.mobile.data.reports.repository

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.data.entity.reports.LoginEntity
import rs.readahead.washington.mobile.data.entity.reports.mapper.mapToDomainModel
import rs.readahead.washington.mobile.data.reports.remote.ReportsApiService
import rs.readahead.washington.mobile.data.reports.utils.ParamsNetwork.URL_LOGIN
import rs.readahead.washington.mobile.domain.entity.reports.ReportsLoginResult
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository
import rs.readahead.washington.mobile.util.StringUtils

class ReportsRepositoryImp constructor(private val apiService: ReportsApiService) :
    ReportsRepository {

    override fun login(server: TellaReportServer): Single<ReportsLoginResult> {
        return apiService.login(
            loginEntity = LoginEntity(server.username, server.password),
            url = StringUtils.append(
                '/',
                server.url,
                URL_LOGIN
            )
        ).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { result -> result.mapToDomainModel() }
    }
}