package org.horizontal.tella.mobile.domain.repository.uwazi

import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer

interface IUWAZIServersRepository {
    suspend fun listUwaziServers(): List<UWaziUploadServer>
    suspend fun createUWAZIServer(server: UWaziUploadServer): UWaziUploadServer?
    suspend fun removeUwaziServer(id: Long)
    suspend fun updateUwaziServer(server: UWaziUploadServer) : UWaziUploadServer?
    suspend fun countUwaziServers(): Long?
}