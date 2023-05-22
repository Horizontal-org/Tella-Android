package rs.readahead.washington.mobile.views.activity.viewer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import io.reactivex.Completable
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.media.MediaFileHandler
import java.util.concurrent.Callable

class SharedMediaFileViewModel(application: Application) : AndroidViewModel(application) {
    private val keyDataSource = MyApplication.getKeyDataSource()

    private val disposables = CompositeDisposable()
    private var _error = MutableLiveData<Int>()
    val error: LiveData<Int> get() = _error
    private lateinit var mediaFileDeleteConfirmation: MediaFileDeleteConfirmation

    private var _onMediaFileExportStatus = MutableLiveData<MediaFileExportStatus>()
    val onMediaFileExportStatus: LiveData<MediaFileExportStatus> get() = _onMediaFileExportStatus

    private var _onMediaFileDeleted = MutableLiveData<Boolean>()
    val onMediaFileDeleted: LiveData<Boolean> get() = _onMediaFileDeleted

    private var _onMediaFileDeleteConfirmed = MutableLiveData<MediaFileDeleteConfirmation>()
    val onMediaFileDeleteConfirmed: LiveData<MediaFileDeleteConfirmation> get() = _onMediaFileDeleteConfirmed

    private var _onMediaFileRenamed = MutableLiveData<VaultFile>()
    val onMediaFileRenamed: LiveData<VaultFile> get() = _onMediaFileRenamed

    fun exportNewMediaFile(withMetadata: Boolean, vaultFile: VaultFile, path: Uri?) {
        disposables.add(Completable.fromCallable(Callable<Void?> {
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
        })
            .subscribeOn(Schedulers.computation())
            .doOnSubscribe { _onMediaFileExportStatus.postValue(MediaFileExportStatus.EXPORT_START) } //onExportStarted
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { _onMediaFileExportStatus.postValue(MediaFileExportStatus.EXPORT_END) } //view.onExportEnded()
            .subscribe(
                { _onMediaFileExportStatus.postValue(MediaFileExportStatus.EXPORT_PROGRESS) } //view.onMediaExported()
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(R.string.gallery_toast_fail_exporting_to_device)

            }
        )
    }

    fun deleteMediaFiles(vaultFile: VaultFile?) {
        disposables.add(MyApplication.rxVault.delete(vaultFile)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { isDeleted: Boolean? -> _onMediaFileDeleted.postValue(isDeleted) }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(R.string.gallery_toast_fail_deleting_files)

            })
    }

    fun renameVaultFile(id: String?, name: String?) {
        disposables.add(MyApplication.rxVault.rename(id, name)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { vaultFile: VaultFile? ->
                    _onMediaFileRenamed.postValue(vaultFile)
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(R.string.gallery_toast_fail_deleting_files)
            })
    }

    /* to be used in audioActivity */
    fun confirmDeleteMediaFile(vaultFile: VaultFile) {
        disposables.add(
            keyDataSource.dataSource
                .flatMapSingle<List<FormMediaFile>>(Function<DataSource, SingleSource<List<FormMediaFile>>> { obj: DataSource -> obj.reportMediaFiles })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ formMediaFileList: List<FormMediaFile> ->
                    if (formMediaFileList.isEmpty()) {
                        mediaFileDeleteConfirmation.vaultFile = vaultFile
                        mediaFileDeleteConfirmation.showConfirmDelete = false
                        _onMediaFileDeleteConfirmed.postValue(mediaFileDeleteConfirmation)
                    } else {
                        var isShowConfirmation = false
                        //#1. Iterate through list
                        for (formMediaFile in formMediaFileList) {
                            if (formMediaFile.id == vaultFile.id) {
                                isShowConfirmation = true
                                break
                            }
                        }
                        mediaFileDeleteConfirmation.vaultFile = vaultFile
                        mediaFileDeleteConfirmation.showConfirmDelete = isShowConfirmation
                        _onMediaFileDeleteConfirmed.postValue(mediaFileDeleteConfirmation)
                        // view.onMediaFileDeleteConfirmation(vaultFile, isShowConfirmation)
                    }
                }, { throwable: Throwable? ->
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                    _error.postValue(R.string.gallery_toast_fail_deleting_files)
                })
        )
    }

}