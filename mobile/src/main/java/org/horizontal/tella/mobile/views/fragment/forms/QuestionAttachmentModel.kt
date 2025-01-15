package org.horizontal.tella.mobile.views.fragment.forms

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.domain.repository.IMediaFileRecordRepository
import org.horizontal.tella.mobile.media.MediaFileHandler
import javax.inject.Inject

class QuestionAttachmentModel @Inject constructor(private val mApplication: Application) :
    AndroidViewModel(mApplication) {
    private val disposables = CompositeDisposable()

    private var attachment: VaultFile? = null

    private var _onGetFilesStart = SingleLiveEvent<Boolean>()
    val onGetFilesStart: LiveData<Boolean> get() = _onGetFilesStart

    private var _onGetFilesEnd = SingleLiveEvent<Boolean>()
    val onGetFilesEnd: LiveData<Boolean> get() = _onGetFilesEnd

    private var _onGetFilesSuccess = SingleLiveEvent<List<VaultFile>>()
    val onGetFilesSuccess: LiveData<List<VaultFile>> get() = _onGetFilesSuccess

    private var _onGetFilesError = SingleLiveEvent<Throwable?>()
    val onGetFilesError: LiveData<Throwable?> get() = _onGetFilesError

    private var _onMediaFileAdded = SingleLiveEvent<VaultFile?>()
    val onMediaFileAdded: LiveData<VaultFile?> get() = _onMediaFileAdded

    private var _onImportStarted = SingleLiveEvent<Boolean>()
    val onImportStarted: LiveData<Boolean> get() = _onImportStarted

    private var _onImportEnded = SingleLiveEvent<Boolean>()
    val onImportEnded: LiveData<Boolean> get() = _onImportEnded

    private var _onMediaFileImported = SingleLiveEvent<VaultFile>()
    val onMediaFileImported: LiveData<VaultFile> get() = _onMediaFileImported

    private var _onImportError = SingleLiveEvent<Throwable?>()
    val onImportError: LiveData<Throwable?> get() = _onImportError

    fun getFiles(
        filter: IMediaFileRecordRepository.Filter?,
        sort: IMediaFileRecordRepository.Sort?
    ) {
        disposables.add(MyApplication.rxVault.list(null, null, null)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { disposable: Disposable? -> _onGetFilesStart.postValue(true) }
            .doFinally { _onGetFilesEnd.postValue(true) }
            .subscribe(
                { mediaFiles: List<VaultFile> ->
                    _onGetFilesSuccess.postValue(
                        mediaFiles
                    )
                }
            ) { throwable: Throwable? ->
                _onGetFilesError.postValue(
                    throwable
                )
            }
        )
    }

    fun getAttachment(): VaultFile? {
        return attachment
    }

    fun setAttachment(attachment: VaultFile?) {
        this.attachment = attachment
    }

    fun addNewMediaFile(vaultFile: VaultFile?) {
        _onMediaFileAdded.postValue(attachment)
    }

    fun addRegisteredMediaFile(id: Long) {}

    fun importImage(uri: Uri?) {
        disposables.add(Observable.fromCallable<Single<VaultFile>> {
            MediaFileHandler.importPhotoUri(
                mApplication.baseContext,
                uri,
                null
            )
        }
            .subscribeOn(Schedulers.computation())
            .doOnSubscribe { disposable: Disposable? -> _onImportStarted.postValue(true) }
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { _onImportEnded.postValue(true) }
            .subscribe(
                { vaultFile: Single<VaultFile> ->
                    _onMediaFileImported.postValue(
                        vaultFile.blockingGet()
                    )
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _onImportError.postValue(throwable)
            }
        )
    }

    fun importVideo(uri: Uri?) {
        disposables.add(Observable.fromCallable<Single<VaultFile>> {
            MediaFileHandler.importVideoUri(
                mApplication.baseContext,
                uri,
                null
            )
        }
            .subscribeOn(Schedulers.computation())
            .doOnSubscribe { disposable: Disposable? -> _onImportStarted.postValue(true) }
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { _onImportEnded.postValue(true) }
            .subscribe(
                { mediaHolder: Single<VaultFile> ->
                    _onMediaFileImported.postValue(
                        mediaHolder.blockingGet()
                    )
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _onImportError.postValue(throwable)
            }
        )
    }

}