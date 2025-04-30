package org.horizontal.tella.mobile.views.fragment.uwazi.attachments

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
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import javax.inject.Inject

class AttachmentsSelectorViewModel @Inject constructor(
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val _error = MutableLiveData<Throwable>()
    val error: LiveData<Throwable> get() = _error

    private val _vaultFiles = MutableLiveData<List<VaultFile>>()
    val vaultFiles: LiveData<List<VaultFile>> get() = _vaultFiles

    private val _selectVaultFiles = MutableLiveData<List<VaultFile>>()
    val selectVaultFiles: LiveData<List<VaultFile>> get() = _selectVaultFiles

    private val _rootVaultFile = SingleLiveEvent<VaultFile>()
    val rootVaultFile: LiveData<VaultFile> get() = _rootVaultFile

    init {
        getRootId()
    }

    fun getFiles(parent: String?, filterType: FilterType?, sort: Sort?) {
        disposables.add(
            MyApplication.keyRxVault.rxVault
                .firstOrError()
                .flatMap { rxVault ->
                    rxVault.get(parent)
                }
                .flatMap { vaultFile ->
                    MyApplication.keyRxVault.getRxVault()
                        .firstOrError()
                        .flatMap { rxVault -> rxVault.list(vaultFile, filterType, sort, null) }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { result: List<VaultFile> ->
                        _vaultFiles.postValue(result)
                    },
                    { throwable: Throwable ->
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
                .flatMap { rxVault -> rxVault.root }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { vaultFile: VaultFile ->
                        _rootVaultFile.postValue(vaultFile)
                    },
                    { throwable: Throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(throwable)
                    }
                )
        )
    }

    fun getFiles(ids: Array<String>) {
        disposables.add(
            MyApplication.keyRxVault.rxVault
                .firstOrError()
                .flatMap { rxVault -> rxVault.get(ids) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { vaultFiles: List<VaultFile> ->
                        _selectVaultFiles.postValue(vaultFiles)
                    },
                    { throwable: Throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(throwable)
                    }
                )
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
