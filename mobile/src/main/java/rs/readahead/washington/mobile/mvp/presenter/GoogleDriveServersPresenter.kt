package rs.readahead.washington.mobile.mvp.presenter

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.GoogleDriveDataSource
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.openrosa.OpenRosaService
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.mvp.contract.IGoogleDriveServersPresenterContract

class GoogleDriveServersPresenter(var view: IGoogleDriveServersPresenterContract.IView) :
    IGoogleDriveServersPresenterContract.IPresenter {

    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()


    override fun getGoogleDriveServers() {
        disposables.add(keyDataSource.googleDriveDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showLoading() }
            .flatMapSingle { dataSource: GoogleDriveDataSource -> dataSource.listGoogleDriveServers() }
            .doFinally { view.hideLoading() }
            .subscribe(
                { list: List<GoogleDriveServer> ->
                    view.onGoogleDriveServersLoaded(
                        list
                    )
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view.onLoadGoogleDriveServersError(throwable)
            }
        )
    }

    override fun create(server: GoogleDriveServer) {
        disposables.add(
            keyDataSource.googleDriveDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.showLoading() }
                .flatMapSingle { dataSource: GoogleDriveDataSource ->
                    dataSource.saveGoogleDriveServer(server)
                }
                .doFinally { view.hideLoading() }
                .subscribe(
                    { googleDriveServer: GoogleDriveServer ->
                        view.onCreatedGoogleDriveServer(
                            googleDriveServer
                        )
                    },
                    { throwable: Throwable? ->
                        FirebaseCrashlytics.getInstance().recordException(throwable!!)
                        view.onCreateGoogleDriveServerError(throwable)
                    })
        )
    }

    override fun update(server: GoogleDriveServer) {

    }

    override fun remove(server: GoogleDriveServer) {
        disposables.add(keyDataSource.googleDriveDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showLoading() }
            .flatMapCompletable { dataSource: GoogleDriveDataSource ->
                dataSource.removeGoogleDriveServer(server.id)
            }
            .doFinally { view.hideLoading() }
            .subscribe(
                {
                    OpenRosaService.clearCache()
                    view.onRemovedGoogleDriveServer(server)
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view.onRemoveGoogleDriveServerError(throwable)
            }
        )
    }

    override fun destroy() {
        disposables.dispose()
    }


}