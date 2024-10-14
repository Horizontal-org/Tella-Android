package rs.readahead.washington.mobile.domain.repository.dropbox

import io.reactivex.Completable
import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.dropbox.DropBoxServer

interface IDropBoxRepository {
    fun saveDropBoxServer(instance: DropBoxServer): Single<DropBoxServer>
    fun listDropBoxServers(): Single<List<DropBoxServer>>
    fun removeDropBoxServer(id: Long): Completable
}