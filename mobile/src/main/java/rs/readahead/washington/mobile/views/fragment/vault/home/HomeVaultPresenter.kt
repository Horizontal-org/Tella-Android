package rs.readahead.washington.mobile.views.fragment.vault.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Limits
import com.hzontal.tella_vault.filter.Sort
import com.hzontal.tella_vault.rx.RxVault
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.BuildConfig
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.media.MediaFileHandler

class HomeVaultPresenter constructor(var view: IHomeVaultPresenter.IView?) :
    IHomeVaultPresenter.IPresenter {
    private var keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private var disposable = CompositeDisposable()
    private var appContext: Context? = null
    private var rxVault: RxVault? = null

    init {
        appContext = view?.getContext()?.applicationContext
        rxVault = MyApplication.rxVault
    }

    private val disposables = CompositeDisposable()

    override fun destroy() {
        disposable.dispose()
        view = null
    }

    override fun executePanicMode() {
        keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .flatMapCompletable { dataSource: DataSource ->
                if (SharedPrefs.getInstance().isEraseGalleryActive) {
                    rxVault?.destroy()?.blockingAwait()
                    MediaFileHandler.destroyGallery(appContext!!)
                }
                if (Preferences.isDeleteServerSettingsActive()) {
                    dataSource.deleteDatabase()
                } else {
                    if (Preferences.isEraseForms()) {
                        dataSource.deleteForms()
                    }
                }
                clearSharedPreferences()
                MyApplication.exit(view?.getContext())
                MyApplication.resetKeys()
                if (Preferences.isUninstallOnPanic()) {
                    view?.getContext()?.let { uninstallTella(it) }
                }
                Completable.complete()
            }
            .blockingAwait()
    }

    override fun countTUServers() {
    }

    override fun countCollectServers() {
        disposable.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapSingle { obj: DataSource -> obj.countCollectServers() }
            .subscribe(
                { num: Long? ->
                    view?.onCountCollectServersEnded(
                        num
                    )
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onCountCollectServersFailed(throwable)
            }
        )
    }


    override fun exportMediaFiles(vaultFiles: List<VaultFile?>) {
        disposables.add(
            Single
                .fromCallable {
                    val resultList = MediaFileHandler.walkAllFiles(vaultFiles)
                    for (vaultFile in resultList) {
                        vaultFile?.let { MediaFileHandler.exportMediaFile(view?.getContext(), it) }
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


    override fun getRecentFiles(filterType: FilterType?, sort: Sort?, limits: Limits) {
        rxVault?.list(filterType, sort, limits)
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe(
                { vaultFile: List<VaultFile?> ->
                    view?.onGetFilesSuccess(
                        vaultFile
                    )
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onGetFilesError(throwable)
            }?.let { disposables.add(it) }
    }

    override fun getFavoriteCollectForms() {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { dataSource: DataSource ->
                dataSource.listFavoriteCollectForms().toObservable()
            }
            .subscribe(
                { forms: List<CollectForm>? ->
                    if (forms != null) {
                        view?.onGetFavoriteCollectFormsSuccess(
                            forms
                        )
                    }
                }
            ) { throwable: Throwable? ->
                view?.onGetFavoriteCollectFormsError(
                    throwable
                )
            }
        )
    }

    private fun clearSharedPreferences() {
        Preferences.setPanicMessage(null)
    }

    private fun uninstallTella(context: Context) {
        val packageUri = Uri.parse("package:" + BuildConfig.APPLICATION_ID)
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri)
        context.startActivity(intent)
    }
}