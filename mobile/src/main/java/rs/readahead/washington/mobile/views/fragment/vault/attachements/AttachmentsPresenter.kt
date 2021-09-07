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
import java.util.*

class AttachmentsPresenter(var view: IAttachmentsPresenter.IView?) :
    IAttachmentsPresenter.IPresenter {
    private val disposables = CompositeDisposable()

    override fun getFiles(parent: String?,filterType: FilterType?, sort: Sort?) {

        MyApplication.rxVault.get(parent)
            .subscribe(
                { vaultFile: VaultFile? ->
                    disposables.add(MyApplication.rxVault.list(vaultFile,filterType, sort, null)
                        .subscribeOn(Schedulers.io())
                        .doOnSubscribe { view?.onGetFilesStart() }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally { view?.onGetFilesEnd() }
                        .subscribe(
                            { vaultFiles: List<VaultFile?> ->
                                view?.onGetFilesSuccess(
                                    vaultFiles
                                )
                            }
                        ) { throwable: Throwable? ->
                            FirebaseCrashlytics.getInstance().recordException(throwable!!)
                            view?.onGetFilesError(throwable)
                        })
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onGetRootIdError(throwable)
            }.dispose()
    }

    override fun importVaultFiles(uris: List<Uri?>) {
        disposables.add(Observable.fromCallable {
            MediaFileHandler.importVaultFilesUris(
                view?.getContext(), uris
            )
        }
            .subscribeOn(Schedulers.computation())
            .doOnSubscribe { view?.onImportStarted() }
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { view?.onImportEnded() }
            .subscribe(
                { vaultFiles ->
                    view?.onMediaImported(vaultFiles)
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onImportError(throwable)
            })

    }


    override fun addNewVaultFiles() {
        view?.onMediaFilesAdded()
    }

    override fun renameVaultFile(id: String, name: String) {
        disposables.add(MyApplication.rxVault.rename(id, name)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { view?.onRenameFileStart() }
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

    override fun createFolder(folderName: String, parent: String) {
        MyApplication.rxVault
            .builder()
            .setName(folderName)
            .setType(VaultFile.Type.DIRECTORY)
            .build(parent)
            .subscribe(
                { vaultFile: VaultFile? -> view?.onCreateFolderSuccess() }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onCountTUServersFailed(throwable)
            }.dispose()

    }

    override fun getRootId() {
        MyApplication.rxVault.root
            .subscribe(
                { vaultFile: VaultFile? -> view?.onGetRootIdSuccess(vaultFile)  }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onGetRootIdError(throwable)
            }.dispose()
    }


    override fun countTUServers() {
    }


    override fun destroy() {
        disposables.dispose()
        view = null
    }
}