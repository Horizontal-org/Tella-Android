package org.horizontal.tella.mobile.views.fragment.peertopeer

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.hzontal.tella_locking_ui.ui.pin.pinview.ResourceUtils.getColor
import org.horizontal.tella.mobile.R
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
                    navManager().navigateFromStartNearBySharingFragmentToConnectHotspotFragment()
                }

                receiveFilesBtn.isChecked -> {
                    viewModel.peerToPeerParticipant = PeerToPeerParticipant.RECIPIENT
                    navManager().navigateFromStartNearBySharingFragmentToConnectHotspotFragment()
                }

                else -> {}
            }
        }
    }
}


