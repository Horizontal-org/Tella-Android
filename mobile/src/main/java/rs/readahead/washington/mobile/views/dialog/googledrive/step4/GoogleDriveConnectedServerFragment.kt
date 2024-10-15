package rs.readahead.washington.mobile.views.dialog.googledrive.step4

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import rs.readahead.washington.mobile.databinding.GoogleDriveConnectedServerFragmentBinding
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER
import rs.readahead.washington.mobile.views.dialog.SharedLiveData.createGoogleDriveServer
import rs.readahead.washington.mobile.views.dialog.SharedLiveData.updateGoogleDriveServer
import rs.readahead.washington.mobile.views.dialog.googledrive.setp0.OBJECT_KEY

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
        // Use requireArguments to avoid nullable arguments handling
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
        if (isUpdate) {
            updateGoogleDriveServer.postValue(googleDriveServer)
        } else {
            createGoogleDriveServer.postValue(googleDriveServer)
        }
    }
}
