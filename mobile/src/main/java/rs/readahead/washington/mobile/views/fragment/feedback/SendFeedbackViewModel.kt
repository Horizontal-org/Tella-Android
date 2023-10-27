package rs.readahead.washington.mobile.views.fragment.feedback

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.entity.feedback.FeedbackBodyEntity
import rs.readahead.washington.mobile.domain.repository.feedback.FeedbackRepository
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class SendFeedbackViewModel @Inject constructor(
        private val feedbackRepository: FeedbackRepository,
        private val dataSource: DataSource
) : ViewModel() {

    private val disposables = CompositeDisposable()

    // LiveData to communicate with the view
    private val _feedbackSubmitted = SingleLiveEvent<Boolean>()
    val feedbackSubmitted: LiveData<Boolean>
        get() = _feedbackSubmitted

    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress

    @SuppressLint("CheckResult")
    fun submitFeedback(instance: FeedbackBodyEntity, backButtonPressed: Boolean) {

        disposables.add(
                feedbackRepository.submitFeedback(instance)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { _progress.postValue(true) }
                        .doFinally { _progress.postValue(false) }
                        .subscribe({
                            it.apply {
                                var test = it.createdAt
                                _feedbackSubmitted.postValue(true)
                            }

                        }) { throwable: Throwable? ->
                            Timber.d(throwable)
                            FirebaseCrashlytics.getInstance().recordException(throwable!!)
                        })

    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }


}