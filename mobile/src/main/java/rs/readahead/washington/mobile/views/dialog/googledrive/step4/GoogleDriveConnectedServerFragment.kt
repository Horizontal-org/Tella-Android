package rs.readahead.washington.mobile.views.dialog.googledrive.step4

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import rs.readahead.washington.mobile.databinding.GoogleDriveConnectedServerFragmentBinding
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.googledrive.setp0.OBJECT_KEY

class GoogleDriveConnectedServerFragment :
    BaseBindingFragment<GoogleDriveConnectedServerFragmentBinding>(
        GoogleDriveConnectedServerFragmentBinding::inflate
    ) {
    private lateinit var googleDriveServer: GoogleDriveServer // for the update

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments == null) return

        arguments?.getString(OBJECT_KEY)?.let {
            googleDriveServer = Gson().fromJson(it, GoogleDriveServer::class.java)
        }
    }
}
