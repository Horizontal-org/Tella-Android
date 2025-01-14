package org.horizontal.tella.mobile.mvp.presenter

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.database.UwaziDataSource
import org.horizontal.tella.mobile.data.openrosa.OpenRosaService
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.mvp.contract.IUWAZIServersPresenterContract

class UwaziServersPresenter(var view: IUWAZIServersPresenterContract.IView?) :
    IUWAZIServersPresenterContract.IPresenter {
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()


    override fun getUwaziServers() {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view!!.showLoading() }
            .flatMapSingle { obj: UwaziDataSource -> obj.listUwaziServers() }
            .doFinally { view!!.hideLoading() }
            .subscribe(
                { list: List<UWaziUploadServer> ->
                    view?.onUwaziServersLoaded(
                        list
                    )
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onLoadUwaziServersError(throwable)
            }
        )
    }

    override fun create(server: UWaziUploadServer) {
        disposables.add(
            keyDataSource.uwaziDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view?.showLoading() }
                .flatMapSingle { dataSource: UwaziDataSource ->
                    dataSource.createUWAZIServer(
                        server
                    )
                }
                .doFinally { view?.hideLoading() }
                .subscribe(
                    { server1: UWaziUploadServer? ->
                        view?.onCreatedUwaziServer(
                            server1
                        )
                    },
                    { throwable: Throwable? ->
                        FirebaseCrashlytics.getInstance().recordException(throwable!!)
                        view?.onCreateUwaziServerError(throwable)
                    })
        )
    }

    override fun update(server: UWaziUploadServer) {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view?.showLoading() }
            .flatMapSingle { dataSource: UwaziDataSource ->
                dataSource.updateUwaziServer(server)
            }
            .doFinally { view?.hideLoading() }
            .subscribe(
                { server1 -> view?.onUpdatedUwaziServer(server1) }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onUpdateUwaziServerError(throwable)
            }
        )
    }

    override fun remove(server: UWaziUploadServer) {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view?.showLoading() }
            .flatMapCompletable { dataSource: UwaziDataSource ->
                dataSource.removeUwaziServer(server.id)
            }
            .doFinally { view?.hideLoading() }
            .subscribe(
                {
                    OpenRosaService.clearCache()
                    view?.onRemovedUwaziServer(server)
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onRemoveUwaziServerError(throwable)
            }
        )
    }

    override fun destroy() {
        disposables.dispose()
        view = null
    }


}