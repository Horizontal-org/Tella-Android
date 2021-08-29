package rs.readahead.washington.mobile.views.fragment.vault.attachements

import android.net.Uri
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.filter.Filter
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Sort
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.media.MediaFileHandler

class AttachmentsPresenter (val view: IAttachmentsPresenter.IView?) : IAttachmentsPresenter.IPresenter  {
    private val disposables = CompositeDisposable()

    override fun getFiles(filterType: FilterType?, sort: Sort?) {
        disposables.add(MyApplication.rxVault.list(filterType, sort, null)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { view?.onGetFilesStart() }
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { view?.onGetFilesEnd() }
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

    override fun importImage(uri: Uri?) {
    }

    override fun importVideo(uri: Uri?) {
    }

    override fun addNewVaultFile(vaultFile: VaultFile?) {
    }

    override fun renameVaultFile(id: String, name: String) {
        disposables.add(MyApplication.rxVault.rename(id,name)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe{ view?.onRenameFileStart()}
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { view?.onRenameFileEnd() }
            .subscribe(
                { view?.onRenameFileSuccess() }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onRenameFileError(throwable)
            })
    }

    override fun deleteVaultFiles(vaultFiles: List<VaultFile?>?) {
    }

    override fun deleteVaultFile(vaultFile: VaultFile?) {
        disposables.add(MyApplication.rxVault.delete(vaultFile)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { view?.onMediaFileDeleted() }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onMediaFileDeletionError(throwable)
            })
    }

    override fun exportMediaFiles(vaultFiles: List<VaultFile?>) {
        disposables.add(
            Single
                .fromCallable {
                    for (mediaFile in vaultFiles) {
                        MediaFileHandler.exportMediaFile(view?.getContext(), mediaFile)
                    }
                    vaultFiles.size
                }
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe { view?.onExportStarted() }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { view?.onExportEnded() }
                .subscribe(
                    { num: Int? -> view?.onMediaExported(num!!) }
                ) { throwable: Throwable? ->
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                    view?.onExportError(throwable)
                }
        )
    }


    override fun countTUServers() {
    }

    override fun destroy() {
    }
}