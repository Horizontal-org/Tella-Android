package org.horizontal.tella.mobile.domain.usecases.shared

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import org.hzontal.tella.keys.key.LifecycleMainKey
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.util.jobs.WorkerUploadReport
import javax.inject.Inject

class ScheduleUploadReportFilesUseCase @Inject constructor(
    private val keyDataSource: KeyDataSource,
    @ApplicationContext
    private val context : Context
) {

    fun execute(vaultFile: VaultFile, uploadServerId: Long): Completable {
        return keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .flatMapCompletable { dataSource ->
                val formMediaFile = FormMediaFile.fromMediaFile(vaultFile)
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val oneTimeJob = OneTimeWorkRequest.Builder(WorkerUploadReport::class.java)
                    .setConstraints(constraints)
                    .build()

                // Schedule the upload report work using WorkManager
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "WorkerUploadReport2",
                    ExistingWorkPolicy.KEEP,
                    oneTimeJob
                )

                // Update the main key holder's timeout
                MyApplication.getMainKeyHolder().timeout = LifecycleMainKey.NO_TIMEOUT

                // Perform the upload report operation and complete the Completable
                dataSource.scheduleUploadReport(formMediaFile, uploadServerId)
            }
            .doOnError { throwable ->
                FirebaseCrashlytics.getInstance().recordException(throwable)
            }
    }

}

