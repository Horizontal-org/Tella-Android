package rs.readahead.washington.mobile.mvp.presenter

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.DropBoxDataSource
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.openrosa.OpenRosaService
import rs.readahead.washington.mobile.domain.entity.dropbox.DropBoxServer
import rs.readahead.washington.mobile.mvp.contract.IDropBoxServersPresenterContract

class DropBoxServersPresenter(var view: IDropBoxServersPresenterContract.IView) :
    IDropBoxServersPresenterContract.IPresenter {

    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()

    override fun getDropBoxServers() {
        disposables.add(keyDataSource.dropBoxDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showLoading() }
            .flatMapSingle { dataSource: DropBoxDataSource -> dataSource.listDropBoxServers() }
            .doFinally { view.hideLoading() }
            .subscribe(
                { list: List<DropBoxServer> ->
                    view.onDropBoxServersLoaded(
                        list
                    )
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view.onLoadDropBoxServersError(throwable)
            }
        )
    }

    override fun create(server: DropBoxServer) {
        disposables.add(
            keyDataSource.dropBoxDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.showLoading() }
                .flatMapSingle { dataSource: DropBoxDataSource ->
                    dataSource.saveDropBoxServer(server)
                }
                .doFinally { view.hideLoading() }
                .subscribe(
                    { server: DropBoxServer ->
                        view.onCreatedDropBoxServer(
                            server
                        )
                    },
                    { throwable: Throwable? ->
                        FirebaseCrashlytics.getInstance().recordException(throwable!!)
                        view.onCreateDropBoxServerError(throwable)
                    })
        )
    }

    override fun remove(server: DropBoxServer) {
        disposables.add(keyDataSource.dropBoxDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showLoading() }
            .flatMapCompletable { dataSource: DropBoxDataSource ->
                dataSource.removeDropBoxServer(server.id)
            }
            .doFinally { view.hideLoading() }
            .subscribe(
                {
                    OpenRosaService.clearCache()
                    view.onRemovedDropBoxServer(server)
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view.onRemoveDropBoxServerError(throwable)
            }
        )
    }

    override fun destroy() {
        disposables.dispose()
    }

}