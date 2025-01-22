package org.horizontal.tella.mobile.mvvm.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.disposables.CompositeDisposable

open class BaseSettingsViewModel: ViewModel() {
    protected val disposables = CompositeDisposable()

    val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<Int>()
    val error: LiveData<Int> get() = _error


    protected fun setLoading(isLoading: Boolean) {
        _loading.postValue(isLoading)
    }

    protected fun handleError(throwable: Throwable, errorResId: Int? = null) {
        FirebaseCrashlytics.getInstance().recordException(throwable)
        errorResId?.let { _error.postValue(it) }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}