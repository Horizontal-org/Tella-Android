package org.horizontal.tella.mobile.data.resources.repository

import io.reactivex.*
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.horizontal.tella.mobile.data.entity.reports.ProjectSlugResourceResponse
import org.horizontal.tella.mobile.data.entity.reports.mapper.mapToDomainModel
import org.horizontal.tella.mobile.data.reports.utils.ParamsNetwork.URL_PROJECTS
import org.horizontal.tella.mobile.data.resources.remote.ResourcesApiService
import org.horizontal.tella.mobile.data.resources.utils.ParamsNetwork.URL_MOBILE_ASSET
import org.horizontal.tella.mobile.data.resources.utils.ParamsNetwork.URL_RESOURCE
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.domain.entity.resources.ListResourceResult
import org.horizontal.tella.mobile.domain.exception.NotFountException
import org.horizontal.tella.mobile.domain.repository.resources.ResourcesRepository
import javax.inject.Inject


class ResourcesRepositoryImp @Inject internal constructor(
    private val apiService: ResourcesApiService
) : ResourcesRepository {

    /**
     * Gets list of resources from the server.
     *
     * @param projects List of Project server connections to get resources from.
     */
    override fun getResourcesResult(projects: List<TellaReportServer>): Single<List<ProjectSlugResourceResponse>> {
        if (projects.isEmpty()) {
            return Single.error(NotFountException())
        }

        val token = projects[0].accessToken
        val url = prepareResourcesUrl(projects[0].url, projects)
        return apiService.getResources(url, access_token = token)
            .subscribeOn(Schedulers.io())
            .map { results ->
                results.map { it.mapToDomainModel() }
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
        if (projects.isEmpty()) {
            return Single.error(NotFountException())
        }

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