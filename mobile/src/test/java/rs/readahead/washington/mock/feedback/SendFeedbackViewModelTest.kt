package rs.readahead.washington.mock.feedback

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackInstance
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackPostResult
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackStatus
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException
import rs.readahead.washington.mobile.domain.repository.feedback.FeedBackRepository
import rs.readahead.washington.mobile.views.fragment.feedback.SendFeedbackViewModel

@RunWith(MockitoJUnitRunner::class)
class SendFeedbackViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var feedbackRepository: FeedBackRepository

    @Mock
    private lateinit var dataSource: DataSource

    private lateinit var viewModel: SendFeedbackViewModel

    private lateinit var testScheduler: TestScheduler

    @Mock
    private lateinit var submittedFeedBackObserver: Observer<Boolean>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        testScheduler = TestScheduler()

        // Inject mock dependencies
        viewModel = SendFeedbackViewModel(feedbackRepository)

        // Override schedulers for RxJava
        RxJavaPlugins.setIoSchedulerHandler { testScheduler }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        viewModel.feedbackSubmitted.observeForever(submittedFeedBackObserver)

    }

    @After
    fun tearDown() {
        // Reset RxJava schedulers
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
        viewModel.feedbackSubmitted.removeObserver(submittedFeedBackObserver)

    }

    @Test
    fun testSubmitFeedback_Success() {

        val feedbackInstance =
            FeedbackInstance(1, FeedbackStatus.SUBMISSION_IN_PROGRESS, "test from Horizontal")
        // Given a successful submission
        `when`(feedbackRepository.submitFeedbackInstance(feedbackInstance))
            .thenReturn(
                Single.just(
                    FeedbackPostResult(
                        1,
                        "Android",
                        "",
                        "",
                        "",
                        "test from Horizontal"
                    )
                )
            )

        viewModel.submitFeedback(feedbackInstance)

        // Fast forward the scheduler to trigger the subscription
        testScheduler.triggerActions()

        // Then verify that the submittedFeedBackObserver LiveData is TRUE :D
        verify(submittedFeedBackObserver).onChanged(true)
    }


    @Test
    fun testSubmitFeedback_Error() {
        // Given an error during submission
        val feedbackInstance =
            FeedbackInstance(1, FeedbackStatus.SUBMISSION_IN_PROGRESS, "test from Horizontal")

        `when`(feedbackRepository.submitFeedbackInstance(feedbackInstance))
            .thenReturn(Single.error(NoConnectivityException()))

        viewModel.submitFeedback(feedbackInstance)

        // Fast forward the scheduler to trigger the subscription
        testScheduler.triggerActions()

        // Then verify that the submittedFeedBackObserver LiveData is FALSE :D
        verify(submittedFeedBackObserver).onChanged(false)

    }


}
