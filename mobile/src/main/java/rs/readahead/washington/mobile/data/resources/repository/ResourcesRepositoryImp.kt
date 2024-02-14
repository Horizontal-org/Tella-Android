package rs.readahead.washington.mobile.data.resources.repository

import android.os.Build
import androidx.annotation.RequiresApi
import io.reactivex.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.entity.reports.ProjectSlugResourceResponse
import rs.readahead.washington.mobile.data.entity.reports.mapper.mapToDomainModel
import rs.readahead.washington.mobile.data.reports.utils.ParamsNetwork.URL_PROJECTS
import rs.readahead.washington.mobile.data.resources.remote.ResourcesApiService
import rs.readahead.washington.mobile.data.resources.utils.ParamsNetwork.URL_MOBILE_ASSET
import rs.readahead.washington.mobile.data.resources.utils.ParamsNetwork.URL_RESOURCE
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.repository.resources.ResourcesRepository
import javax.inject.Inject


class ResourcesRepositoryImp @Inject internal constructor(
    private val apiService: ResourcesApiService,
    private val dataSource: DataSource
) : ResourcesRepository {

    private val disposables = CompositeDisposable()

    /**
     * Gets list of resources from the server.
     *
     * @param server Project server connection to get resources from.
     */
    override fun getResourcesResult(servers:List<TellaReportServer>): Single<List<ProjectSlugResourceResponse>> {
        //use proper Builder
        var url = servers[0].url + URL_RESOURCE + URL_PROJECTS + '?'
        val token = servers[0].accessToken
        servers.forEach {
            url = url.plus("&").plus("projectId[]=${it.projectId}")
        }
        return apiService.getResources(url, access_token = token)
            .subscribeOn(Schedulers.io())
            .map { results ->

                val slugs = mutableListOf<ProjectSlugResourceResponse>()
                results.forEach {
                    slugs.add(it.mapToDomainModel())
                }

                return@map slugs
            }
    }

    override fun downloadResource(server: TellaReportServer, filename: String?): Single<ResponseBody> {
        val url = server.url + URL_RESOURCE + URL_MOBILE_ASSET + filename
        val token = server.accessToken
        return apiService.getResource(url, access_token = token)
            .subscribeOn(Schedulers.io())
    }

    override fun getDisposable() = disposables

    override fun cleanup() {
        disposables.clear()
    }
}