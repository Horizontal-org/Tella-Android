package org.horizontal.tella.mobile.views.dialog.googledrive.step4

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import org.horizontal.tella.mobile.databinding.GoogleDriveConnectedServerFragmentBinding
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.dialog.IS_UPDATE_SERVER
import org.horizontal.tella.mobile.views.dialog.SharedLiveData.createGoogleDriveServer
import org.horizontal.tella.mobile.views.dialog.SharedLiveData.updateGoogleDriveServer
import org.horizontal.tella.mobile.views.dialog.googledrive.GoogleDriveConnectFlowActivity
import org.horizontal.tella.mobile.views.dialog.googledrive.setp0.OBJECT_KEY

class GoogleDriveConnectedServerFragment :
    BaseBindingFragment<GoogleDriveConnectedServerFragmentBinding>(
        GoogleDriveConnectedServerFragmentBinding::inflate
    ) {
    private lateinit var googleDriveServer: GoogleDriveServer
    private var isUpdate = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupData()
        initListeners()
    }

    private fun setupData() {
        googleDriveServer =
            Gson().fromJson(requireArguments().getString(OBJECT_KEY), GoogleDriveServer::class.java)
        isUpdate = requireArguments().getBoolean(IS_UPDATE_SERVER, false)
    }

    private fun initListeners() {
        binding.goToGoogleDriveBtn.setOnClickListener {
            handleServerUpdate()
            baseActivity.finish()
        }
    }

    private fun handleServerUpdate() {
        val activity = baseActivity
        val isMigration = activity is GoogleDriveConnectFlowActivity &&
            activity.intent.hasExtra(GoogleDriveConnectFlowActivity.EXTRA_MIGRATION_SERVER)
        if (isMigration && isUpdate) {
            val result = Intent().apply {
                putExtra(GoogleDriveConnectFlowActivity.EXTRA_RESULT_SERVER_ID, googleDriveServer.id)
                putExtra(GoogleDriveConnectFlowActivity.EXTRA_RESULT_FOLDER_ID, googleDriveServer.folderId)
                putExtra(GoogleDriveConnectFlowActivity.EXTRA_RESULT_FOLDER_NAME, googleDriveServer.folderName)
            }
            activity.setResult(Activity.RESULT_OK, result)
        } else if (isUpdate) {
            updateGoogleDriveServer.postValue(googleDriveServer)
        } else {
            createGoogleDriveServer.postValue(googleDriveServer)
        }
    }
}
