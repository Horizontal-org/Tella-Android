package rs.readahead.washington.mobile.mvp.presenter

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.database.NextCloudDataSource
import rs.readahead.washington.mobile.data.openrosa.OpenRosaService
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer
import rs.readahead.washington.mobile.mvp.contract.INextCloudServersPresenterContract

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