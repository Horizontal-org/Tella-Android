package org.horizontal.tella.mobile.domain.repository.resources

import io.reactivex.Single
import okhttp3.ResponseBody
import org.horizontal.tella.mobile.data.entity.reports.ProjectSlugResourceResponse
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.domain.entity.resources.ListResourceResult

interface ResourcesRepository {

    fun getResourcesResult(projects: List<TellaReportServer>): Single<List<ProjectSlugResourceResponse>>

    fun getAllResourcesResult(urlServer: String, projects: ArrayList<TellaReportServer>): Single<ListResourceResult>

    fun downloadResourceByFileName(server: TellaReportServer, filename : String?): Single<ResponseBody>
}