package org.horizontal.tella.mobile.domain.repository.nextcloud

import io.reactivex.Completable
import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.nextcloud.NextCloudServer

interface ITellaNextCloudRepository {
    fun saveNextCloudServer(instance: NextCloudServer): Single<NextCloudServer>
    fun listNextCloudServers(): Single<List<NextCloudServer>>
    fun removeNextCloudServer(id: Long): Completable
}