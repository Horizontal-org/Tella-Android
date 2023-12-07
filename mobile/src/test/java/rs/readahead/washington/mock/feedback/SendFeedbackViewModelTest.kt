package rs.readahead.washington.mock.feedback

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.rx.RxVault
import io.reactivex.Single
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackInstance
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackPostResult
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

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var rxVault: RxVault

    @Mock
    private lateinit var firebaseCrashlytics: FirebaseCrashlytics

    private lateinit var viewModel: SendFeedbackViewModel

    @Mock
    private lateinit var submitedFeedbackObserver : Observer<Boolean>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        viewModel = SendFeedbackViewModel(feedbackRepository, dataSource)

        // Observer simulate pour les LiveData
        viewModel.feedbackSubmitted.observeForever(submitedFeedbackObserver)
    }

    @After
    fun tearDown() {
        viewModel.feedbackSubmitted.removeObserver(submitedFeedbackObserver)
    }

    @Test
    fun testSubmitFeedback() {
       // val feedbackInstance = FeedbackInstance()
        val feedbackInstance = mock(FeedbackInstance::class.java)

        viewModel.feedbackSubmitted.observeForever(submitedFeedbackObserver)

        // Call the method under test
        viewModel.submitFeedback(feedbackInstance)

        // Update the data and verify that the observer was invoked with the updated data
        verify(submitedFeedbackObserver).onChanged(true)

        // Clean up
        viewModel.feedbackSubmitted.removeObserver(submitedFeedbackObserver)
    }


}
