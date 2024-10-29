package rs.readahead.washington.mobile.domain.repository.nextcloud

import io.reactivex.Completable
import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer

interface ITellaNextCloudRepository {
    fun saveNextCloudServer(instance: NextCloudServer): Single<NextCloudServer>
    fun listNextCloudServers(nextCloudId:String): Single<List<NextCloudServer>>
    fun removeNextCloudServer(id: Long): Completable
}