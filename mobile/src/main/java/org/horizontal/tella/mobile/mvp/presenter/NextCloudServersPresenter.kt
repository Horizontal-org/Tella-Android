package org.horizontal.tella.mobile.mvp.presenter

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.database.NextCloudDataSource
import org.horizontal.tella.mobile.data.openrosa.OpenRosaService
import org.horizontal.tella.mobile.domain.entity.nextcloud.NextCloudServer
import org.horizontal.tella.mobile.mvp.contract.INextCloudServersPresenterContract

class NextCloudServersPresenter(var view: INextCloudServersPresenterContract.IView) :
    INextCloudServersPresenterContract.IPresenter {

    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()

    override fun getNextCloudServers() {
        disposables.add(keyDataSource.nextCloudDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showLoading() }
            .flatMapSingle { dataSource: NextCloudDataSource ->
                dataSource.listNextCloudServers()
            }
            .doFinally { view.hideLoading() }
            .subscribe(
                { list: List<NextCloudServer> ->
                    view.onNextCloudServersLoaded(
                        list
                    )
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view.onLoadNextCloudServersError(throwable)
            }
        )
    }

    override fun create(server: NextCloudServer) {
        disposables.add(
            keyDataSource.nextCloudDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.showLoading() }
                .flatMapSingle { dataSource: NextCloudDataSource ->
                    dataSource.saveNextCloudServer(server)
                }
                .doFinally { view.hideLoading() }
                .subscribe(
                    { server: NextCloudServer ->
                        view.onCreatedNextCloudServer(
                            server
                        )
                    },
                    { throwable: Throwable? ->
                        FirebaseCrashlytics.getInstance().recordException(throwable!!)
                        view.onCreateNextCloudServerError(throwable)
                    })
        )
    }

    override fun remove(server: NextCloudServer) {
        disposables.add(keyDataSource.nextCloudDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showLoading() }
            .flatMapCompletable { dataSource: NextCloudDataSource ->
                dataSource.removeNextCloudServer(server.id)
            }
            .doFinally { view.hideLoading() }
            .subscribe(
                {
                    OpenRosaService.clearCache()
                    view.onRemovedNextCloudServer(server)
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view.onRemoveNextCloudServerError(throwable)
            }
        )
    }

    override fun destroy() {
        disposables.dispose()
    }

}