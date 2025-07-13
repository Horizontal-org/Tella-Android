package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentRecipientSuccessBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerToPeerViewModel

/**
 * Created by wafa on 3/6/2025.
 */
class RecipientSuccessFragment :
    BaseBindingFragment<FragmentRecipientSuccessBinding>(FragmentRecipientSuccessBinding::inflate) {
    private val viewModel: PeerToPeerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
    }

    private fun initView() {

        with(binding) {
            // Set the dynamic message
            waitingText.text =
                getString(R.string.prepare_upload_message, viewModel.p2PState.session?.files?.size)

            // Handle Accept/Reject buttons
            acceptBtn.setOnClickListener {
                viewModel.confirmPrepareUpload(viewModel.getSessionId(), true)
                navManager().navigateFromRecipientSuccessFragmentToRecipientUploadFilesFragment()
            }

            rejectBtn.setOnClickListener {
                //TODO WE MOVE THIS TO THE NAV MANAGER
                viewModel.confirmPrepareUpload(viewModel.getSessionId(), false)
                // Set result safely via SavedStateHandle
                findNavController().previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("receiverDeclined", true)

                // Pop back
                findNavController().popBackStack()
            }
        }
    }


}