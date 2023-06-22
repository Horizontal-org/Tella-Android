package rs.readahead.washington.mobile.views.fragment.vault.attachements

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Sort
import com.hzontal.tella_vault.rx.RxVault
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.media.MediaFileHandler
import javax.inject.Inject

@HiltViewModel
class AttachmentsViewModel @Inject constructor(
    application: Application,
    private val keyDataSource: KeyDataSource,
    private val rxVault: RxVault
    ) : AndroidViewModel(
    application
) {
    private val disposables = CompositeDisposable()
    private val _filesData = MutableLiveData<List<VaultFile?>>()
    val filesData: LiveData<List<VaultFile?>> = _filesData
    private val _error = MutableLiveData<Throwable>()
    val error: LiveData<Throwable> = _error
    private val _filesSize = MutableLiveData<Int>()
    val filesSize: LiveData<Int> = _filesSize
    private val _moveFilesError = MutableLiveData<Throwable>()
    val moveFilesError: LiveData<Throwable> = _moveFilesError
    private val _deletedFiles = MutableLiveData<Int>()
    val deletedFiles: LiveData<Int> = _deletedFiles
    private val _deletedFileError = MutableLiveData<Throwable>()
    val deletedFileError: LiveData<Throwable> = _deletedFileError
    private val _deletedFile = MutableLiveData<VaultFile>()
    val deletedFile: LiveData<VaultFile> = _deletedFile
    private val _folderCreated = MutableLiveData<VaultFile>()
    val folderCreated: LiveData<VaultFile> = _folderCreated
    private val _rootId = MutableLiveData<VaultFile>()
    val rootId: LiveData<VaultFile> = _rootId
    val counterData = MutableLiveData<Int>()

    //TODO AHLEM FIX THIS val counterData: LiveData<Int> = _counterData
    private val _progressPercent = MutableLiveData<Pair<Double, Int>>()
    val progressPercent: LiveData<Pair<Double, Int>> = _progressPercent
    private val _mediaImportedWithDelete = MutableLiveData<Uri>()
    val mediaImportedWithDelete: LiveData<Uri> = _mediaImportedWithDelete
    private val _mediaImported = MutableLiveData<VaultFile>()
    val mediaImported: LiveData<VaultFile> = _mediaImported
    private val _renameFileSuccess = MutableLiveData<VaultFile>()
    val renameFileSuccess: LiveData<VaultFile> = _renameFileSuccess
    private val _exportState = MutableLiveData<Boolean>()
    val exportState: LiveData<Boolean> = _exportState
    private val _mediaExported = MutableLiveData<Int>()
    val mediaExported: LiveData<Int> = _mediaExported
    private val _onConfirmDeleteFiles = MutableLiveData<Pair<List<VaultFile?>, Boolean>>()
    val onConfirmDeleteFiles: LiveData<Pair<List<VaultFile?>, Boolean>> = _onConfirmDeleteFiles

    fun getFiles(parent: String?, filterType: FilterType?, sort: Sort?) {
       rxVault.get(parent)
            .subscribe(
                { vaultFile: VaultFile? ->
                    disposables.add(rxVault.list(vaultFile, filterType, sort, null)
                        .subscribeOn(Schedulers.io())
                        .doOnSubscribe { }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally { }
                        .subscribe(
                            { vaultFiles: List<VaultFile?> ->
                                _filesData.postValue(vaultFiles)
                            }
                        ) { throwable: Throwable? ->
                            FirebaseCrashlytics.getInstance().recordException(throwable!!)
                            _error.postValue(throwable)
                        })
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(throwable)
            }.dispose()
    }

    fun moveFiles(parentId: String?, vaultFiles: List<VaultFile?>?) {
        if (vaultFiles == null || parentId == null) return

        val completable: MutableList<Single<Boolean>> = ArrayList()

        for (vaultFile in vaultFiles) {
            vaultFile?.let { moveFile(parentId, it) }?.let { completable.add(it) }
        }

        disposables.add(
            Single.zip(
                completable
            ) { objects: Array<Any?> -> objects.size }.observeOn(AndroidSchedulers.mainThread())
                .subscribe({ _filesSize.postValue(it) }) { throwable: Throwable? ->
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                    _moveFilesError.postValue(throwable)
                })
    }

     private fun moveFile(parentId: String, vaultFile: VaultFile): Single<Boolean> {
        return MyApplication.rxVault.move(vaultFile, parentId).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun deleteVaultFiles(vaultFiles: List<VaultFile?>?) {
        if (vaultFiles == null) return

        val completable: MutableList<Single<Boolean>> = ArrayList()

        val resultList = MediaFileHandler.walkAllFilesWithDirectories(vaultFiles)

        for (vaultFile in resultList) {
            vaultFile?.let { deleteFile(it) }?.let { completable.add(it) }
        }

        disposables.add(Single.zip(
            completable
        ) { objects: Array<Any?> -> objects.size }.observeOn(AndroidSchedulers.mainThread())
            .subscribe({ num: Int? -> _deletedFiles.postValue(num!!) }) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(throwable)
            })
    }

    fun deleteVaultFile(vaultFile: VaultFile) {
        disposables.add(MyApplication.rxVault.delete(vaultFile).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ _deletedFile.postValue(vaultFile) }) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _deletedFileError.postValue(throwable)
            })
    }

    private fun deleteFile(vaultFile: VaultFile): Single<Boolean> {
        return MyApplication.rxVault.delete(vaultFile).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun createFolder(folderName: String, parent: String) {
        MyApplication.rxVault.builder().setName(folderName).setType(VaultFile.Type.DIRECTORY)
            .build(parent)
            .subscribe({ vaultFile -> _folderCreated.postValue(vaultFile) }) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(throwable)
            }.dispose()
    }

    fun getRootId() {
        MyApplication.rxVault?.root?.subscribe({ vaultFile: VaultFile ->
            _rootId.postValue(vaultFile)
        }) { throwable: Throwable? ->
            FirebaseCrashlytics.getInstance().recordException(throwable!!)
            _error.postValue(throwable)
        }?.dispose()
    }

    fun importVaultFiles(uris: List<Uri>, parentId: String?, deleteOriginal: Boolean) {
        if (uris.isEmpty()) return
        counterData.value = 0
        var counter = 1
        var currentUri: Uri? = null
        disposables.add(Flowable.fromIterable(uris).flatMap { uri ->
            currentUri = uri
            MediaFileHandler.importVaultFileUri(getApplication(), uri, parentId).toFlowable()
        }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                if (counter == 1) {
                    _progressPercent.postValue(Pair(counter.toDouble(), uris.size))
                    counterData.postValue(counter++)
                } else counterData.postValue(counter++)
            }.subscribe({ vaultFile ->
                if (deleteOriginal) {
                    currentUri?.let { uri -> _mediaImportedWithDelete.postValue(uri) }
                } else {
                    _mediaImported.postValue(vaultFile)
                }

            }) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(throwable)
            })

    }

    fun renameVaultFile(id: String, name: String) {
        disposables.add(MyApplication.rxVault.rename(id, name).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ _renameFileSuccess.postValue(it) }) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(throwable)
            })
    }

    fun exportMediaFiles(withMetadata: Boolean, vaultFiles: List<VaultFile?>, path: Uri?) {
        disposables.add(Single.fromCallable {
            val resultList = MediaFileHandler.walkAllFiles(vaultFiles)
            for (vaultFile in resultList) {
                vaultFile?.let {
                    MediaFileHandler.exportMediaFile(getApplication(), it, path)
                    if (withMetadata && vaultFile.metadata != null) {
                        MediaFileHandler.exportMediaFile(
                            getApplication(),
                            MediaFileHandler.maybeCreateMetadataMediaFile(it),
                            path
                        )
                    }
                }
            }
            resultList.size
        }.subscribeOn(Schedulers.computation()).doOnSubscribe { _exportState.postValue(true) }
            .observeOn(AndroidSchedulers.mainThread()).doFinally { _exportState.postValue(false) }
            .subscribe({ num: Int? ->
                if (num != null) {
                    _mediaExported.postValue(num)
                }
            }) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(throwable)
            })
    }

    fun deleteFilesAfterConfirmation(
        vaultFiles: List<VaultFile?>
    ) {
        disposables.add(keyDataSource.dataSource
            .flatMapSingle { dataSource: DataSource ->
                dataSource.reportMediaFiles
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { files: List<FormMediaFile> ->
                    val doesFileExist =
                        files.any { file -> vaultFiles.any { vaultFile -> vaultFile?.id == file.id } }
                    _onConfirmDeleteFiles.postValue(Pair(vaultFiles, doesFileExist))
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(throwable)
            }
        )
    }

    fun cancelImportVaultFiles() {
        disposables.clear()
    }

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }
}