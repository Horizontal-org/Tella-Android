package rs.readahead.washington.mobile.mvp.presenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import rs.readahead.washington.mobile.data.entity.uwazi.LoginEntity
import rs.readahead.washington.mobile.data.repository.UwaziRepository
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer

class CheckUwaziServerVM constructor(val uwaziRepository: UwaziRepository) : ViewModel() {

    fun checkServer(uwaziServer: UWaziUploadServer){

      /*  val client = TUSClient(
            view.getContext().getApplicationContext(),
            server.getUrl(), server.getUsername(), server.getPassword()
        )

        OpenRosaService.clearCache()

        disposables.add(client.check()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { disposable: Disposable? -> view.showServerCheckLoading() }
            .doFinally { view.hideServerCheckLoading() }
            .subscribe({ uploadProgressInfo: UploadProgressInfo ->
                if (uploadProgressInfo.status == UploadProgressInfo.Status.OK) {
                    server.setChecked(true)
                    view.onServerCheckSuccess(server)
                } else {
                    view.onServerCheckFailure(uploadProgressInfo.status)
                }
            }) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view.onServerCheckError(throwable)
            }
        )*/
    }

}