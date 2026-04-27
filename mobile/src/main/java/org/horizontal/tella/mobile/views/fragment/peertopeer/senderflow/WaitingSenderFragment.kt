package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerServerStarterManager
import org.horizontal.tella.mobile.databinding.FragmentWaitingBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.FileTransferViewModel
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PrepareFailureKind
import javax.inject.Inject

/**
 * Created by wafa on 3/6/2025.
 */
@AndroidEntryPoint
class WaitingSenderFragment :
    BaseBindingFragment<FragmentWaitingBinding>(FragmentWaitingBinding::inflate) {
    private val viewModel: FileTransferViewModel by activityViewModels()

    @Inject
    lateinit var peerServerStarterManager: PeerServerStarterManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.setStartTextTitle(getString(R.string.send_files))
        binding.waitingText.text = getString(R.string.waiting_for_the_recipient_to_accept_files)

        val discontinueSession: () -> Unit = {
            peerServerStarterManager.stopServer()
            viewModel.closePeerConnection()
            navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
        }
        binding.toolbar.backClickListener = discontinueSession
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            discontinueSession()
        }

        viewModel.prepareFailure.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { kind ->
                when (kind) {
                    PrepareFailureKind.RECIPIENT_REJECTED -> {
                        findNavController().previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("prepareTransferRecipientRejected", true)
                        findNavController().popBackStack()
                    }
                    PrepareFailureKind.GENERIC -> {
                        navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
                        baseActivity.showToast(R.string.failure_title)
                    }
                }
            }
        }

        viewModel.prepareRejected.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { messageRes ->
                findNavController().previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("transferRejectedMessageRes", messageRes)
                findNavController().popBackStack()
            }
        }

        viewModel.prepareResults.observe(viewLifecycleOwner) { response ->
            val fileInfos = response.files
            fileInfos.forEach { fileInfo ->
                viewModel.p2PSharedState.session?.files?.get(fileInfo.id)?.let { pf ->
                    pf.transmissionId = fileInfo.transmissionId
                }
            }
            navManager().navigateFromWaitingSenderFragmentToUploadFilesFragment()
        }

        // Start prepare only after observers are registered so SingleLiveEvent results are not lost.
        viewModel.prepareUploadsFromVaultFiles()
    }
}
