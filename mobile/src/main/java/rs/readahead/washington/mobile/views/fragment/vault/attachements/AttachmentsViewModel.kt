package rs.readahead.washington.mobile.views.fragment.vault.attachements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Sort
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.media.MediaFileHandler

class AttachmentsViewModel : ViewModel() {
    private val disposables = CompositeDisposable()
    private var keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
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

    fun getFiles(parent: String?, filterType: FilterType?, sort: Sort?) {
        MyApplication.rxVault.get(parent)
            .subscribe(
                { vaultFile: VaultFile? ->
                    disposables.add(MyApplication.rxVault.list(vaultFile, filterType, sort, null)
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

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }
}