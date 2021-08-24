package rs.readahead.washington.mobile.views.fragment.vault.attachements

import android.net.Uri
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.Filter
import com.hzontal.tella_vault.IVaultDatabase
import com.hzontal.tella_vault.VaultFile
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.media.MediaFileHandler

class AttachmentsPresenter (val view: IAttachmentsPresenter.IView) : IAttachmentsPresenter.IPresenter  {
    private val disposables = CompositeDisposable()

    override fun getFiles(filter: Filter?, sort: IVaultDatabase.Sort?) {
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
    }

    override fun importVideo(uri: Uri?) {
    }

    override fun addNewVaultFile(vaultFile: VaultFile?) {
    }

    override fun deleteVaultFiles(vaultFiles: List<VaultFile?>?) {
    }

    override fun deleteVaultFile(vaultFile: VaultFile?) {
        disposables.add(MyApplication.rxVault.delete(vaultFile)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { view.onMediaFileDeleted() }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view.onMediaFileDeletionError(throwable)
            })
    }

    override fun exportMediaFiles(vaultFiles: List<VaultFile?>) {
        disposables.add(
            Single
                .fromCallable {
                    for (mediaFile in vaultFiles) {
                        MediaFileHandler.exportMediaFile(view.getContext(), mediaFile)
                    }
                    vaultFiles.size
                }
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe { view.onExportStarted() }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { view.onExportEnded() }
                .subscribe(
                    { num: Int? -> view.onMediaExported(num!!) }
                ) { throwable: Throwable? ->
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                    view.onExportError(throwable)
                }
        )
    }


    override fun countTUServers() {
    }

    override fun destroy() {
    }
}