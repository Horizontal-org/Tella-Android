package rs.readahead.washington.mobile.views.fragment.uwazi.attachments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Sort
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.bus.SingleLiveEvent

class AttachmentsSelectorViewModel : ViewModel() {
    private val disposables by lazy { CompositeDisposable() }
    private var _error = MutableLiveData<Throwable>()
    val error: LiveData<Throwable> get() = _error
    private var _vaultFiles = MutableLiveData<List<VaultFile>>()
    val vaultFiles: LiveData<List<VaultFile>> get() = _vaultFiles

    private var _selectVaultFiles = MutableLiveData<List<VaultFile>>()
    val selectVaultFiles: LiveData<List<VaultFile>> get() = _selectVaultFiles

    private var _rootVaultFile = SingleLiveEvent<VaultFile>()
    val rootVaultFile: LiveData<VaultFile> get() = _rootVaultFile

    init {
        getRootId()
    }

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
                            { result: List<VaultFile>? ->
                                _vaultFiles.postValue(result ?: emptyList())
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

    fun getRootId() {
        MyApplication.rxVault?.root
            ?.subscribe(
                { vaultFile: VaultFile? -> _rootVaultFile.postValue(vaultFile) }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(throwable)
            }?.dispose()
    }


    fun getFiles(ids: Array<String>) {
        MyApplication.rxVault.get(ids)
            .subscribe(
                { vaultFiles: List<VaultFile>? ->
                    _selectVaultFiles.postValue(vaultFiles)
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(throwable)
            }.dispose()
    }


}