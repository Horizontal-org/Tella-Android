package rs.readahead.washington.mobile.views.fragment.feedback

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.hzontal.tella_locking_ui.common.extensions.onChange
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.databinding.FragmentSendFeedbackBinding
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackInstance
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackStatus
import rs.readahead.washington.mobile.util.StringUtils
import rs.readahead.washington.mobile.views.activity.SettingsActivity
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.vault.attachements.OnNavBckListener
import java.util.Locale

@AndroidEntryPoint
class SendFeedbackFragment : BaseBindingFragment<FragmentSendFeedbackBinding>(FragmentSendFeedbackBinding::inflate), OnNavBckListener {
    private var feedbackInstance: FeedbackInstance? = null
    private var isDescriptionEnabled = false

    companion object {
        fun newInstance() = SendFeedbackFragment()
    }

    private val viewModel by viewModels<SendFeedbackViewModel>()
    private val isSubmitEnabled by lazy { binding.feedbackSwitch.mSwitch.isEnabled  && isDescriptionEnabled }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(isSubmitEnabled) viewModel.getFeedBackDraft()

        binding.feedbackSwitch.mSwitch.isChecked = Preferences.isFeedbackSharingEnabled()

        val feedbackSwitch = binding.feedbackSwitch
        feedbackSwitch.mSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            binding.sendFeedbackBtn.isVisible = isChecked
            binding.newFeedbackEditDescription.isVisible = isChecked
            Preferences.setFeedbackSharingEnabled(isChecked)
        }
        binding.newFeedbackEditDescription.onChange { description ->
            isDescriptionEnabled = description.isNotEmpty()
            highLightButton()
        }

        binding.sendFeedbackBtn.setOnClickListener {
            feedbackInstance = FeedbackInstance(status = FeedbackStatus.DRAFT, text = binding.newFeedbackEditDescription.toString())
            feedbackInstance?.let { it1 ->
                viewModel.submitFeedback(it1)
            }
        }
        (activity as SettingsActivity).setToolbarHomeIcon(R.drawable.ic_close_white)
        (activity as SettingsActivity).toolbar.backClickListener = {
                handleBackButton()

        }


        initObservers()
    }

    fun handleBackButton() {
        BottomSheetUtils.showConfirmSheet(
                fragmentManager = parentFragmentManager,
                getString(R.string.save_draft),
                getString(R.string.description_submit_feedback),
                StringUtils.capitalize(getString(R.string.Uwazi_Action_Save_Draft), Locale.ROOT),
                getString(R.string.action_exit_without_saving),
                object : BottomSheetUtils.ActionConfirmed {
                    override fun accept(isConfirmed: Boolean) = if (isConfirmed) {
                        feedbackInstance = FeedbackInstance(status = FeedbackStatus.DRAFT, text = binding.newFeedbackEditDescription.text.toString(), platform = "ANDROID")
                        viewModel.saveFeedbackDraft(feedbackInstance!!)
                    } else {
                    }
                })

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

        viewModel.feedbackSubmitted.observe(viewLifecycleOwner) {
            onFeedbackSubmittedSuccess()
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
            }
            activity?.onBackPressed()
        }
        with(viewModel) {
            progress.observe(
                    viewLifecycleOwner,
            ) {
                binding.progressCircular.isVisible = it
            }
        }

        viewModel.draftFeedBackInstance.observe(viewLifecycleOwner) {
             binding.newFeedbackEditDescription.setText(it.text)
        }

    }

    private fun onFeedbackSubmittedSuccess() {
        DialogUtils.showBottomMessage(activity, getString(R.string.thanks_for_your_feedback), true)
    }


}