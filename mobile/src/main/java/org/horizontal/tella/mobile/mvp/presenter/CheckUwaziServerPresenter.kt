package org.horizontal.tella.mobile.mvp.presenter

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.repository.UwaziRepository
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.domain.entity.UploadProgressInfo
import org.horizontal.tella.mobile.mvp.contract.ICheckUwaziServerContract

class CheckUwaziServerPresenter constructor(private var view: ICheckUwaziServerContract.IView?) :
    ICheckUwaziServerContract.IPresenter {
    private val disposables = CompositeDisposable()
    private var saveAnyway = false

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
            .doOnSubscribe { view?.showServerCheckLoading() }
            .doFinally { view?.hideServerCheckLoading() }
            .subscribe({ result ->
                if (result.isSuccess) {
                    server.isChecked = true
                    server.connectCookie = result.cookies
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