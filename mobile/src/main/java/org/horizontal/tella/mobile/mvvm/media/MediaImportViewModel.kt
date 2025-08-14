package org.horizontal.tella.mobile.mvvm.media

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.exceptions.DuplicateVaultFileException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.media.MediaFileHandler
import javax.inject.Inject

@HiltViewModel
class MediaImportViewModel @Inject constructor(@ApplicationContext private val context : Context) : ViewModel() {
    private val disposables = CompositeDisposable()

    private val _mediaFileLiveData = SingleLiveEvent<VaultFile>()
    val mediaFileLiveData: LiveData<VaultFile> get() = _mediaFileLiveData

    private val _importInProgress = SingleLiveEvent<Boolean>()
    val importInProgress: LiveData<Boolean> get() = _importInProgress

    private val _importError = SingleLiveEvent<Throwable>()
    val importError: LiveData<Throwable> get() = _importError

    private val _duplicateNameError = MutableLiveData<Boolean>()
    val duplicateNameError: LiveData<Boolean> = _duplicateNameError

    var attachment: VaultFile? = null

    fun importImage(uri: Uri) {
        importMedia {
            MediaFileHandler.importPhotoUri(context, uri, null).blockingGet()
        }
    }

    fun importVideo(uri: Uri) {
        importMedia {
            MediaFileHandler.importVideoUri(context, uri, null).blockingGet()
        }
    }

    fun importFile(uri: Uri) {
        importMedia {
            MediaFileHandler.importVaultFileUri(context, uri, null).blockingGet()
        }
    }

    private fun importMedia(importTask: () -> VaultFile) {
        disposables.add(
            Observable.fromCallable(importTask)
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe { _importInProgress.postValue(true) }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _importInProgress.postValue(false) }
                .subscribe({ vaultFile ->
                    _mediaFileLiveData.postValue(vaultFile)
                }, { throwable ->
                    if (throwable is DuplicateVaultFileException) {
                        _duplicateNameError.postValue(true)
                    }
                    FirebaseCrashlytics.getInstance().recordException(throwable)
                    _importError.postValue(throwable)
                })
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }
}
