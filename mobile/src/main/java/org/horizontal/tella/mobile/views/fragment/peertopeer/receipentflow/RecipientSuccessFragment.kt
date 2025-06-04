package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentRecipientSuccessBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerToPeerViewModel

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
                onAcceptFilesSelected()
                viewModel.confirmPrepareUpload(sessionId, true)
            }

            rejectBtn.setOnClickListener {
                onRejectFilesSelected()
                viewModel.confirmPrepareUpload(sessionId, false)
                // Set result to notify previous fragment
                setFragmentResult("prepare_upload_result", bundleOf("rejected" to true))
                nav().popBackStack()

            }
        }
    }

    private fun onAcceptFilesSelected() {
        binding.acceptBtn.isChecked = true
        binding.rejectBtn.isChecked = false
    }

    private fun onRejectFilesSelected() {
        binding.rejectBtn.isChecked = true
        binding.acceptBtn.isChecked = false
    }

}