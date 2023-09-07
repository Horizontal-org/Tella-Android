package rs.readahead.washington.mobile.mvp.presenter

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.hzontal.tella.keys.key.LifecycleMainKey
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.util.jobs.WorkerUploadReport
import javax.inject.Inject

@HiltViewModel
class TellaFileUploadSchedulerViewModel @Inject constructor(val application: Application) :
    ViewModel() {
    private val disposables = CompositeDisposable()
    private val keyDataSource = MyApplication.getKeyDataSource()

    // LiveData to communicate with the view
    private val _mediaFilesUploadScheduled = SingleLiveEvent<Boolean>()
    val mediaFilesUploadScheduled: LiveData<Boolean>
        get() = _mediaFilesUploadScheduled

    private val _mediaFilesUploadScheduleError = MutableLiveData<Throwable>()
    val mediaFilesUploadScheduleError: LiveData<Throwable>
        get() = _mediaFilesUploadScheduleError

    //TODO MAYBE WE SHOULD TRANSFORM THESE INTO A USECASE ?
    fun scheduleUploadReportFiles(vaultFile: VaultFile, uploadServerId: Long) {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapCompletable { dataSource ->
                val formMediaFile = FormMediaFile.fromMediaFile(vaultFile)
                dataSource.scheduleUploadReport(formMediaFile, uploadServerId)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val oneTimeJob = OneTimeWorkRequest.Builder(WorkerUploadReport::class.java)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(application)
                .enqueueUniqueWork("WorkerUploadReport2", ExistingWorkPolicy.KEEP, oneTimeJob)

                MyApplication.getMainKeyHolder().timeout = LifecycleMainKey.NO_TIMEOUT

                _mediaFilesUploadScheduled.postValue(true)
            }, { throwable ->
                FirebaseCrashlytics.getInstance().recordException(throwable)
                _mediaFilesUploadScheduleError.postValue(throwable)
            })
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}