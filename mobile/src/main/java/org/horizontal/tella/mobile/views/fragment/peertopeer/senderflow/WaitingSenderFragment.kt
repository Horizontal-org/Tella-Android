package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.hzontal.tella_vault.VaultFile
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.peertopeer.remote.PrepareUploadResult
import org.horizontal.tella.mobile.databinding.FragmentWaitingBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.SenderViewModel
import org.hzontal.shared_ui.utils.DialogUtils

/**
 * Created by wafa on 3/6/2025.
 */
class WaitingSenderFragment :
    BaseBindingFragment<FragmentWaitingBinding>(FragmentWaitingBinding::inflate) {
    private val viewModel: SenderViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.setStartTextTitle(getString(R.string.send_files))
        binding.waitingText.text = getString(R.string.waiting_for_the_recipient_to_accept_files)
        val selectedFiles =
            arguments?.getSerializable("selectedFiles") as? List<VaultFile> ?: emptyList()
        viewModel.prepareUploadsFromVaultFiles(selectedFiles)
        binding.toolbar.backClickListener = {
            navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
        }

        viewModel.prepareRejected.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { wasRejected ->
                if (wasRejected) {
                    findNavController().previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("transferRejected", true)
                    findNavController().popBackStack()
                }
            }
        }

        viewModel.prepareResults.observe(viewLifecycleOwner) { response ->
            val fileInfos = response.files
            fileInfos.forEach { fileInfo ->
                viewModel.p2PSharedState.session?.files?.let { filesMap ->
                    filesMap[fileInfo.id]?.transmissionId = fileInfo.transmissionId
                }
                navManager().navigateFromWaitingSenderFragmentToUploadFilesFragment()
            }
        }

    }
}