package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.peertopeer.model.P2PFileStatus
import org.horizontal.tella.mobile.data.peertopeer.model.ProgressFile
import org.horizontal.tella.mobile.databinding.FragmentPeerToPeerResultBinding
import org.horizontal.tella.mobile.views.activity.MainActivity
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel

class PeerToPeerResultFragment :
    BaseBindingFragment<FragmentPeerToPeerResultBinding>(FragmentPeerToPeerResultBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private var transferredFiles: List<ProgressFile>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get values from shared state
        transferredFiles = viewModel.p2PState.session?.files?.values?.toList()

        setupImage()
        setupTexts()
        setupButton()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { baseActivity.finish() }
    }


    private fun setupImage() {
        binding.setupImgV.run {
            val isSuccess = allFilesTransferred
            setImageResource(if (isSuccess) R.drawable.checked_circle else R.drawable.ic_warning_orange)

            if (!isSuccess) {
                val whiteColor = ContextCompat.getColor(context, R.color.wa_white)
                setColorFilter(whiteColor, PorterDuff.Mode.SRC_IN)
            } else {
                clearColorFilter()
            }
        }
    }

    private fun setupTexts() {
        binding.toolbar.setStartTextTitle(
            getString(if (allFilesTransferred) R.string.success_title else R.string.nearby_sharing_results_title)
        )
        val titleResId = when {
            allFilesTransferred -> R.string.success_title
            isPartialTransfer -> R.string.transfer_interrupted_title
            else -> R.string.failure_title
        }
        binding.tileTv.text = getString(titleResId)
        binding.descriptionTv.text = computeSubtitle()
    }

    private fun setupButton() {
        binding.toolbar.backClickListener = { baseActivity.finish() }

        val isSender = viewModel.peerToPeerParticipant == PeerToPeerParticipant.SENDER
        val showBackToHome = isSender || noFilesTransferred

        binding.backToHomeBtn.apply {
            visibility = if (showBackToHome) View.VISIBLE else View.GONE
            setOnClickListener { navigateToHome() }
        }

        binding.viewFilesBtn.apply {
            val isRecipient = viewModel.peerToPeerParticipant == PeerToPeerParticipant.RECIPIENT

            if (!noFilesTransferred && isRecipient) {
                setText(getString(R.string.view_files_action))
                visibility = View.VISIBLE
                setOnClickListener {
                    val transferFolderId = viewModel.getTransferFolderId()
                    val intent = Intent(requireContext(), MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(MainActivity.EXTRA_NAVIGATE_TO, "attachments_screen")
                        if (!transferFolderId.isNullOrBlank()) {
                            putExtra(MainActivity.EXTRA_VAULT_PARENT_ID, transferFolderId)
                        }
                    }
                    startActivity(intent)
                    requireActivity().finish() // finish PeerToPeerActivity
                }
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun computeSubtitle(): String {
        val total = transferredFiles?.size ?: 0
        val successCount = transferredFiles?.count { it.status == successStatus } ?: 0
        val failureCount = total - successCount

        return when {
            // Case 1: All successful
            successCount == total -> {
                val resId = if (viewModel.peerToPeerParticipant == PeerToPeerParticipant.RECIPIENT)
                    R.plurals.success_file_received_from_sender
                else
                    R.plurals.success_file_sent_to_recipient

                resources.getQuantityString(resId, total, total)
            }

            // Case 2: All failed
            failureCount == total -> {
                resources.getQuantityString(R.plurals.failure_file_received_expl, failureCount, failureCount)
            }

            //Case 3: Partial success
            else -> {
                getString(R.string.partial_success_summary, successCount, failureCount)
            }
        }
    }

    private val successStatus: P2PFileStatus
        get() = if (viewModel.peerToPeerParticipant == PeerToPeerParticipant.RECIPIENT)
            P2PFileStatus.SAVED
        else
            P2PFileStatus.FINISHED

    private val allFilesTransferred: Boolean
        get() = transferredFiles?.all { it.status == successStatus } == true

    private val noFilesTransferred: Boolean
        get() = transferredFiles?.none { it.status == successStatus } == true

    private val isPartialTransfer: Boolean
        get() = !allFilesTransferred && !noFilesTransferred

}