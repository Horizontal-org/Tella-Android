package rs.readahead.washington.mobile.views.fragment.feedback

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.data.entity.feedback.FeedbackBodyEntity
import rs.readahead.washington.mobile.databinding.FragmentSendFeedbackBinding

@AndroidEntryPoint
class SendFeedbackFragment : Fragment() {
    private var binding: FragmentSendFeedbackBinding? = null
    private  var feedbackInstance : FeedbackBodyEntity? = null

    companion object {
        fun newInstance() = SendFeedbackFragment()
    }

   // private lateinit var viewModel: SendFeedbackViewModel
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
        feedbackInstance  = FeedbackBodyEntity("ANDROID","test test")

        binding?.sendFeedbackBtn?.setOnClickListener {
            feedbackInstance?.let { it1 ->
                viewModel.submitFeedback(it1,true) }
        }

}
}