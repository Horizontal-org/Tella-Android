package org.horizontal.tella.mobile.views.activity.viewer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.media.MediaFileHandler
import javax.inject.Inject


@HiltViewModel
class SharedMediaFileViewModel @Inject constructor(
    application: Application,
    private val keyDataSource: KeyDataSource
) : AndroidViewModel(application) {

    private val disposables = CompositeDisposable()
    private var _error = MutableLiveData<Int>()
    val error: LiveData<Int> get() = _error

    private lateinit var mediaFileDeleteConfirmation: MediaFileDeleteConfirmation

    private val _onMediaFileExportStatus = MutableLiveData<MediaFileExportStatus>()
    val onMediaFileExportStatus: LiveData<MediaFileExportStatus> get() = _onMediaFileExportStatus

    private val _onMediaFileDeleted = MutableLiveData<Boolean>()
    val onMediaFileDeleted: LiveData<Boolean> get() = _onMediaFileDeleted

    private val _onMediaFileDeleteConfirmed = MutableLiveData<MediaFileDeleteConfirmation>()
    val onMediaFileDeleteConfirmed: LiveData<MediaFileDeleteConfirmation> get() = _onMediaFileDeleteConfirmed

    private val _onMediaFileRenamed = MutableLiveData<VaultFile>()
    val onMediaFileRenamed: LiveData<VaultFile> get() = _onMediaFileRenamed

    private val _onMediaFileGot = MutableLiveData<VaultFile>()
    val onMediaFileGot: LiveData<VaultFile> get() = _onMediaFileGot

    fun exportNewMediaFile(withMetadata: Boolean, vaultFile: VaultFile, path: Uri?) {
        disposables.add(
            Completable.fromCallable {
                MediaFileHandler.exportMediaFile(
                    getApplication(),
                    vaultFile,
                    path
                )
                if (withMetadata && vaultFile.metadata != null) {
                    MediaFileHandler.exportMediaFile(
                        getApplication(),
                        MediaFileHandler.maybeCreateMetadataMediaFile(vaultFile),
                        path
                    )
                }
                null
            }
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe { _onMediaFileExportStatus.postValue(MediaFileExportStatus.EXPORT_START) }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _onMediaFileExportStatus.postValue(MediaFileExportStatus.EXPORT_END) }
                .subscribe(
                    { _onMediaFileExportStatus.postValue(MediaFileExportStatus.EXPORT_PROGRESS) },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(R.string.gallery_toast_fail_exporting_to_device)
                    }
                )
        )
    }

    fun renameVaultFile(id: String, name: String) {
        disposables.add(
            MyApplication.keyRxVault.rxVault
                .firstOrError()
                .flatMap { rxVault -> rxVault.rename(id, name) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { vaultFile ->
                        _onMediaFileRenamed.postValue(vaultFile)
                    },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(R.string.file_name_taken)
                    }
                )
        )
    }

    fun deleteMediaFiles(vaultFile: VaultFile) {
        disposables.add(
            MyApplication.keyRxVault.rxVault
                .firstOrError()
                .flatMap { rxVault -> rxVault.delete(vaultFile) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { isDeleted ->
                        _onMediaFileDeleted.postValue(isDeleted)
                    },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(R.string.gallery_toast_fail_deleting_files)
                    }
                )
        )
    }

    fun confirmDeleteMediaFile(vaultFile: VaultFile) {
        disposables.add(
            keyDataSource.dataSource
                .flatMapSingle { it.reportMediaFiles }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { formMediaFileList ->
                        val isShowConfirmation = formMediaFileList.any { it.id == vaultFile.id }
                        mediaFileDeleteConfirmation =
                            MediaFileDeleteConfirmation(vaultFile, isShowConfirmation)
                        _onMediaFileDeleteConfirmed.postValue(mediaFileDeleteConfirmation)
                    },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(R.string.gallery_toast_fail_deleting_files)
                    }
                )
        )
    }

    fun getMediaFile(id: String) {
        disposables.add(
            MyApplication.keyRxVault.rxVault
                .firstOrError()
                .flatMap { rxVault -> rxVault[id] }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { vaultFile ->
                        _onMediaFileGot.postValue(vaultFile)
                    },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(R.string.default_error_msg)
                    }
                )
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
