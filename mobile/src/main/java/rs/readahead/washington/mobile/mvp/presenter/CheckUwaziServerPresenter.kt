package rs.readahead.washington.mobile.mvp.presenter

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.entity.uwazi.LoginEntity
import rs.readahead.washington.mobile.data.repository.UwaziRepository
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.mvp.contract.ICheckUwaziServerContract
import kotlin.coroutines.CoroutineContext

class CheckUwaziServerPresenter constructor(private  var view: ICheckUwaziServerContract.IView?):  ICheckUwaziServerContract.IPresenter {
    private val disposables = CompositeDisposable()
    private var saveAnyway = false
    private var parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.Main

    private val scope = CoroutineScope(coroutineContext)

    override fun checkServer(server: UWaziUploadServer) {
        if (!MyApplication.isConnectedToInternet(view!!.getContext())) {
            if (saveAnyway ) {
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

        scope.launch {
            uwaziRepository.login(server)
                .onStart {
                    view?.showServerCheckLoading()
                }
                .catch {
                    FirebaseCrashlytics.getInstance().recordException(it)
                    view?.onServerCheckError(it)
                    view?.onServerCheckFailure(UploadProgressInfo.Status.ERROR)
                    view?.hideServerCheckLoading()
                }
                .collect {
                    if (it.success) {
                        server.isChecked = true
                        view?.onServerCheckSuccess(server)
                    } else {
                        view?.onServerCheckFailure(UploadProgressInfo.Status.UNAUTHORIZED)
                    }
                    view?.hideServerCheckLoading()

                }
        }


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