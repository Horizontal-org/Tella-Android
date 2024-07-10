package rs.readahead.washington.mobile.domain.usecases.nextcloud

import rs.readahead.washington.mobile.domain.repository.nextcloud.NextCloudRepository
import javax.inject.Inject

class ValidateNextcloudServerUrlUseCase @Inject constructor(private val repository: NextCloudRepository) {
    operator fun invoke(serverUrl: String, callback: (Boolean) -> Unit) {
        repository.validateServerUrl(serverUrl, callback)
    }
}