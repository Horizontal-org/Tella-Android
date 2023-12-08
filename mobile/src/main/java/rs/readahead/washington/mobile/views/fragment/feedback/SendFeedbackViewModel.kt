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
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackInstance
import rs.readahead.washington.mobile.domain.repository.feedback.FeedBackRepository
import rs.readahead.washington.mobile.util.exception.ExceptionHandler
import rs.readahead.washington.mobile.util.exception.ExceptionType
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class SendFeedbackViewModel @Inject constructor(private val feedbackRepository: FeedBackRepository) :
    ViewModel() {
    private val disposables = CompositeDisposable()

    private val _feedbackSubmittedInBackground = SingleLiveEvent<Boolean>()
    val feedbackSubmittedInBackground: LiveData<Boolean>
        get() = _feedbackSubmittedInBackground

    private val _feedbackSubmitted = SingleLiveEvent<Boolean>()
    val feedbackSubmitted: LiveData<Boolean>
        get() = _feedbackSubmitted

    private val _feedbackSavedAsDraft = SingleLiveEvent<Boolean>()
    val feedbackSavedAsDraft: LiveData<Boolean>
        get() = _feedbackSavedAsDraft

    private val _feedbackSavedToBeSubmitted = SingleLiveEvent<Boolean>()
    val feedbackSavedToBeSubmitted: LiveData<Boolean>
        get() = _feedbackSavedToBeSubmitted

    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress

    private val _errorMessage = SingleLiveEvent<Int>()
    val errorMessage: LiveData<Int>
        get() = _errorMessage

    private val _draftFeedBackInstance = MutableLiveData<FeedbackInstance>()
    val draftFeedBackInstance: LiveData<FeedbackInstance> get() = _draftFeedBackInstance

    @SuppressLint("CheckResult")
    fun submitFeedback(instance: FeedbackInstance) {
        disposables.add(
            feedbackRepository.submitFeedbackInstance(instance).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _progress.postValue(true) }
                .doFinally { _progress.postValue(false) }.subscribe({
                    it.apply {
                        _feedbackSubmitted.postValue(true)
                    }
                }) { throwable: Throwable? ->
                    Timber.d(throwable)
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                    val exceptionType = ExceptionHandler.handleException(throwable)
                    handleException(exceptionType)
                })
    }

    fun saveFeedbackToBeSubmitted(feedbackInstance: FeedbackInstance) {
        disposables.add(
            feedbackRepository.saveFeedbackInstance(feedbackInstance).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _progress.postValue(true) }
                .doFinally { _progress.postValue(false) }.subscribe({
                    it.apply {
                        _feedbackSavedToBeSubmitted.postValue(true)
                    }
                }) { throwable: Throwable? ->
                    Timber.d(throwable)
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                })
    }

    fun saveFeedbackDraft(feedbackInstance: FeedbackInstance) {
        disposables.add(
            feedbackRepository.saveFeedbackInstance(feedbackInstance).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _progress.postValue(true) }
                .doFinally { _progress.postValue(false) }.subscribe({
                    it.apply {
                        _feedbackSavedAsDraft.postValue(true)
                    }
                }) { throwable: Throwable? ->
                    Timber.d(throwable)
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                })
    }

    fun getFeedBackDraft() {
        disposables.add(
            feedbackRepository.getFeedbackDraft().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _progress.postValue(true) }
                .doFinally { _progress.postValue(false) }.subscribe({ draft ->
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

    private fun handleException(exceptionType: ExceptionType) {
        _errorMessage.postValue(exceptionType.id)
    }
}