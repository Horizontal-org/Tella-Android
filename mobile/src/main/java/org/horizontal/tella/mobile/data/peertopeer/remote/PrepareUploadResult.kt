package org.horizontal.tella.mobile.data.peertopeer.remote

import org.horizontal.tella.mobile.domain.peertopeer.FileInfo


/**
 * Created by wafa on 2/7/2025.
 */
sealed class PrepareUploadResult {
    data class Success(val transmissions: List<FileInfo>) : PrepareUploadResult()
    data object Forbidden : PrepareUploadResult()
    data object BadRequest : PrepareUploadResult()
    data object Conflict : PrepareUploadResult()
    data object ServerError : PrepareUploadResult()
    data class Failure(val exception: Throwable) : PrepareUploadResult()
}