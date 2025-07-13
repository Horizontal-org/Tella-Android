package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState
import org.horizontal.tella.mobile.databinding.FragmentUploadFilesBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.FileTransferViewModel
import org.horizontal.tella.mobile.views.fragment.uwazi.widgets.PeerToPeerEndView
import javax.inject.Inject

@AndroidEntryPoint
class RecipientUploadFilesFragment :
    BaseBindingFragment<FragmentUploadFilesBinding>(FragmentUploadFilesBinding::inflate) {

    private val viewModel: FileTransferViewModel by activityViewModels()

    @Inject
    lateinit var p2PSharedState: P2PSharedState

    private lateinit var endView: PeerToPeerEndView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showFormEndView()
        observeUploadProgress()
    }

    private fun showFormEndView() {
        val session = p2PSharedState.session ?: return
        val files = session.files.values.toList()

        endView = PeerToPeerEndView(
            baseActivity,
            session.title
        )

        endView.setFiles(files, MyApplication.isConnectedToInternet(baseActivity), false)

        binding.endViewContainer.removeAllViews()
        binding.endViewContainer.addView(endView)
    }

    private fun observeUploadProgress() {

    }
}
