package rs.readahead.washington.mobile.views.fragment.vault.attachements

import android.net.Uri
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.IVaultDatabase
import com.hzontal.tella_vault.VaultFile
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication

class AttachmentsPresenter (val view: IAttachmentsPresenter.IView) : IAttachmentsPresenter.IPresenter  {
    private val disposables = CompositeDisposable()

    override fun getFiles(filter: IVaultDatabase.Filter?, sort: IVaultDatabase.Sort?) {
        disposables.add(MyApplication.rxVault.list(filter, sort, null)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { view.onGetFilesStart() }
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { view.onGetFilesEnd() }
            .subscribe(
                { vaultFile: List<VaultFile?> ->
                    view.onGetFilesSuccess(
                        vaultFile
                    )
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view.onGetFilesError(throwable)
            })
    }

    override fun importImage(uri: Uri?) {
        TODO("Not yet implemented")
    }

    override fun importVideo(uri: Uri?) {
        TODO("Not yet implemented")
    }

    override fun addNewMediaFile(vaultFile: VaultFile?) {
        TODO("Not yet implemented")
    }

    override fun deleteMediaFiles(mediaFiles: List<VaultFile?>?) {
        TODO("Not yet implemented")
    }

    override fun exportMediaFiles(mediaFiles: List<VaultFile?>?) {
        TODO("Not yet implemented")
    }

    override fun countTUServers() {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }
}