package rs.readahead.washington.mobile.views.fragment.vault.home

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Limits
import com.hzontal.tella_vault.filter.Sort
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication

class HomeVaultPresenter constructor(val view: IHomeVaultPresenter.IView?) :
    IHomeVaultPresenter.IPresenter {

    private val disposables = CompositeDisposable()

    override fun destroy() {

    }

    override fun executePanicMode() {
    }

    override fun countTUServers() {
    }

    override fun countCollectServers() {
    }

    override fun getRecentFiles(filterType: FilterType?, sort: Sort?, limits: Limits) {
        disposables.add(MyApplication.rxVault.list(filterType, sort, limits)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { vaultFile: List<VaultFile?> ->
                    view?.onGetFilesSuccess(
                        vaultFile
                    )
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onGetFilesError(throwable)
            })
    }


}