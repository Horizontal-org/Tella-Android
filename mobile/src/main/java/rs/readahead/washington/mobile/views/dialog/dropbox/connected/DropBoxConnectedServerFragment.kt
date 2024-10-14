package rs.readahead.washington.mobile.views.dialog.dropbox.connected

import android.os.Bundle
import android.view.View
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.GoogleDriveConnectedServerFragmentBinding
import rs.readahead.washington.mobile.domain.entity.dropbox.DropBoxServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment

class DropBoxConnectedServerFragment :
    BaseBindingFragment<GoogleDriveConnectedServerFragmentBinding>(
        GoogleDriveConnectedServerFragmentBinding::inflate
    ) {
    private lateinit var dropBoxServer: DropBoxServer

    companion object {
        val TAG = DropBoxConnectedServerFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            dropBoxServer: DropBoxServer,
            isUpdate: Boolean
        ): DropBoxConnectedServerFragment {
            val frag = DropBoxConnectedServerFragment()
            val args = Bundle()
            return frag
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpView()
        setupData()
        initListeners()
    }

    private fun setUpView() {
        binding.goToBtn.setText(getString(R.string.go_to_dropbox))
    }

    private fun setupData() {
        // Use requireArguments to avoid nullable arguments handling
        //  dropBoxServer =
        //    Gson().fromJson(requireArguments().getString(OBJECT_KEY), GoogleDriveServer::class.java)

        //  isUpdate = requireArguments().getBoolean(IS_UPDATE_SERVER, false)
    }

    private fun initListeners() {
        binding.goToBtn.setOnClickListener {
            handleServerUpdate()
            baseActivity.finish()
        }
    }

    private fun handleServerUpdate() {
        //   if (isUpdate) {
        //     updateGoogleDriveServer.postValue(googleDriveServer)
        //  } else {
        //  createGoogleDriveServer.postValue(googleDriveServer)
        // }
    }
}
