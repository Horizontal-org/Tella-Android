package rs.readahead.washington.mobile.data.resources.repository

import io.reactivex.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import rs.readahead.washington.mobile.data.entity.reports.ProjectSlugResourceResponse
import rs.readahead.washington.mobile.data.entity.reports.mapper.mapToDomainModel
import rs.readahead.washington.mobile.data.reports.utils.ParamsNetwork.URL_PROJECTS
import rs.readahead.washington.mobile.data.resources.remote.ResourcesApiService
import rs.readahead.washington.mobile.data.resources.utils.ParamsNetwork.URL_MOBILE_ASSET
import rs.readahead.washington.mobile.data.resources.utils.ParamsNetwork.URL_RESOURCE
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.entity.resources.ListResourceResult
import rs.readahead.washington.mobile.domain.repository.resources.ResourcesRepository
import javax.inject.Inject


class ResourcesRepositoryImp @Inject internal constructor(
    private val apiService: ResourcesApiService
) : ResourcesRepository {

    private val disposables = CompositeDisposable()

    /**
     * Gets list of resources from the server.
     *
     * @param projects List of Project server connections to get resources from.
     */
    override fun getResourcesResult(projects: List<TellaReportServer>): Single<List<ProjectSlugResourceResponse>> {
        val token = projects[0].accessToken
        val url = prepareResourcesUrl(projects[0].url, projects)
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

    /**
     * Gets list of resources from the server.
     *
     * @param urlServer url of the server connection.
     * @param projects List of projects on the server.
     */
    override fun getAllResourcesResult(
        urlServer: String,
        projects: ArrayList<TellaReportServer>
    ): Single<ListResourceResult> {
        val token = projects[0].accessToken
        val url = prepareResourcesUrl(urlServer, projects)
        return apiService.getResources(url, access_token = token)
            .subscribeOn(Schedulers.io())
            .map { results ->
                val slugs = mutableListOf<ProjectSlugResourceResponse>()
                results.forEach {
                    slugs.add(it.mapToDomainModel())
                }
                ListResourceResult().apply {
                    this.slugs = slugs
                }
            }
            .onErrorResumeNext { throwable: Throwable ->
                val listResourceResult = ListResourceResult()
                listResourceResult.apply {
                    this.errors = listOf(throwable)
                }
                Single.just(listResourceResult)
            }
    }

    /**
     * Gets list of resources from the server.
     *
     * @param server server connection.
     * @param filename Name of the file we are downloading.
     */
    override fun downloadResourceByFileName(
        server: TellaReportServer,
        filename: String?
    ): Single<ResponseBody> {
        val url = server.url + URL_RESOURCE + URL_MOBILE_ASSET + filename
        val token = server.accessToken
        return apiService.getResource(url, access_token = token)
            .subscribeOn(Schedulers.io())
    }

    override fun getDisposable() = disposables

    override fun cleanup() {
        disposables.clear()
    }

    /**
     *  This fun prepares url to get resources of projects from one server that hosts those projects.
     *
     * @param baseUrl url of a server.
     * @param projects List of projects that the server hosts.
     */
    private fun prepareResourcesUrl(baseUrl: String, projects: List<TellaReportServer>): String {
        var url = "$baseUrl$URL_RESOURCE$URL_PROJECTS?"
        projects.forEach {
            url = url.plus("&").plus("projectId[]=${it.projectId}")
        }
        return url
    }
}