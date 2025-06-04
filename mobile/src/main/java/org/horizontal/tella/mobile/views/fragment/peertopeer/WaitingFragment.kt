package org.horizontal.tella.mobile.views.fragment.peertopeer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentWaitingBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.hzontal.shared_ui.utils.DialogUtils

/**
 * Created by wafa on 3/6/2025.
 */
class WaitingFragment : BaseBindingFragment<FragmentWaitingBinding>(FragmentWaitingBinding::inflate){
    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private val viewModelSender: SenderViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val isSender = arguments?.getBoolean("isSender") ?: false

        if (!isSender) {
            binding.toolbar.setStartTextTitle(getString(R.string.receive_files))
            binding.waitingText.text = getString(R.string.waiting_for_the_sender_to_share_files)
        } else {
            binding.toolbar.setStartTextTitle(getString(R.string.send_files))
            binding.waitingText.text = getString(R.string.waiting_for_the_recipient_to_accept_files)
        }

        viewModel.incomingPrepareRequest.observe(viewLifecycleOwner) { request ->
            val fileCount = request.files.size
            bundle.putInt("fileCount", fileCount)
            bundle.putString("sessionId", request.sessionId)
            navManager().navigateFromWaitingFragmentToRecipientSuccessFragment()
        }

        viewModelSender.prepareRejected.observe(viewLifecycleOwner) { wasRejected ->
            if (wasRejected) {
                parentFragmentManager.setFragmentResult("prepare_upload_result", bundleOf("rejected" to true))
                findNavController().popBackStack()
            }
        }
        parentFragmentManager.setFragmentResultListener("prepare_upload_result", viewLifecycleOwner) { _, result ->
            val wasRejected = result.getBoolean("rejected", false)
            if (wasRejected) {
                DialogUtils.showBottomMessage(
                    baseActivity, "Sender's files rejected.",
                    true
                )
            }
        }
    }

}