package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.peertopeer.model.P2PFileStatus
import org.horizontal.tella.mobile.data.peertopeer.model.ProgressFile
import org.horizontal.tella.mobile.databinding.FragmentPeerToPeerResultBinding
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
        binding.tileTv.text =
            getString(if (allFilesTransferred) R.string.success_title else R.string.failure_title)
        binding.descriptionTv.text = computeSubtitle()
    }

    private fun setupButton() {
        if (!noFilesTransferred) {
            binding.viewFilesBtn.apply {
                setText(getString(R.string.view_files_action))
                visibility = View.VISIBLE
                setOnClickListener {
                    baseActivity.finish()
                }
            }
        } else {
            binding.viewFilesBtn.visibility = View.GONE
        }
    }

    private fun computeSubtitle(): String {
        val total = transferredFiles?.size ?: 0
        val successCount = transferredFiles?.count { it.status == successStatus } ?: 0
        val failureCount = total - successCount

        return when (successCount) {
            total -> {
                val resId = if (viewModel.peerToPeerParticipant == PeerToPeerParticipant.RECIPIENT)
                    R.plurals.success_file_received_from_sender
                else
                    R.plurals.success_file_sent_to_recipient

                return resources.getQuantityString(resId, total, total)
            }
            0 -> {
                baseActivity.resources.getQuantityString(
                    R.plurals.failure_file_received_expl,
                    failureCount,
                    failureCount
                )
            }
            else -> {
                getString(R.string.partial_success_summary, successCount, failureCount)
            }
        }
    }


    private val successStatus: P2PFileStatus
        get() = P2PFileStatus.FINISHED

    private val allFilesTransferred: Boolean
        get() = transferredFiles?.all { it.status == successStatus } == true

    private val noFilesTransferred: Boolean
        get() = transferredFiles?.none { it.status == successStatus } == true

}