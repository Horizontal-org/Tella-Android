package rs.readahead.washington.mobile.views.fragment.feedback

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
import rs.readahead.washington.mobile.util.jobs.WorkerSendFeedBack
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
        (activity as SettingsActivity).toolbar.setStartTextTitle(getString(R.string.feedback_title))

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
            if (isSubmitEnabled) {
                if (feedbackInstance == null)
                    feedbackInstance = FeedbackInstance(text = binding.newFeedbackEditDescription.text.toString())
                else
                    feedbackInstance!!.text = binding.newFeedbackEditDescription.text.toString()

                if (MyApplication.isConnectedToInternet(baseActivity)) {
                    feedbackInstance!!.status = FeedbackStatus.SUBMISSION_IN_PROGRESS
                    viewModel.saveFeedbackToBeSubmitted(feedbackInstance!!)
                } else {
                    feedbackInstance!!.status = FeedbackStatus.SUBMISSION_PENDING
                    viewModel.saveFeedbackToBeSubmitted(feedbackInstance!!)
                    handleNoInternetBehavior()
                }
            }

        }
        (activity as SettingsActivity).setToolbarHomeIcon(R.drawable.ic_close_white)
        (activity as SettingsActivity).toolbar.backClickListener = {
            handleBackButton()

        }
    }

    private fun handleNoInternetBehavior() {
        scheduleWorker()
        activity?.let {
            DialogUtils.showBottomMessageWithButton(it, getString(R.string.not_internet_msg)) { nav().popBackStack() }
        }
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
        viewModel.feedbackSubmittedInBackground.observe(viewLifecycleOwner) { isFeedbackSubmitted ->
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


        viewModel.feedbackSubmitted.observe(viewLifecycleOwner) { isFeedbackSubmitted ->
            if (isFeedbackSubmitted) {
                onFeedbackSubmittedSuccess()
                nav().popBackStack()
            } else {
                handleNoInternetBehavior()
                nav().popBackStack()
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
            if (isSavedToBeSubmit) {
                feedbackInstance?.let {
                    viewModel.submitFeedback(instance = it)
                    feedbackInstance = null
                }
            }
        }
        viewModel.feedbackSavedAsDraft.observe(viewLifecycleOwner) { isSavedAsDraft ->
            if (isSavedAsDraft) nav().popBackStack()
        }

    }

    private fun onFeedbackSubmittedSuccess() {
        activity?.let { DialogUtils.showBottomMessage(it, getString(R.string.thanks_for_your_feedback), true,4000) }
    }

    private fun setupFeedbackSwitchView() {

        if (Preferences.isFeedbackSharingEnabled()) {
            binding.sendFeedbackBtn.isVisible = true
            binding.newFeedbackEditDescription.isVisible = true

        } else {
            binding.sendFeedbackBtn.isVisible = false
            binding.newFeedbackEditDescription.isVisible = false
            binding.newFeedbackEditDescription.setText("")
            feedbackInstance = null

        }
    }

    private fun scheduleWorker() {
        val constraints =
                Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
        val onetimeJob = OneTimeWorkRequest.Builder(WorkerSendFeedBack::class.java)
                .setConstraints(constraints).build()
        WorkManager.getInstance(baseActivity)
                .enqueueUniqueWork("WorkerSendFeedBack", ExistingWorkPolicy.KEEP, onetimeJob)
        WorkManager.getInstance(baseActivity).getWorkInfoByIdLiveData(onetimeJob.id)
                .observeForever(object : Observer<WorkInfo> {
                    override fun onChanged(workInfo: WorkInfo?) {
                        if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                            activity?.let { DialogUtils.showBottomMessage(it, getString(R.string.feedback_sent_msg), true,4000) }
                        }
                        /* Here We remove the Observer if Not needed anymore
                             'this' here = the Observer */
                        WorkManager.getInstance(baseActivity).getWorkInfoByIdLiveData(onetimeJob.id)
                                .removeObserver(this)
                    }

                })
    }

}

