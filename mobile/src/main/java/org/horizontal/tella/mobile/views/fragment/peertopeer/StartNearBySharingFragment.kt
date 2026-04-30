package org.horizontal.tella.mobile.views.fragment.peertopeer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.hzontal.tella_locking_ui.ui.pin.pinview.ResourceUtils.getColor
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.sharedpref.Preferences
import org.horizontal.tella.mobile.databinding.StartNearBySharingFragmentBinding
import org.horizontal.tella.mobile.util.Util
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow.PeerToPeerParticipant
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel

class StartNearBySharingFragment : BaseBindingFragment<StartNearBySharingFragmentBinding>(
    StartNearBySharingFragmentBinding::inflate
) {
    private val viewModel: PeerToPeerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        binding.apply {
            nextBtn.setTextColor(getColor(baseActivity, R.color.wa_white_40))
            toolbar.backClickListener = { baseActivity.onBackPressed() }
            toolbar.onRightClickListener = {
                navManager().navigateFromStartNearBySharingFragmentToTipsToConnectFragment()
            }
            learnMoreTextView.setOnClickListener {
                baseActivity.maybeChangeTemporaryTimeout()
                Util.startBrowserIntent(context, getString(R.string.peerToPeer_documentation_url))
            }

            sendFilesBtn.setOnClickListener { selectOption(true) }
            receiveFilesBtn.setOnClickListener { selectOption(false) }
            nextBtn.setOnClickListener { }
        }
    }

    private fun selectOption(isSend: Boolean) {
        binding.apply {
            sendFilesBtn.isChecked = isSend
            receiveFilesBtn.isChecked = !isSend
            nextBtn.setOnClickListener { onNextClicked() }
            nextBtn.setTextColor(getColor(baseActivity, R.color.wa_white))
        }
    }

    private fun onNextClicked() {
        with(binding) {
            when {
                sendFilesBtn.isChecked -> {
                    viewModel.peerToPeerParticipant = PeerToPeerParticipant.SENDER
                    maybeShowWifiConfirmation()
                }

                receiveFilesBtn.isChecked -> {
                    viewModel.peerToPeerParticipant = PeerToPeerParticipant.RECIPIENT
                    maybeShowWifiConfirmation()
                }

                else -> {}
            }
        }
    }

    private fun maybeShowWifiConfirmation() {
        if (Preferences.isShowP2pWifiConfirmationSheet()) {
            showWifiConfirmationBottomSheet()
        } else {
            navigateToNextStep()
        }
    }

    private fun showWifiConfirmationBottomSheet() {
        val context = context ?: return
        val dialog = BottomSheetDialog(
            context,
            org.hzontal.shared_ui.R.style.AppBottomSheetDialogTheme
        )
        val contentView = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_p2p_wifi_confirmation, null, false)
        val dontShowAgainCheckBox = contentView.findViewById<CheckBox>(R.id.dontShowAgainCheckBox)
        val noButton = contentView.findViewById<TextView>(R.id.cancelButton)
        val yesButton = contentView.findViewById<TextView>(R.id.continueButton)

        noButton.setOnClickListener { dialog.dismiss() }
        yesButton.setOnClickListener {
            if (dontShowAgainCheckBox.isChecked) {
                Preferences.setShowP2pWifiConfirmationSheet(false)
            }
            dialog.dismiss()
            navigateToNextStep()
        }

        dialog.setContentView(contentView)
        dialog.show()
    }

    private fun navigateToNextStep() {
        if (viewModel.peerToPeerParticipant == PeerToPeerParticipant.RECIPIENT) {
            navManager().navigateFromStartNearBySharingFragmentToQrCodeScreen()
        } else {
            navManager().navigateFromStartNearBySharingFragmentToScanQrCodeScreen()
        }
    }
}


