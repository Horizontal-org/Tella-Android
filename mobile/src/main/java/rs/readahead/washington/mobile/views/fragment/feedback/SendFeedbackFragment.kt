package rs.readahead.washington.mobile.views.fragment.feedback

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import com.hzontal.tella_locking_ui.common.extensions.onChange
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.KeyboardUtil
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.databinding.FragmentSendFeedbackBinding
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackInstance
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackStatus
import rs.readahead.washington.mobile.views.activity.SettingsActivity
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.vault.attachements.OnNavBckListener

@AndroidEntryPoint
class SendFeedbackFragment : BaseBindingFragment<FragmentSendFeedbackBinding>(FragmentSendFeedbackBinding::inflate), OnNavBckListener {
    private var feedbackInstance: FeedbackInstance? = null
    private var isDescriptionEnabled = false

    companion object {
        fun newInstance() = SendFeedbackFragment()
    }

    private val viewModel by viewModels<SendFeedbackViewModel>()
    private var isSubmitEnabled = isDescriptionEnabled && Preferences.isFeedbackSharingEnabled()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Preferences.isFeedbackSharingEnabled()) viewModel.getFeedBackDraft()
        initViews()
        initObservers()
    }

    private fun initViews() {
        setupFeedbackSwitchView()
        KeyboardUtil(activity, view)
        val feedbackSwitch = binding.feedbackSwitch
        feedbackSwitch.mSwitch.isChecked = Preferences.isFeedbackSharingEnabled()
        feedbackSwitch.mSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            Preferences.setFeedbackSharingEnabled(isChecked)
            setupFeedbackSwitchView()
        }
        binding.newFeedbackEditDescription.onChange { description ->
            isDescriptionEnabled = description.isNotEmpty()
            isSubmitEnabled = isDescriptionEnabled && feedbackSwitch.mSwitch.isChecked
            highLightButton()
        }

        binding.sendFeedbackBtn.setOnClickListener {

            if (feedbackInstance == null)
                feedbackInstance = FeedbackInstance(text = binding.newFeedbackEditDescription.text.toString())

             else
                 feedbackInstance!!.text =  binding.newFeedbackEditDescription.text.toString()

            if (MyApplication.isConnectedToInternet(baseActivity)) {
                feedbackInstance!!.status = FeedbackStatus.SUBMISSION_IN_PROGRESS
                viewModel.saveFeedbackToBeSubmitted(feedbackInstance!!)
                binding.sendFeedbackBtn.setOnClickListener {  }
            } else {
                feedbackInstance!!.status = FeedbackStatus.SUBMISSION_PENDING
                viewModel.saveFeedbackToBeSubmitted(feedbackInstance!!)
                binding.sendFeedbackBtn.setOnClickListener {  }
                handleNoInternetBehavior()
            }

        }
        (activity as SettingsActivity).setToolbarHomeIcon(R.drawable.ic_close_white)
        (activity as SettingsActivity).toolbar.backClickListener = {
            handleBackButton()

        }
    }

    private fun handleNoInternetBehavior() {
        scheduleWorker()
        DialogUtils.showBottomMessage(activity, "You are not connected to the internet. Your feedback will be sent automatically when you are connected.\n", true)
    }

    fun handleBackButton() {
        if (isSubmitEnabled)
            BottomSheetUtils.showConfirmSheet(
                    fragmentManager = parentFragmentManager,
                    getString(R.string.save_draft),
                    getString(R.string.description_submit_feedback),
                    getString(R.string.Uwazi_Action_Save_Draft).uppercase(),
                    getString(R.string.action_exit_without_saving),
                    object : BottomSheetUtils.ActionConfirmed {
                        override fun accept(isConfirmed: Boolean) {
                            if (isConfirmed) {
                                if (feedbackInstance != null) {
                                    feedbackInstance!!.text = binding.newFeedbackEditDescription.text.toString()
                                } else {
                                    feedbackInstance = FeedbackInstance(status = FeedbackStatus.DRAFT, text = binding.newFeedbackEditDescription.text.toString())
                                }
                                viewModel.saveFeedbackDraft(feedbackInstance!!)
                            } else {
                                nav().popBackStack()
                            }
                        }
                    })
        else nav().popBackStack()
    }

    private fun highLightButton() {

        binding.sendFeedbackBtn.setBackgroundResource(if (isSubmitEnabled) R.drawable.bg_round_orange_btn else R.drawable.bg_round_orange16_btn)
        binding.sendFeedbackBtn.isEnabled = isSubmitEnabled
    }


    override fun onBackPressed(): Boolean {
        handleBackButton()
        return true
    }

    private fun initObservers() {

        viewModel.feedbackSubmitted.observe(viewLifecycleOwner) { isFeedbackSubmitted ->
            if (isFeedbackSubmitted) {
                onFeedbackSubmittedSuccess()
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000)
                }
                nav().popBackStack()
            } else {
                handleNoInternetBehavior()
            }
        }
        with(viewModel) {
            progress.observe(
                    viewLifecycleOwner,
            ) {
                binding.progressCircular.isVisible = it
            }
        }

        viewModel.draftFeedBackInstance.observe(viewLifecycleOwner) { draft ->
            binding.newFeedbackEditDescription.setText(draft.text)
            feedbackInstance = draft
        }

        viewModel.feedbackSavedToBeSubmitted.observe(viewLifecycleOwner) { isSavedToBeSubmit ->
            if (isSavedToBeSubmit) feedbackInstance?.let { viewModel.submitFeedback(instance = it) }
            else {
                handleNoInternetBehavior()
            }

        }
        viewModel.feedbackSavedAsDraft.observe(viewLifecycleOwner) { isSavedAsDraft ->
            if (isSavedAsDraft) nav().popBackStack()
        }


    }

    private fun onFeedbackSubmittedSuccess() {
        DialogUtils.showBottomMessage(activity, getString(R.string.thanks_for_your_feedback), true)
    }

    private fun setupFeedbackSwitchView() {

        if (Preferences.isFeedbackSharingEnabled()) {
            binding.sendFeedbackBtn.isVisible = true
            binding.newFeedbackEditDescription.isVisible = true

        } else {
            binding.sendFeedbackBtn.isVisible = false
            binding.newFeedbackEditDescription.isVisible = false
            binding.newFeedbackEditDescription.setText("")

        }
    }

    private fun scheduleWorker() {
        val constraints =
                Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
        val onetimeJob = OneTimeWorkRequest.Builder(WorkerUploadReport::class.java)
                .setConstraints(constraints).build()
        WorkManager.getInstance(baseActivity)
                .enqueueUniqueWork("WorkerUploadReport", ExistingWorkPolicy.KEEP, onetimeJob)
    }

}