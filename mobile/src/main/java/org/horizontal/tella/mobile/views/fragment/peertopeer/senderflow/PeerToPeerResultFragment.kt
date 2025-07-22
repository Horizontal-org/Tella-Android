package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
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
        binding.toolbar.setStartTextTitle( getString(if (allFilesTransferred) R.string.success_title else R.string.result))
        binding.tileTv.text =
            getString(if (allFilesTransferred) R.string.success_title else R.string.failure_title)
        binding.descriptionTv.text = computeSubtitle()
    }

    private fun setupButton() {
        binding.toolbar.backClickListener = { baseActivity.finish() }

        binding.viewFilesBtn.apply {
            val isRecipient = viewModel.peerToPeerParticipant == PeerToPeerParticipant.RECIPIENT

            if (!noFilesTransferred && isRecipient) {
                setText(getString(R.string.view_files_action))
                visibility = View.VISIBLE
                setOnClickListener {
                    val intent = Intent(requireContext(), MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("navigateTo", "attachments_screen")
                    }
                    startActivity(intent)
                    requireActivity().finish() // finish PeerToPeerActivity
                }
            } else {
                visibility = View.GONE
            }
        }
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

}