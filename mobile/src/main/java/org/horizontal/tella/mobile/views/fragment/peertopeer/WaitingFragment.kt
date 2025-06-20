package org.horizontal.tella.mobile.views.fragment.peertopeer

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentWaitingBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.hzontal.shared_ui.utils.DialogUtils

/**
 * Created by wafa on 3/6/2025.
 */
class WaitingFragment :
    BaseBindingFragment<FragmentWaitingBinding>(FragmentWaitingBinding::inflate) {
    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private val viewModelSender: SenderViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val isSender = arguments?.getBoolean("isSender") ?: false

        binding.toolbar.backClickListener = {
            navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
        }
        if (!isSender) {
            binding.toolbar.setStartTextTitle(getString(R.string.receive_files))
            binding.waitingText.text = getString(R.string.waiting_for_the_sender_to_share_files)
        } else {
            binding.toolbar.setStartTextTitle(getString(R.string.send_files))
            binding.waitingText.text = getString(R.string.waiting_for_the_recipient_to_accept_files)
        }

        viewModel.incomingPrepareRequest.observe(viewLifecycleOwner) { request ->
            if (request != null && !viewModel.hasNavigatedToSuccessFragment) {
                viewModel.hasNavigatedToSuccessFragment = true

                val fileCount = request.files.size
                bundle.putInt("fileCount", fileCount)
                bundle.putString("sessionId", request.sessionId)
                navManager().navigateFromWaitingFragmentToRecipientSuccessFragment()
            }
        }

        viewModelSender.prepareRejected.observe(viewLifecycleOwner) { wasRejected ->
            if (wasRejected) {
                findNavController().previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("prepare_upload_result", true)

                findNavController().popBackStack()
            }
        }

        val navBackStackEntry = findNavController().currentBackStackEntry
        navBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("prepare_upload")
            ?.observe(viewLifecycleOwner) { wasRejected ->
                if (wasRejected) {
                    DialogUtils.showBottomMessage(
                        baseActivity, "Sender's files rejected.",
                        true
                    )
                }
            }
    }


    override fun onStop() {
        super.onStop()
        viewModel.clearPrepareRequest()
    }
}