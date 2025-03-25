package org.horizontal.tella.mobile.views.fragment.peertopeer

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.StartNearBySharingFragmentBinding
import org.horizontal.tella.mobile.util.Util
import org.horizontal.tella.mobile.util.hide
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

class StartNearBySharingFragment  : BaseBindingFragment<StartNearBySharingFragmentBinding>(
    StartNearBySharingFragmentBinding::inflate), View.OnClickListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.learnMoreTextView?.setOnClickListener {
            Util.startBrowserIntent(context, getString(R.string.peerToPeer_documentation_url))
        }

        binding.nextBtn.hide()
        binding.sendFilesBtn.setOnClickListener {
            onSendFilesSelected()
        }
        binding.receiveFilesBtn.setOnClickListener {
            onReceiveFilesSelected()
        }
        binding.nextBtn.setOnClickListener(this)
    }

    private fun onReceiveFilesSelected() {
        binding.receiveFilesBtn.isChecked = true
        binding.sendFilesBtn.isChecked = false
        binding.nextBtn.isVisible = true
    }

    private fun onSendFilesSelected() {
        binding.sendFilesBtn.isChecked = true
        binding.receiveFilesBtn.isChecked = false
        binding.nextBtn.isVisible = true
    }

    override fun onClick(v: View?) {
    }

}


