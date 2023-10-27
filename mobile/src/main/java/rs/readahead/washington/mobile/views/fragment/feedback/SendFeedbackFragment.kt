package rs.readahead.washington.mobile.views.fragment.feedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.entity.feedback.FeedbackBodyEntity
import rs.readahead.washington.mobile.databinding.FragmentSendFeedbackBinding

@AndroidEntryPoint
class SendFeedbackFragment : Fragment() {
    private var binding: FragmentSendFeedbackBinding? = null
    private var feedbackInstance: FeedbackBodyEntity? = null

    companion object {
        fun newInstance() = SendFeedbackFragment()
    }

    private val viewModel by viewModels<SendFeedbackViewModel>()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSendFeedbackBinding.inflate(inflater, container, false)
        return binding?.root!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val feedbackSwitch = binding?.feedbackSwitch
        feedbackSwitch?.mSwitch?.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            binding?.sendFeedbackBtn?.isVisible = isChecked
            binding?.newFeedbackEditDescription?.isVisible = isChecked

        }
        feedbackInstance = FeedbackBodyEntity("ANDROID", binding?.newFeedbackEditDescription.toString())

        binding?.sendFeedbackBtn?.setOnClickListener {
            feedbackInstance?.let { it1 ->
                viewModel.submitFeedback(it1, true)
            }

        }

//        BottomSheetUtils.showConfirmSheet(
//                fragmentManager = parentFragmentManager,
//                getString(R.string.save_draft),
//                getString(R.string.verification_prompt_dialog_expl),
//                getString(R.string.verification_prompt_action_enable_GPS),
//                getString(R.string.verification_prompt_action_ignore),
//                object : BottomSheetUtils.ActionConfirmed {
//                    override fun accept(isConfirmed: Boolean) {
//                        if (isConfirmed) {
//
//                        } else {
//                        }
//                    }
//                })

        initObservers()


    }

    private fun initObservers() {
        viewModel.feedbackSubmitted.observe(viewLifecycleOwner) {
            onFeedbackSubmittedSuccess()
            activity?.onBackPressed()
        }
        with(viewModel) {
            progress.observe(
                    viewLifecycleOwner,
            ) {
                binding?.progressCircular?.isVisible = it
            }
        }
    }

    private fun onFeedbackSubmittedSuccess() {
        DialogUtils.showBottomMessage(activity, getString(R.string.thanks_for_your_feedback), true)
    }

}