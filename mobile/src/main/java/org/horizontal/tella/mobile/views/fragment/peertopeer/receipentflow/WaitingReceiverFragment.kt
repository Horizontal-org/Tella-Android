package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentWaitingBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import org.hzontal.shared_ui.utils.DialogUtils

/**
 * Created by wafa on 3/6/2025.
 */

@AndroidEntryPoint
class WaitingReceiverFragment :
    BaseBindingFragment<FragmentWaitingBinding>(FragmentWaitingBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private var navigated = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar()
        observeIncomingPrepareRequest()
        observeReceiverRejection()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setStartTextTitle(getString(R.string.receive_files))
            backClickListener = {
                navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
            }
        }
        binding.waitingText.text = getString(R.string.waiting_for_the_sender_to_share_files)
    }

    private fun observeIncomingPrepareRequest() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                viewModel.incomingPrepareRequest.collect { request ->
                    if (!navigated &&
                        isAdded &&
                        findNavController().currentDestination?.id == R.id.waitingReceiverFragment
                    ) {
                        navigated = true

                        bundle.putInt("fileCount", request.files.size)
                        bundle.putString("sessionId", request.sessionId)

                        navManager().navigateFromWaitingReceiverFragmentToRecipientSuccessFragment()
                    }
                }
            }
        }
    }

    private fun observeReceiverRejection() {
        val navBackStackEntry = findNavController().currentBackStackEntry
        navBackStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>("receiverDeclined")
            ?.observe(viewLifecycleOwner) { wasRejected ->
                if (wasRejected) {
                    DialogUtils.showBottomMessage(
                        baseActivity,
                        getString(R.string.sender_files_rejected),
                        isError = true
                    )
                }
            }
    }

}