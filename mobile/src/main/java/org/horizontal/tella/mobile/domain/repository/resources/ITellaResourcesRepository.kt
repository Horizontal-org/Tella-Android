package org.horizontal.tella.mobile.domain.repository.resources

import io.reactivex.Completable
import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.domain.entity.resources.Resource

interface ITellaResourcesRepository {

    fun listResources(): Single<List<Resource>?>

    fun removeTellaServerAndResources(id: Long): Completable

    fun saveResource(instance: Resource): Single<Resource>

    fun deleteResource(resource: Resource): Single<String>

    fun listTellaUploadServers(): Single<List<TellaReportServer>>

    fun getTellaUploadServer(id: Long): Single<TellaReportServer>
}