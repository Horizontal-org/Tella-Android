package org.horizontal.tella.mobile.data.peertopeer.remote

import com.hzontal.tella_vault.VaultFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile

@Serializable
data class PrepareUploadRequest(
    val title: String,
    @SerialName("sessionId") val sessionId: String,
    val files: List<FormMediaFile>
)