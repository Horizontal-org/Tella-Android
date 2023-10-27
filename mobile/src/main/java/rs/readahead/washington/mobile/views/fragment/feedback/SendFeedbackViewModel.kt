package rs.readahead.washington.mobile.views.fragment.feedback

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
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

    @SuppressLint("CheckResult")
    fun submitFeedback(instance: FeedbackBodyEntity, backButtonPressed: Boolean) {

        disposables.add(
            feedbackRepository.submitFeedback(instance)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { }
                .subscribe({
                    it.apply {
                        var test = it.createdAt
                    }

                }) { throwable: Throwable? ->
                    Timber.d(throwable)
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                })

    }

}