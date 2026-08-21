package org.horizontal.tella.mobile.domain.usecases.nextcloud

import io.reactivex.Single
import org.horizontal.tella.mobile.domain.repository.nextcloud.NextCloudRepository
import javax.inject.Inject

class ValidateNextcloudServerUrlUseCase @Inject constructor(
    private val repository: NextCloudRepository
) {
    operator fun invoke(serverUrl: String): Single<ValidationResult<Boolean>> {
        return repository.validateServerUrl(serverUrl)
            .map { isSuccess ->
                if (isSuccess) {
                    ValidationResult.Success(true)
                } else {
                    ValidationResult.Error(Exception("Server validation failed."))
                }
            }
            .onErrorResumeNext { throwable ->
                Single.just(ValidationResult.Error(throwable))
            }
    }
}