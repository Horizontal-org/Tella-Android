package rs.readahead.washington.mobile.views.dialog.dropbox.connected

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.GoogleDriveConnectedServerFragmentBinding
import rs.readahead.washington.mobile.domain.entity.dropbox.DropBoxServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.SharedLiveData.createDropBoxServer

class DropBoxConnectedServerFragment :
    BaseBindingFragment<GoogleDriveConnectedServerFragmentBinding>(
        GoogleDriveConnectedServerFragmentBinding::inflate
    ) {
    private var dropBoxServer: DropBoxServer? = null

    companion object {
        val TAG = DropBoxConnectedServerFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            dropBoxServer: DropBoxServer,
        ): DropBoxConnectedServerFragment {
            val frag = DropBoxConnectedServerFragment()
            val args = Bundle()
            args.putString(OBJECT_KEY, Gson().toJson(dropBoxServer))
            frag.arguments = args
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
        arguments?.getString(OBJECT_KEY)?.let {
            dropBoxServer = Gson().fromJson(it, DropBoxServer::class.java)
        }
    }

    private fun initListeners() {
        binding.goToBtn.setOnClickListener {
            handleServerUpdate()
            baseActivity.finish()
        }
    }

    private fun handleServerUpdate() {
        createDropBoxServer.postValue(dropBoxServer)
    }
}
