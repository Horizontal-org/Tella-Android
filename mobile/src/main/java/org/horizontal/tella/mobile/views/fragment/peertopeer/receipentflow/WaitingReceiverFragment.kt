package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerServerStarterManager
import org.horizontal.tella.mobile.databinding.FragmentWaitingBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import org.hzontal.shared_ui.utils.DialogUtils
import javax.inject.Inject

/**
 * Created by wafa on 3/6/2025.
 */

@AndroidEntryPoint
class WaitingReceiverFragment :
    BaseBindingFragment<FragmentWaitingBinding>(FragmentWaitingBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()

    @Inject
    lateinit var peerServerStarterManager: PeerServerStarterManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar()
        observeIncomingPrepareRequest()
        observeReceiverRejection()
        observeSenderCloseConnection()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setStartTextTitle(getString(R.string.receive_files))
            backClickListener = {
                viewModel.resetRegistrationState()
                navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
            }
        }
        binding.waitingText.text = getString(R.string.waiting_for_the_sender_to_share_files)
    }

    private fun observeIncomingPrepareRequest() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                viewModel.incomingPrepareRequest.collect { request ->
                    if (!viewModel.shouldSkipWaitingToPrepareSuccessNavigation() &&
                        isAdded &&
                        findNavController().currentDestination?.id == R.id.waitingReceiverFragment
                    ) {
                        viewModel.markNavigatedFromWaitingToPrepareSuccess()

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
                        isError = false
                    )
                }
            }
    }

    /**
     * Sender called [org.horizontal.tella.mobile.data.peertopeer.remote.PeerApiRoutes.CLOSE] on our
     * embedded server; tear down the listener and return to the nearby-sharing start screen.
     */
    private fun observeSenderCloseConnection() {
        viewModel.closeConnection.observe(viewLifecycleOwner) { closed ->
            if (closed != true) return@observe
            if (!isAdded ||
                findNavController().currentDestination?.id != R.id.waitingReceiverFragment
            ) {
                return@observe
            }
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.resetRegistrationState()
                viewModel.discardStalePrepareOfferReplayAndNavigationGate()
                withContext(Dispatchers.IO) {
                    peerServerStarterManager.stopServer()
                }
                navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
            }
        }
    }

}