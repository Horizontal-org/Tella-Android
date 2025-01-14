package org.horizontal.tella.mobile.domain.usecases.nextcloud

import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import io.reactivex.Single
import org.horizontal.tella.mobile.domain.repository.nextcloud.NextCloudRepository
import javax.inject.Inject

class CheckUserCredentialsUseCase @Inject constructor(private val repository: NextCloudRepository) {
    fun execute(serverUrl: String, username: String, password: String): Single<RemoteOperationResult<UserInfo?>> {
        return repository.checkUserCredentials(serverUrl, username, password)
    }
}