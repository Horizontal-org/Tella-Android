package rs.readahead.washington.mobile.views.fragment.feedback

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.entity.feedback.FeedbackBodyEntity
import rs.readahead.washington.mobile.domain.repository.feedback.FeedbackRepository

import javax.inject.Inject


@HiltViewModel
class SendFeedbackViewModel  @Inject constructor(
        private val feedbackRepository: FeedbackRepository,
        private val dataSource: DataSource
) : ViewModel() {


    fun submitFeedback(instance: FeedbackBodyEntity, backButtonPressed: Boolean) {
        feedbackRepository.submitFeedback(instance).doOnSubscribe {  }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally {  }
                .map {
                    it.apply {
                        var test = it.createdAt
                    }

                }
                .onErrorResumeNext { exception: Throwable? ->
                    Single.error(
                            exception
                    )
                }

    }

}