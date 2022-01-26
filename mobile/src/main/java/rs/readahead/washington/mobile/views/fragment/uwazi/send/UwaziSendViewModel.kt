package rs.readahead.washington.mobile.views.fragment.uwazi.send

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntityRow
import rs.readahead.washington.mobile.data.repository.UwaziRepository
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer


class UwaziSendViewModel : ViewModel() {
    private val repository = UwaziRepository()
    private val disposables = CompositeDisposable()
    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress
    private val _entitySubmitted = MutableLiveData<Boolean>()
    val entitySubmitted: LiveData<Boolean> get() = _entitySubmitted
    var error = MutableLiveData<Throwable>()

    fun submitEntity(server: UWaziUploadServer, uwaziEntityRow: UwaziEntityRow) {
        disposables.add(
            repository.submitEntity(uwaziEntityRow,server)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _progress.postValue(true) }
                .doFinally { _progress.postValue(false) }
                .subscribe({
                    _entitySubmitted.postValue(true)
                }
                ) { throwable: Throwable? ->
                    FirebaseCrashlytics.getInstance().recordException(
                        throwable
                            ?: throw NullPointerException("Expression 'throwable' must not be null")
                    )
                    error.postValue(throwable)
                })
    }
}