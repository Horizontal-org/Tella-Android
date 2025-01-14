package org.horizontal.tella.mobile.mvp.presenter

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.database.GoogleDriveDataSource
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.openrosa.OpenRosaService
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer
import org.horizontal.tella.mobile.mvp.contract.IGoogleDriveServersPresenterContract

class GoogleDriveServersPresenter(var view: IGoogleDriveServersPresenterContract.IView) :
    IGoogleDriveServersPresenterContract.IPresenter {

    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()

    override fun getGoogleDriveServers(googleDriveId: String) {
        disposables.add(keyDataSource.googleDriveDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showLoading() }
            .flatMapSingle { dataSource: GoogleDriveDataSource -> dataSource.listGoogleDriveServers(googleDriveId) }
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