package rs.readahead.washington.mobile.domain.usecases.nextcloud

sealed class ValidationResult<out T> {
    data class Success<out T>(val data: T) : ValidationResult<T>()
    data class Error(val exception: Throwable) : ValidationResult<Nothing>()
}