package org.horizontal.tella.mobile.views.fragment.forms

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.domain.repository.IMediaFileRecordRepository
import org.horizontal.tella.mobile.media.MediaFileHandler
import javax.inject.Inject

class QuestionAttachmentModel @Inject constructor(
    private val application: Application
) : AndroidViewModel(application) {

    private val disposables = CompositeDisposable()

    private var attachment: VaultFile? = null

    private val _onGetFilesStart = SingleLiveEvent<Boolean>()
    val onGetFilesStart: LiveData<Boolean> get() = _onGetFilesStart

    private val _onGetFilesEnd = SingleLiveEvent<Boolean>()
    val onGetFilesEnd: LiveData<Boolean> get() = _onGetFilesEnd

    private val _onGetFilesSuccess = SingleLiveEvent<List<VaultFile>>()
    val onGetFilesSuccess: LiveData<List<VaultFile>> get() = _onGetFilesSuccess

    private val _onGetFilesError = SingleLiveEvent<Throwable?>()
    val onGetFilesError: LiveData<Throwable?> get() = _onGetFilesError

    private val _onMediaFileAdded = SingleLiveEvent<VaultFile?>()
    val onMediaFileAdded: LiveData<VaultFile?> get() = _onMediaFileAdded

    private val _onImportStarted = SingleLiveEvent<Boolean>()
    val onImportStarted: LiveData<Boolean> get() = _onImportStarted

    private val _onImportEnded = SingleLiveEvent<Boolean>()
    val onImportEnded: LiveData<Boolean> get() = _onImportEnded

    private val _onMediaFileImported = SingleLiveEvent<VaultFile>()
    val onMediaFileImported: LiveData<VaultFile> get() = _onMediaFileImported

    private val _onImportError = SingleLiveEvent<Throwable?>()
    val onImportError: LiveData<Throwable?> get() = _onImportError

    fun getFiles(
        filter: IMediaFileRecordRepository.Filter?,
        sort: IMediaFileRecordRepository.Sort?
    ) {
        disposables.add(
            MyApplication.keyRxVault.rxVault
                .firstOrError()
                .flatMap { rxVault -> rxVault.list(null, null, null) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _onGetFilesStart.postValue(true) }
                .doFinally { _onGetFilesEnd.postValue(true) }
                .subscribe(
                    { mediaFiles -> _onGetFilesSuccess.postValue(mediaFiles) },
                    { throwable -> _onGetFilesError.postValue(throwable) }
                )
        )
    }

    fun getAttachment(): VaultFile? = attachment

    fun setAttachment(attachment: VaultFile?) {
        this.attachment = attachment
    }

    fun addNewMediaFile(vaultFile: VaultFile?) {
        _onMediaFileAdded.postValue(vaultFile)
    }

    fun addRegisteredMediaFile(id: Long) {
        // Empty implementation
    }

    fun importImage(uri: Uri?) {
        disposables.add(
            Observable.fromCallable {
                MediaFileHandler.importPhotoUri(application.baseContext, uri, null)
            }
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe { _onImportStarted.postValue(true) }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _onImportEnded.postValue(true) }
                .subscribe(
                    { vaultFile -> _onMediaFileImported.postValue(vaultFile.blockingGet()) },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable!!)
                        _onImportError.postValue(throwable)
                    }
                )
        )
    }

    fun importVideo(uri: Uri?) {
        disposables.add(
            Observable.fromCallable {
                MediaFileHandler.importVideoUri(application.baseContext, uri, null)
            }
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe { _onImportStarted.postValue(true) }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _onImportEnded.postValue(true) }
                .subscribe(
                    { vaultFile -> _onMediaFileImported.postValue(vaultFile.blockingGet()) },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable!!)
                        _onImportError.postValue(throwable)
                    }
                )
        )
    }

    fun importFile(uri: Uri?) {
        disposables.add(
            Observable.fromCallable {
                MediaFileHandler.importVaultFileUri(application.baseContext, uri, null)
            }
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe { _onImportStarted.postValue(true) }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _onImportEnded.postValue(true) }
                .subscribe(
                    { vaultFile -> _onMediaFileImported.postValue(vaultFile.blockingGet()) },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable!!)
                        _onImportError.postValue(throwable)
                    }
                )
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
