package org.horizontal.tella.mobile.views.fragment.peertopeer

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.StartNearBySharingFragmentBinding
import org.horizontal.tella.mobile.util.Util
import org.horizontal.tella.mobile.util.hide
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

class StartNearBySharingFragment : BaseBindingFragment<StartNearBySharingFragmentBinding>(
    StartNearBySharingFragmentBinding::inflate
) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        binding?.apply {
            nextBtn.hide()

            learnMoreTextView.setOnClickListener {
                baseActivity.maybeChangeTemporaryTimeout()
                Util.startBrowserIntent(context, getString(R.string.peerToPeer_documentation_url))
            }

            sendFilesBtn.setOnClickListener { selectOption(true) }
            receiveFilesBtn.setOnClickListener { selectOption(false) }
            nextBtn.setOnClickListener { onNextClicked() }
        }
    }
    private fun selectOption(isSend: Boolean) {
        binding?.apply {
            sendFilesBtn.isChecked = isSend
            receiveFilesBtn.isChecked = !isSend
            nextBtn.isVisible = true
        }
    }

    private fun onNextClicked() {
        navManager().navigateFromStartNearBySharingFragmentToGoogleDriveConnectHotspotFragment()
    }

}


