package org.horizontal.tella.mobile.domain.repository.dropbox

import io.reactivex.Completable
import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer

interface ITellaDropBoxRepository {
    fun saveDropBoxServer(server: DropBoxServer): Single<DropBoxServer>
    fun updateDropBoxServer(server: DropBoxServer): Single<DropBoxServer>
    fun listDropBoxServers(): Single<List<DropBoxServer>>
    fun removeDropBoxServer(id: Long): Completable
}