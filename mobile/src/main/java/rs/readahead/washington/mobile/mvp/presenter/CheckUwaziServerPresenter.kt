package rs.readahead.washington.mobile.mvp.presenter

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.repository.UwaziRepository
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.mvp.contract.ICheckUwaziServerContract

class CheckUwaziServerPresenter constructor(private var view: ICheckUwaziServerContract.IView?) :
    ICheckUwaziServerContract.IPresenter {
    private val disposables = CompositeDisposable()
    private var saveAnyway = false
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()


    override fun checkServer(server: UWaziUploadServer) {
        if (!MyApplication.isConnectedToInternet(view!!.getContext())) {
            if (saveAnyway) {
                server.isChecked = false
                view?.onServerCheckSuccess(server)
            } else {
                view?.onNoConnectionAvailable()
                setSaveAnyway(true)
            }
            return
        } else {
            if (saveAnyway) {
                setSaveAnyway(false)
            }
        }
        val uwaziRepository = UwaziRepository()

        disposables.add(uwaziRepository.login(server)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {view?.showServerCheckLoading()}
            .doFinally { view?.hideServerCheckLoading() }
            .subscribe({ result ->
                if (result.success) {
                    server.isChecked = true
                    view?.onServerCheckSuccess(server)
                } else {
                    view?.onServerCheckFailure(UploadProgressInfo.Status.UNAUTHORIZED)
                }
                view?.hideServerCheckLoading()
            }) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )
                view?.onServerCheckError(throwable)
                view?.onServerCheckFailure(UploadProgressInfo.Status.ERROR)
                view?.hideServerCheckLoading()
            })
    }

    override fun destroy() {
        disposables.dispose()
        view = null
    }

    private fun setSaveAnyway(enable: Boolean) {
        saveAnyway = enable
        view?.setSaveAnyway(enable)
    }
}