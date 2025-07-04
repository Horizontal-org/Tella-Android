package org.horizontal.tella.mobile.data.peertopeer.remote

/**
 * Created by wafa on 2/7/2025.
 */
sealed class PrepareUploadResult {
    data class Success(val transmissionId: String) : PrepareUploadResult()
    data object Forbidden : PrepareUploadResult()
    data object BadRequest : PrepareUploadResult()
    data object Conflict : PrepareUploadResult()
    data object ServerError : PrepareUploadResult()
    data class Failure(val exception: Throwable) : PrepareUploadResult()
}