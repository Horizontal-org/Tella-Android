package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.databinding.FragmentUploadFilesBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.FileTransferViewModel
import org.horizontal.tella.mobile.views.fragment.uwazi.widgets.PeerToPeerEndView

class SenderUploadFilesFragment :
    BaseBindingFragment<FragmentUploadFilesBinding>(FragmentUploadFilesBinding::inflate) {

    private val viewModel: FileTransferViewModel by activityViewModels()
    private lateinit var endView: PeerToPeerEndView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showFormEndView()
        observeUploadProgress()
        viewModel.uploadAllFiles()
    }

    private fun showFormEndView() {
        val session = viewModel.p2PSharedState.session ?: return
        val files = session.files.values.toList()

        endView = PeerToPeerEndView(
            baseActivity, session.title ?: "Transfer"
        )
        endView.setFiles(files, MyApplication.isConnectedToInternet(baseActivity), false)
        binding.endViewContainer.removeAllViews()
        binding.endViewContainer.addView(endView)
        endView.clearPartsProgress(files, session.status)
    }

    private fun observeUploadProgress() {
        viewModel.uploadProgress.observe(viewLifecycleOwner) { percent ->
            viewModel.p2PSharedState.session?.let { session ->
                endView.setUploadProgress(session.files.values.toList(), percent.toFloat())
            }
        }
    }
}
