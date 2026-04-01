package org.horizontal.tella.mobile.mvvm.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import org.horizontal.tella.mobile.util.crash.CrashReporterProvider
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.hzontal.tella.keys.key.LifecycleMainKey
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.util.jobs.WorkerUploadReport
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

                // APPEND_OR_REPLACE ensures a new worker is queued even when one is already running.
                // Without this, files captured during an active upload would be silently dropped by
                // WorkManager and never uploaded. This trades off strict 1-hour report grouping
                // (files captured mid-upload go into a separate auto-report) for correctness.
                // TODO: revisit if same-report grouping during active uploads becomes a requirement.
                WorkManager.getInstance(application)
                .enqueueUniqueWork("WorkerUploadReport2", ExistingWorkPolicy.APPEND_OR_REPLACE, oneTimeJob)

                MyApplication.getMainKeyHolder().timeout = LifecycleMainKey.NO_TIMEOUT

                _mediaFilesUploadScheduled.postValue(true)
            }, { throwable ->
                CrashReporterProvider.get().recordException(throwable)
                _mediaFilesUploadScheduleError.postValue(throwable)
            })
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}