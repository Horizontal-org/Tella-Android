package org.horizontal.tella.mobile.views.fragment.peertopeer

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.databinding.FragmentWaitingBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.hzontal.shared_ui.utils.DialogUtils

/**
 * Created by wafa on 3/6/2025.
 */
class WaitingSenderFragment :
    BaseBindingFragment<FragmentWaitingBinding>(FragmentWaitingBinding::inflate) {
    private val viewModelSender: SenderViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.setStartTextTitle(getString(R.string.send_files))
        binding.waitingText.text = getString(R.string.waiting_for_the_recipient_to_accept_files)

        binding.toolbar.backClickListener = {
            navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
        }

        viewModelSender.prepareRejected.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { wasRejected ->
                if (wasRejected) {
                    findNavController().previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("transferRejected", true)
                    findNavController().popBackStack()
                }
            }
        }
    }
}