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
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackInstance
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackStatus
import rs.readahead.washington.mobile.domain.repository.feedback.FeedBackRepository
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class SendFeedbackViewModel @Inject constructor(
        private val feedbackRepository: FeedBackRepository,
        private val dataSource: DataSource
) : ViewModel() {
    private val disposables = CompositeDisposable()

    // LiveData to communicate with the view
    private val _feedbackSubmitted = SingleLiveEvent<Boolean>()
    val feedbackSubmitted: LiveData<Boolean>
        get() = _feedbackSubmitted

    // LiveData to communicate with the view
    private val _feedbackSaved = SingleLiveEvent<Boolean>()
    val feedbackSaved: LiveData<Boolean>
        get() = _feedbackSaved

    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress

    private val _draftFeedBackInstance = MutableLiveData<FeedbackInstance>()
    val draftFeedBackInstance: LiveData<FeedbackInstance> get() = _draftFeedBackInstance


    @SuppressLint("CheckResult")
    fun submitFeedback(instance: FeedbackInstance) {
        disposables.add(
                feedbackRepository.submitFeedback(instance)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { _progress.postValue(true) }
                        .doFinally { _progress.postValue(false) }
                        .subscribe({
                            it.apply {
                                _feedbackSubmitted.postValue(true)
                            }

                        }) { throwable: Throwable? ->
                            Timber.d(throwable)
                            FirebaseCrashlytics.getInstance().recordException(throwable!!)
                        })

    }

    fun saveFeedbackDraft(feedbackInstance: FeedbackInstance) {
        disposables.add(dataSource.saveInstance(feedbackInstance).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _progress.postValue(true) }
                .doFinally { _progress.postValue(false) }
                .subscribe({
                    it.apply {
                        _feedbackSaved.postValue(true)
                    }

                }) { throwable: Throwable? ->
                    Timber.d(throwable)
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                })

    }

    fun getFeedBackDraft() {
        disposables.add(dataSource.getFeedbackDraft().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _progress.postValue(true) }
                .doFinally { _progress.postValue(false) }
                .subscribe({ draft ->
                    _draftFeedBackInstance.postValue(draft)

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