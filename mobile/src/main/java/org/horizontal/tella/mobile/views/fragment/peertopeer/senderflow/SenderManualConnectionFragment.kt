package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.hzontal.tella_locking_ui.common.extensions.onChange
import org.horizontal.tella.mobile.databinding.SenderManualConnectionBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerToPeerViewModel
import org.hzontal.shared_ui.bottomsheet.KeyboardUtil

class SenderManualConnectionFragment :
    BaseBindingFragment<SenderManualConnectionBinding>(SenderManualConnectionBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()
        initView()
    }

    private fun initView() {
        binding.connectCode.onChange {
            updateNextButtonState()
        }
        binding.port.onChange {
            updateNextButtonState()
        }

        binding.port.onChange {
            updateNextButtonState()
        }

        updateNextButtonState()

        KeyboardUtil(binding.root)

    }

    private fun initListeners() {
        binding.backBtn.setOnClickListener {
            nav().popBackStack()
        }

        binding.nextBtn.setOnClickListener {

        }
    }

    private fun isInputValid(): Boolean {
        return binding.connectCode.text?.isNotBlank() == true &&
                binding.pin.text?.isNotBlank() == true &&
                binding.port.text?.isNotBlank() == true
    }

    private fun updateNextButtonState() {
        val enabled = isInputValid()
        binding.nextBtn.isEnabled = enabled
        binding.nextBtn.setTextColor(
            ContextCompat.getColor(
                baseActivity,
                if (enabled) android.R.color.white else android.R.color.darker_gray
            )
        )
    }

}