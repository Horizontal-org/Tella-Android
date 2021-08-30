package rs.readahead.washington.mobile.views.fragment.vault.attachements

import android.net.Uri
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Sort
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.media.MediaFileHandler
import java.util.ArrayList

class AttachmentsPresenter (var view: IAttachmentsPresenter.IView?) : IAttachmentsPresenter.IPresenter  {
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
        disposables.add(Observable.fromCallable {
            MediaFileHandler.importPhotoUri(
                view?.getContext(), uri
            )
        }
            .subscribeOn(Schedulers.computation())
            .doOnSubscribe { view?.onImportStarted() }
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { view?.onImportEnded() }
            .subscribe(
                { vaultFile: VaultFile? ->
                    view?.onMediaImported(
                        vaultFile
                    )
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onImportError(throwable)
            })
    }

    override fun importVideo(uri: Uri?) {
        disposables.add(Observable.fromCallable {
            MediaFileHandler.importVideoUri(
                view?.getContext(), uri
            )
        }
            .subscribeOn(Schedulers.computation())
            .doOnSubscribe { view?.onImportStarted() }
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { view!!.onImportEnded() }
            .subscribe(
                { vaultFile: VaultFile? ->
                    view?.onMediaImported(
                        vaultFile
                    )
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onImportError(throwable)
            }
        )
    }

    override fun addNewVaultFile(vaultFile: VaultFile?) {
        view?.onMediaFilesAdded(vaultFile)
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
        val completable: MutableList<Single<Boolean>> = ArrayList()
        if (vaultFiles != null) {
            for (vaultFile in vaultFiles) {
                vaultFile?.let { deleteFile(it) }?.let { completable.add(it) }
            }
        }

        disposables.add(
            Single.zip(
                completable
            ) { objects: Array<Any?> -> objects.size }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { num: Int? -> view?.onMediaFilesDeleted(num!!) }
                ) { throwable: Throwable? ->
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                    view?.onMediaFilesDeletionError(throwable)
                }
        )
    }
    private fun deleteFile(vaultFile: VaultFile): Single<Boolean> {
        return MyApplication.rxVault.delete(vaultFile)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
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
        disposables.dispose()
        view = null
    }
}