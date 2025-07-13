package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.databinding.FragmentUploadFilesBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.FileTransferViewModel
import org.horizontal.tella.mobile.views.fragment.uwazi.widgets.PeerToPeerEndView

class RecipientUploadFilesFragment:
    BaseBindingFragment<FragmentUploadFilesBinding>(FragmentUploadFilesBinding::inflate) {

    private val viewModel: FileTransferViewModel by activityViewModels()
    private lateinit var endView: PeerToPeerEndView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showFormEndView()
    }

    private fun showFormEndView() {
        if (viewModel.peerToPeerInstance == null) {
            return
        }

        viewModel.peerToPeerInstance?.let { peerInstance ->

            endView = PeerToPeerEndView(
                baseActivity,
                peerInstance.title,
            )
            endView.setInstance(
                peerInstance, MyApplication.isConnectedToInternet(baseActivity), false
            )
            binding.endViewContainer.removeAllViews()
            binding.endViewContainer.addView(endView)
            endView.clearPartsProgress(peerInstance)
        }
    }

}