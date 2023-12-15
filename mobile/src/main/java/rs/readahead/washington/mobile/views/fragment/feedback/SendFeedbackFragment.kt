package rs.readahead.washington.mobile.views.fragment.feedback

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.hzontal.tella_locking_ui.common.extensions.onChange
import com.hzontal.utils.Util
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.KeyboardUtil
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.databinding.FragmentSendFeedbackBinding
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackInstance
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackStatus
import rs.readahead.washington.mobile.util.jobs.WorkerSendFeedBack
import rs.readahead.washington.mobile.views.activity.SettingsActivity
import rs.readahead.washington.mobile.views.activity.clean_insights.CleanInsightContributeFragment
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.vault.attachements.OnNavBckListener

@AndroidEntryPoint
class SendFeedbackFragment :
    BaseBindingFragment<FragmentSendFeedbackBinding>(FragmentSendFeedbackBinding::inflate),
    OnNavBckListener {
    private var feedbackInstance: FeedbackInstance? = null
    private var isDescriptionEnabled = false

    private val viewModel by viewModels<SendFeedbackViewModel>()
    private var isSubmitEnabled = isDescriptionEnabled && Preferences.isFeedbackSharingEnabled()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Preferences.isFeedbackSharingEnabled()) viewModel.getFeedBackDraft()
        initViews()
        initObservers()
    }

    /**
     * Initializes the views in the fragment, sets up the feedback switch, and configures the toolbar.
     */
    private fun initViews() {
        setupFeedbackSwitchView()
        KeyboardUtil.hideKeyboard(baseActivity, binding.root)
        (activity as SettingsActivity).toolbar.setStartTextTitle(getString(R.string.feedback_title))
        val feedbackSwitch = binding.feedbackSwitch
        feedbackSwitch.mSwitch.isChecked = Preferences.isFeedbackSharingEnabled()
        // Set an event listener for changes in the feedback switch state
        feedbackSwitch.mSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            Preferences.setFeedbackSharingEnabled(isChecked)
            setupFeedbackSwitchView()
        }
        // Set up listener for changes in the feedback description
        binding.newFeedbackEditDescription.onChange { description ->
            isDescriptionEnabled = description.isNotEmpty()
            isSubmitEnabled = isDescriptionEnabled && feedbackSwitch.mSwitch.isChecked
            highLightButton()
        }
        // Set up click listener for the "Send Feedback" button
        binding.sendFeedbackBtn.setOnClickListener {
            if (isSubmitEnabled) {
                // Create or update the feedback instance with the entered text
                if (feedbackInstance == null) feedbackInstance =
                    FeedbackInstance(text = binding.newFeedbackEditDescription.text.toString())
                else feedbackInstance!!.text = binding.newFeedbackEditDescription.text.toString()

                // Check internet connection before attempting to submit
                if (MyApplication.isConnectedToInternet(baseActivity)) {
                    feedbackInstance!!.status = FeedbackStatus.SUBMISSION_IN_PROGRESS
                    viewModel.saveFeedbackToBeSubmitted(feedbackInstance!!)
                } else {
                    feedbackInstance!!.status = FeedbackStatus.SUBMISSION_PENDING
                    viewModel.saveFeedbackToBeSubmitted(feedbackInstance!!)
                }
            }
        }
        binding.feedbackSwitch.setTextAndAction(R.string.action_learn_more) {
            Util.startBrowserIntent(requireContext(), getString(R.string.config_feedback_url))
        }
        // Set up the toolbar icons
        (activity as SettingsActivity).setToolbarHomeIcon(R.drawable.ic_close_white)
    }

    /**
     * Handles the behavior when there is no internet connection.
     * Schedules a worker to run in the background using scheduleWorker().
     * Displays a bottom message with a button to navigate back when clicked.
     */
    private fun handleNoInternetBehavior(resMessage: Int = R.string.not_internet_msg) {
        // Schedule a worker to run in the background
        scheduleWorker()
        // Show a bottom message with a button to navigate back
        activity?.let {
            DialogUtils.showBottomMessageWithButton(it, getString(resMessage)) {
                // Navigate back when the button is clicked
                nav().popBackStack()
            }
        }
    }

    /**
     * Handles the behavior when the back button is pressed.
     * If isSubmitEnabled is true, displays a confirmation dialog. If confirmed, saves the feedback
     * as a draft; otherwise, navigates back without saving.
     * If isSubmitEnabled is false, navigates back without any confirmation or saving.
     */
    fun handleBackButton() {
        if (isSubmitEnabled)
        // Display a confirmation dialog using BottomSheetUtils
            BottomSheetUtils.showConfirmSheet(fragmentManager = parentFragmentManager,
                titleText = getString(R.string.save_draft),
                descriptionText = getString(R.string.description_submit_feedback),
                actionButtonLabel = getString(R.string.Uwazi_Action_Save_Draft).uppercase(),
                cancelButtonLabel = getString(R.string.action_exit_without_saving),
                // Callback for the user's choice in the dialog
                object : BottomSheetUtils.ActionConfirmed {
                    override fun accept(isConfirmed: Boolean) {
                        if (isConfirmed) {
                            // Save feedback as a draft
                            if (feedbackInstance != null) {
                                feedbackInstance!!.text =
                                    binding.newFeedbackEditDescription.text.toString()
                            } else {
                                feedbackInstance = FeedbackInstance(
                                    status = FeedbackStatus.DRAFT,
                                    text = binding.newFeedbackEditDescription.text.toString()
                                )
                            }
                            viewModel.saveFeedbackDraft(feedbackInstance!!)
                        } else {
                            // User chose not to save, navigate back
                            nav().popBackStack()
                            (activity as SettingsActivity).toolbar.setBackIcon(R.drawable.ic_arrow_back_white_24dp)
                        }
                    }
                })
        // If not submitting, navigate back without any confirmation or saving
        else {
            nav().popBackStack()
            (activity as SettingsActivity).toolbar.setBackIcon(R.drawable.ic_arrow_back_white_24dp)
        }
    }

    /**
     * Updates the appearance and state of the "Send Feedback" button based on the current
     * state of isSubmitEnabled.
     */
    @SuppressLint("ResourceAsColor")
    private fun highLightButton() {
        // Determine the background resource based on the value of isSubmitEnabled
        binding.sendFeedbackBtn.setBackgroundResource(if (isSubmitEnabled) R.drawable.bg_round_orange_btn else R.drawable.bg_round_orange16_btn)
        binding.sendFeedbackBtn.setTextColor(if (isSubmitEnabled) R.color.wa_black_contrast else R.color.wa_white)
        // Set the background resource of the button
        binding.sendFeedbackBtn.isEnabled = isSubmitEnabled
    }


    override fun onBackPressed(): Boolean {
        handleBackButton()
        return true
    }

    /**
     * Set up observers for LiveData in the ViewModel.
     */
    private fun initObservers() {
        // Observer for background feedback submission
        viewModel.feedbackSubmittedInBackground.observe(viewLifecycleOwner) { isFeedbackSubmitted ->
            if (isFeedbackSubmitted) {
                // Handle successful feedback submission
                onFeedbackSubmittedSuccess()
                // Navigate back to the previous screen
                nav().popBackStack()
            }
        }

        viewModel.feedbackSubmitted.observe(viewLifecycleOwner) { isFeedbackSubmitted ->
            // Check if feedback is successfully submitted
            if (isFeedbackSubmitted) {
                onFeedbackSubmittedSuccess()
                nav().popBackStack()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            handleNoInternetBehavior(message)
        }

        // Observe progress changes to show/hide progress circular
        with(viewModel) {
            progress.observe(
                viewLifecycleOwner,
            ) {
                binding.progressCircular.isVisible = it
            }
        }

        // Observe changes in the feedback draft instance
        viewModel.draftFeedBackInstance.observe(viewLifecycleOwner) { draft ->
            binding.newFeedbackEditDescription.setText(draft.text)
            feedbackInstance = draft
        }

        // Observe changes in the flag indicating that feedback is saved to be submitted
        viewModel.feedbackSavedToBeSubmitted.observe(viewLifecycleOwner) { isSavedToBeSubmit ->
            if (isSavedToBeSubmit) {
                // Submit the feedback instance if it is saved to be submitted
                feedbackInstance?.let {
                    viewModel.submitFeedback(instance = it)
                    feedbackInstance = null
                }
            }
        }

        // Observe changes in the flag indicating that feedback is saved as a draft
        viewModel.feedbackSavedAsDraft.observe(viewLifecycleOwner) { isSavedAsDraft ->
            if (isSavedAsDraft) {
                // Pop the back stack if feedback is saved as a draft
                nav().popBackStack()
            }
        }
    }

    /**
     * Displays a bottom message with a thank you message for successful feedback submission.
     */
    private fun onFeedbackSubmittedSuccess() {
        // Show a bottom message with success message
        DialogUtils.showBottomMessage(
            baseActivity,
            getString(R.string.thanks_for_your_feedback),
            false,
            4000
        )
    }

    /**
     * Sets up the visibility of feedback-related views based on the feedback sharing status.
     */
    private fun setupFeedbackSwitchView() {
        if (Preferences.isFeedbackSharingEnabled()) {
            // If feedback sharing is enabled, show the feedback button and input description
            binding.sendFeedbackBtn.isVisible = true
            binding.newFeedbackEditDescription.isVisible = true
            binding.textInputLayoutDescription.isVisible = true
        } else {
            // If feedback sharing is disabled, hide the feedback button and input description
            binding.sendFeedbackBtn.isVisible = false
            binding.newFeedbackEditDescription.isVisible = false
            binding.textInputLayoutDescription.isVisible = false

            // Clear the feedback input description and reset feedbackInstance
            binding.newFeedbackEditDescription.setText("")
            feedbackInstance = null
        }
    }

    /**
     * Schedules a one-time background work for sending feedback.
     * The work is executed when the device is connected to the network.
     */
    private fun scheduleWorker() {
        // Define network constraints for the work
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        // Create a one-time work request for the WorkerSendFeedBack class
        val oneTimeJob =
            OneTimeWorkRequest.Builder(WorkerSendFeedBack::class.java).setConstraints(constraints)
                .build()

        // Enqueue the work with a unique name and keep existing work if it exists
        WorkManager.getInstance(baseActivity)
            .enqueueUniqueWork("WorkerSendFeedBack", ExistingWorkPolicy.KEEP, oneTimeJob)

        // Observe the work's status using LiveData
        WorkManager.getInstance(baseActivity).getWorkInfoByIdLiveData(oneTimeJob.id)
            .observeForever(object : Observer<WorkInfo> {
                override fun onChanged(workInfo: WorkInfo?) {
                    // Check if the work has succeeded
                    if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                        // Show a success message with a duration of 4000 milliseconds (4 seconds)
                        activity?.let {
                            DialogUtils.showBottomMessage(
                                it,
                                getString(R.string.feedback_sent_msg),
                                false,
                                4000
                            )
                        }
                    }
                    // Remove the observer when it's no longer needed
                    WorkManager.getInstance(baseActivity).getWorkInfoByIdLiveData(oneTimeJob.id)
                        .removeObserver(this)
                }
            })
    }
}

