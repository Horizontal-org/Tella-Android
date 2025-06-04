package org.horizontal.tella.mobile.views.fragment.peertopeer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentWaitingBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

/**
 * Created by wafa on 3/6/2025.
 */
class WaitingFragment : BaseBindingFragment<FragmentWaitingBinding>(FragmentWaitingBinding::inflate){
    private val viewModel: PeerToPeerViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val isSender = arguments?.getBoolean("isSender") ?: false

        if (!isSender) {
            binding.toolbar.setToolbarTitle(getString(R.string.receive_files))
            binding.waitingText.text = getString(R.string.waiting_for_the_sender_to_share_files)
        } else {
            binding.toolbar.setToolbarTitle(getString(R.string.send_files))
            binding.waitingText.text = "Waiting for the recipient to accept files"
        }

        viewModel.incomingPrepareRequest.observe(viewLifecycleOwner) { request ->
            val fileCount = request.files.size
            bundle.putInt("fileCount", fileCount)
            bundle.putString("sessionId", request.sessionId)
            navManager().navigateFromWaitingFragmentToRecipientSuccessFragment()
        }

        childFragmentManager.setFragmentResultListener("prepare_upload_result", viewLifecycleOwner) { _, result ->
            val wasRejected = result.getBoolean("rejected", false)
            if (wasRejected) {
                Toast.makeText(requireContext(), "Sender's files rejected.", Toast.LENGTH_LONG).show()
            }
        }
    }

}