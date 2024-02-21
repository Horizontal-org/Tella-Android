package rs.readahead.washington.mobile.domain.repository.resources

import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import okhttp3.ResponseBody
import rs.readahead.washington.mobile.data.entity.reports.ProjectSlugResourceResponse
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer

interface ResourcesRepository {

    fun getResourcesResult(projects: List<TellaReportServer>): Single<List<ProjectSlugResourceResponse>>

    fun getAllResourcesResult(urlServer: String, projects: ArrayList<TellaReportServer>): Single<List<ProjectSlugResourceResponse>>

    fun downloadResourceByFileName(server: TellaReportServer, filename : String?): Single<ResponseBody>

    fun getDisposable(): CompositeDisposable

    fun cleanup()
}