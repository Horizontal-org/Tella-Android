package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentRecipientSuccessBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerToPeerViewModel
import org.hzontal.shared_ui.utils.DialogUtils

/**
 * Created by wafa on 3/6/2025.
 */
class RecipientSuccessFragment : BaseBindingFragment<FragmentRecipientSuccessBinding>(FragmentRecipientSuccessBinding::inflate){
    private val viewModel: PeerToPeerViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
    }
    private fun initView() {
        val fileCount = arguments?.getInt("fileCount") ?: 0
        val sessionId = arguments?.getString("sessionId").orEmpty()

        with(binding) {
            // Set the dynamic message
            waitingText.text = getString(R.string.prepare_upload_message, fileCount)

            // Handle Accept/Reject buttons
            acceptBtn.setOnClickListener {
             //   onAcceptFilesSelected()
                viewModel.confirmPrepareUpload(sessionId, true)
                DialogUtils.showBottomMessage(
                    baseActivity,
                    "The receiver accepted the files transfer ",
                    false,
                    3000
                )
            }

            rejectBtn.setOnClickListener {
                viewModel.confirmPrepareUpload(sessionId, false)
                // Set result safely via SavedStateHandle
                findNavController().previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("prepare_upload_result", true)

                // Pop back
                findNavController().popBackStack()
            }
        }
    }


}