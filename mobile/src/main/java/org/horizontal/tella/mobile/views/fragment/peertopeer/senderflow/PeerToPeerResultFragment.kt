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
    private lateinit var participant: PeerToPeerParticipant
    private var transferredFiles: List<ProgressFile>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get values from shared state
        participant = PeerToPeerParticipant.SENDER
        transferredFiles = viewModel.p2PState.session.files.values.toList()

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
        binding.tileTv.text = getString(if (allFilesTransferred) R.string.success_title else R.string.failure_title)
        binding.descriptionTv.text = computeSubtitle()
    }

    private fun setupButton() {
        if (!noFilesTransferred) {
            binding.viewFilesBtn.apply {
                setText(getString(R.string.view_files_action))
                visibility = View.VISIBLE
                setOnClickListener {
                    // TODO: Implement button action
                }
            }
        } else {
            binding.viewFilesBtn.visibility = View.GONE
        }
    }

    private fun computeSubtitle(): String {
        val total = transferredFiles?.size
        val successCount = transferredFiles?.count { it.status == successStatus }
        val failureCount = total?.minus(successCount!!)

        return when {
            successCount == total -> getString(if (total == 1) successSingleRes() else successMultipleRes(), total)
            failureCount == total -> getString(if (total == 1) failureSingleRes() else failureMultipleRes(), total)
            else -> {
                val resId = when {
                    successCount == 1 && failureCount == 1 -> R.string.file_received_file_not_received
                    successCount == 1 -> R.string.file_received_files_not_received
                    failureCount == 1 -> R.string.files_received_file_not_received
                    else -> R.string.files_received_files_not_received
                }
                getString(resId, successCount, failureCount)
            }
        }
    }

    private val successStatus: P2PFileStatus
        get() = P2PFileStatus.FINISHED

    private val allFilesTransferred: Boolean
        get() = transferredFiles?.all { it.status == successStatus } == true

    private val noFilesTransferred: Boolean
        get() = transferredFiles?.none { it.status == successStatus } == true

    private fun successSingleRes(): Int =
        if (participant == PeerToPeerParticipant.RECIPIENT)
            R.string.success_file_received_expl
        else
            R.string.success_file_sent_expl

    private fun successMultipleRes(): Int =
        if (participant == PeerToPeerParticipant.RECIPIENT)
            R.string.success_files_received_expl
        else
            R.string.success_files_sent_expl

    private fun failureSingleRes(): Int = R.string.failure_file_received_expl
    private fun failureMultipleRes(): Int = R.string.failure_files_received_expl
}