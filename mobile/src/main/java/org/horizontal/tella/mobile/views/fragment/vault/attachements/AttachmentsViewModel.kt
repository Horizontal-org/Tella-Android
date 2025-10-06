package org.horizontal.tella.mobile.views.fragment.vault.attachements

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.exceptions.DuplicateVaultFileException
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Sort
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.bus.event.RecentBackgroundActivitiesEvent
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.domain.entity.background_activity.BackgroundActivityModel
import org.horizontal.tella.mobile.domain.entity.background_activity.BackgroundActivityStatus
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.media.MediaFileHandler
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AttachmentsViewModel @Inject constructor(
    private val application: Application,
    private val keyDataSource: KeyDataSource,
) : AndroidViewModel(
    application
) {
    private val disposables = CompositeDisposable()
    private val _filesData = MutableLiveData<List<VaultFile?>>()
    val filesData: LiveData<List<VaultFile?>> = _filesData
    private val _error = MutableLiveData<Throwable?>()
    val error: LiveData<Throwable?> = _error
    private val _importError = SingleLiveEvent<Throwable>()
    val importError: LiveData<Throwable> get() = _importError
    private val _filesSize = MutableLiveData<Int>()
    val filesSize: LiveData<Int> = _filesSize
    private val _moveFilesError = MutableLiveData<Throwable?>()
    val moveFilesError: LiveData<Throwable?> = _moveFilesError
    private val _deletedFiles = MutableLiveData<Int>()
    val deletedFiles: LiveData<Int> = _deletedFiles
    private val _deletedFileError = MutableLiveData<Throwable?>()
    val deletedFileError: LiveData<Throwable?> = _deletedFileError
    private val _deletedFile = MutableLiveData<VaultFile>()
    val deletedFile: LiveData<VaultFile> = _deletedFile
    private val _folderCreated = MutableLiveData<VaultFile>()
    val folderCreated: LiveData<VaultFile> = _folderCreated
    private val _rootId = MutableLiveData<VaultFile>()
    val rootId: LiveData<VaultFile> = _rootId
    private val _duplicateNameError = MutableLiveData<Boolean>()
    val duplicateNameError: LiveData<Boolean> = _duplicateNameError
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
    var spanCount = 1

    fun getFiles(parent: String?, filterType: FilterType?, sort: Sort?) {
        Timber.d("getFiles() called with parent: %s", parent)
        disposables.add(
            MyApplication.keyRxVault.rxVault
                .firstOrError()
                .doOnSuccess { Timber.d("Vault initialized successfully") }
                .flatMap { rxVault ->
                    Timber.d("Getting vault file for parent: %s", parent)
                    rxVault.get(parent).flatMap { vaultFile ->
                        Timber.d("Listing files for vault file: %s", vaultFile.id)
                        rxVault.list(vaultFile, filterType, sort, null)
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { vaultFiles ->
                        Timber.d("Received %d files", vaultFiles.size)
                        _filesData.postValue(vaultFiles)
                    },
                    { throwable ->
                        Timber.e(throwable, "Failed to get files")
                        FirebaseCrashlytics.getInstance().apply {
                            recordException(throwable)
                            log("Failed to get files for parent: $parent")
                        }
                        _error.postValue(throwable)
                    }
                )
        )
    }


    fun moveFiles(parentId: String?, vaultFiles: List<VaultFile?>?) {
        if (vaultFiles == null || parentId == null) return

        val completable: MutableList<Single<Boolean>> = ArrayList()

        for (vaultFile in vaultFiles) {
            vaultFile?.let { moveFile(parentId, it) }?.let { completable.add(it) }
        }

        disposables.add(Single.zip(
            completable
        ) { objects: Array<Any?> -> objects.size }.observeOn(AndroidSchedulers.mainThread())
            .subscribe({ _filesSize.postValue(it) }) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _moveFilesError.postValue(throwable)
            })
    }

    private fun moveFile(parentId: String, vaultFile: VaultFile): Single<Boolean> {
        return MyApplication.keyRxVault.rxVault
            .firstOrError()
            .flatMap { it.move(vaultFile, parentId) }
            .subscribeOn(Schedulers.io())
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
        disposables.add(
            MyApplication.keyRxVault.rxVault
                .firstOrError()
                .flatMap { it.delete(vaultFile) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { _deletedFile.postValue(vaultFile) },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _deletedFileError.postValue(throwable)
                    }
                )
        )
    }


    private fun deleteFile(vaultFile: VaultFile): Single<Boolean> {
        return MyApplication.keyRxVault.rxVault
            .firstOrError()
            .flatMap { it.delete(vaultFile) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


    fun createFolder(folderName: String, parent: String) {
        disposables.add(
            MyApplication.keyRxVault.rxVault
                .firstOrError()
                .flatMap { rxVault ->
                    rxVault.builder()
                        .setName(folderName)
                        .setType(VaultFile.Type.DIRECTORY)
                        .build(parent)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { vaultFile -> _folderCreated.postValue(vaultFile) },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(throwable)
                    }
                )
        )
    }


    fun getRootId() {
        disposables.add(
            MyApplication.keyRxVault.rxVault
                .firstOrError()
                .flatMap { it.root }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { vaultFile -> _rootId.postValue(vaultFile) },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(throwable)
                    }
                )
        )
    }

    fun importVaultFiles(uris: List<Uri>, parentId: String?, deleteOriginal: Boolean) {
        if (uris.isEmpty()) return
        // counterData.value = 0
        //  var counter = 1
        var currentUri: Uri? = null
        disposables.add(Flowable.fromIterable(uris).flatMap { uri ->
            MediaFileHandler.importVaultFileUri(getApplication(), uri, parentId).toFlowable()
                .doOnSubscribe {
                    currentUri = uri
                    val file = MediaFileHandler.getUriInfo(application, uri)
                    val backgroundVideoFile = BackgroundActivityModel(
                        id = file.name,
                        name = file.name,
                        mimeType = file.mimeType,
                        status = BackgroundActivityStatus.IN_PROGRESS,
                        thumb = null
                    )

                    // Emitting the initial status to the event bus
                    MyApplication.bus().post(
                        RecentBackgroundActivitiesEvent(
                            mutableListOf(backgroundVideoFile)
                        )
                    )
                }
        }.observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread()).subscribe({ vaultFile ->
                handleAddSuccess(vaultFile, BackgroundActivityStatus.COMPLETED)

                if (deleteOriginal) {
                    currentUri?.let { uri -> _mediaImportedWithDelete.postValue(uri) }
                } else {
                    _mediaImported.postValue(vaultFile)
                }

            }) { throwable: Throwable? ->
                if (throwable is DuplicateVaultFileException) {
                    _duplicateNameError.postValue(true)
                }
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _importError.postValue(throwable!!)
            })
    }

    private fun handleAddSuccess(vaultFile: VaultFile, status: BackgroundActivityStatus) {

        val completedActivity = BackgroundActivityModel(
            id = vaultFile.id,
            name = vaultFile.name,
            mimeType = vaultFile.mimeType,
            status = status,
            thumb = vaultFile.thumb
        )
        Timber.d("send BackgroundActivityModel inside attachments viewModel handleAddSuccess")
        MyApplication.bus().post(RecentBackgroundActivitiesEvent(mutableListOf(completedActivity)))
    }

    fun renameVaultFile(id: String, name: String) {
        disposables.add(
            MyApplication.keyRxVault.rxVault
                .firstOrError()
                .flatMap { it.rename(id, name) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { _renameFileSuccess.postValue(it) },
                    { throwable ->
                        if (throwable is SQLiteConstraintException) {
                            _duplicateNameError.postValue(true)
                        }
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(throwable)
                    }
                )
        )
    }

    fun exportMediaFiles(withMetadata: Boolean, vaultFiles: List<VaultFile?>, path: Uri?) {
        disposables.add(
            Single.fromCallable {
                val resultList = MediaFileHandler.walkAllFiles(vaultFiles)
                for (vf in resultList) {
                    vf?.let {
                        MediaFileHandler.exportMediaFile(getApplication(), it, path)
                        if (withMetadata && it.metadata != null) {
                            MediaFileHandler.exportMediaFile(
                                getApplication(),
                                MediaFileHandler.maybeCreateMetadataMediaFile(it),
                                path
                            )
                        }
                    }
                }
                resultList.size
            }
                .subscribeOn(Schedulers.io())                 // <-- was computation()
                .doOnSubscribe { _exportState.postValue(true) }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _exportState.postValue(false) }
                .subscribe({ count -> count?.let { _mediaExported.postValue(it) } }, { t ->
                    FirebaseCrashlytics.getInstance().recordException(t!!)
                    _error.postValue(t)
                })
        )

    }

    fun deleteFilesAfterConfirmation(
        vaultFiles: List<VaultFile?>
    ) {
        disposables.add(keyDataSource.dataSource.flatMapSingle { dataSource: DataSource ->
            dataSource.reportMediaFiles
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({ files: List<FormMediaFile> ->
                val doesFileExist =
                    files.any { file -> vaultFiles.any { vaultFile -> vaultFile?.id == file.id } }
                _onConfirmDeleteFiles.postValue(Pair(vaultFiles, doesFileExist))
            }) { throwable: Throwable? ->
                if (throwable != null) {
                    FirebaseCrashlytics.getInstance().recordException(throwable)
                }
                _error.postValue(throwable)
            })
    }

    fun cancelImportVaultFiles() {
        disposables.clear()
    }

    override fun onCleared() {
        // disposables.dispose()
        super.onCleared()
    }
}