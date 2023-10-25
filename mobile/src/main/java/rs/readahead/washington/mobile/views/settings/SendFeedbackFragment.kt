package rs.readahead.washington.mobile.views.settings

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.view.isVisible
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.databinding.FragmentGeneralSettingsBinding
import rs.readahead.washington.mobile.databinding.FragmentSendFeedbackBinding

class SendFeedbackFragment : Fragment() {
    private var binding: FragmentSendFeedbackBinding? = null

    companion object {
        fun newInstance() = SendFeedbackFragment()
    }

    private lateinit var viewModel: SendFeedbackViewModel

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSendFeedbackBinding.inflate(inflater, container, false)
        return binding?.root!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SendFeedbackViewModel::class.java]
        val feedbackSwitch = binding?.feedbackSwitch
        feedbackSwitch?.mSwitch?.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            binding?.sendFeedbackBtn?.isVisible = isChecked
            binding?.newFeedbackEditDescription?.isVisible = isChecked

        }

}
}