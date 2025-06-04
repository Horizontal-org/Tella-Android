package org.horizontal.tella.mobile.views.fragment.peertopeer

import android.os.Bundle
import android.view.View
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentWaitingBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

/**
 * Created by wafa on 3/6/2025.
 */
class WaitingFragment : BaseBindingFragment<FragmentWaitingBinding>(FragmentWaitingBinding::inflate){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val isSender = arguments?.getBoolean("isSender") ?: false

        if (!isSender) {
            binding.toolbar.setToolbarTitle(getString(R.string.receive_files))
            binding.waitingText.text = getString(R.string.waiting_for_the_sender_to_share_files)
        } else {
            binding.toolbar.setToolbarTitle(getString(R.string.send_files))
            binding.waitingText.text = "Waiting for the recipient to accept files"
        }
    }

}