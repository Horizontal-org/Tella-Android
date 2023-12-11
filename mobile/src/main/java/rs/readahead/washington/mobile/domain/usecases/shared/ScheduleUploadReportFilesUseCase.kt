import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.usecases.base.CompletableUseCase
import rs.readahead.washington.mobile.util.jobs.WorkerUploadReport
import javax.inject.Inject

class ScheduleUploadReportFilesUseCase @Inject constructor(
    private val keyDataSource: KeyDataSource,
    private val vaultFile: VaultFile,
    private val uploadServerId: Long,
    private val context : Context
) : CompletableUseCase() {

    override fun buildUseCaseSingle(): Completable {
        return keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapCompletable { dataSource ->
                dataSource.scheduleUploadReport(FormMediaFile.fromMediaFile(vaultFile), uploadServerId)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val onetimeJob = OneTimeWorkRequest.Builder(WorkerUploadReport::class.java)
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context)
                    .enqueueUniqueWork("WorkerUploadReport2", ExistingWorkPolicy.KEEP, onetimeJob)

            }
            .doOnError { throwable ->
                FirebaseCrashlytics.getInstance().recordException(throwable)
            }
    }
}
