package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerServerStarterManager
import org.horizontal.tella.mobile.data.peertopeer.model.P2PFileStatus
import org.horizontal.tella.mobile.data.peertopeer.model.SessionStatus
import org.horizontal.tella.mobile.databinding.FragmentUploadFilesBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow.PeerToPeerParticipant
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import org.horizontal.tella.mobile.views.fragment.uwazi.widgets.PeerToPeerEndView
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showProgressImportSheet
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showStandardSheet
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class RecipientUploadFilesFragment :
    BaseBindingFragment<FragmentUploadFilesBinding>(FragmentUploadFilesBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private lateinit var endView: PeerToPeerEndView
    private val progressPercentLiveData = MutableLiveData<Int>()
    private var sheetShown = false

    @Inject
    lateinit var peerServerStarterManager: PeerServerStarterManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUI()
        setupCancelButton()
    }

    private fun initializeUI() {
        showFormEndView()
        observeUploadProgress()
        observeBottomSheetProgress()
        viewModel.peerToPeerParticipant = PeerToPeerParticipant.RECIPIENT
    }

    private fun setupCancelButton() {
        binding.cancel.setOnClickListener {
            showStopSharingConfirmation()
        }
    }

    private fun showStopSharingConfirmation() {
        showStandardSheet(baseActivity.supportFragmentManager,
            getString(R.string.stop_sharing_files),
            getString(R.string.nearby_sharing_will_be_stopped_the_recipient_will_not_have_access_to_files_that_were_not_fully_transferred),
            getString(R.string.action_continue).uppercase(),
            getString(R.string.stop).uppercase(),
            onConfirmClick = {},
            onCancelClick = {
                stopServerAndNavigate()
            })
    }

    private fun stopServerAndNavigate() {
        peerServerStarterManager.stopServer()
        viewModel.peerToPeerParticipant = PeerToPeerParticipant.RECIPIENT
        navManager().navigateFromRecipientUploadFilesFragmentToPeerToPeerResultFragment()
    }

    private fun showFormEndView() {
        val session = viewModel.p2PState.session ?: return
        val files = session.files.values.toList()

        endView = PeerToPeerEndView(
            baseActivity, session.title
        )

        endView.setFiles(files, MyApplication.isConnectedToInternet(baseActivity), false)

        binding.endViewContainer.removeAllViews()
        binding.endViewContainer.addView(endView)
    }

    private fun observeUploadProgress() {
        viewModel.uploadProgress.observe(viewLifecycleOwner) { state ->
            if (state == null) return@observe

            val files = state.files
            val percent = state.percent.coerceIn(0, 100)
            val percentFloat = percent / 100f

            endView.setUploadProgress(files, percentFloat)

            // Only show the bottom sheet once, updates come from observeBottomSheetProgress
            if (!sheetShown) {
                sheetShown = true
                showProgressImportSheet(
                    baseActivity.supportFragmentManager,
                    getString(R.string.Vault_Importing_SheetTitle),
                    files.size,
                    resources.getQuantityString(
                        R.plurals.Vault_Importing_SheetProgress, files.size
                    ),
                    progressStatus = progressPercentLiveData,
                    getString(R.string.action_cancel).uppercase(),
                    viewLifecycleOwner
                ) {

                }
            }

            val allSaved = files.all { it.status == P2PFileStatus.SAVED }
            if (state.sessionStatus == SessionStatus.FINISHED && allSaved) {
                stopServerAndNavigate()
            }
        }
    }

    private fun observeBottomSheetProgress() {
        viewModel.bottomSheetProgress.observe(viewLifecycleOwner) { progress ->
            if (!sheetShown) return@observe
            progressPercentLiveData.postValue(progress.current)
        }
    }


}
