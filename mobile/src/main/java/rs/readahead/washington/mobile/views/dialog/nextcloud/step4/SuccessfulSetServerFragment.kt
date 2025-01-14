package rs.readahead.washington.mobile.views.dialog.nextcloud.step4

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.databinding.FragmentSuccessfulSetServerBinding
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.SharedLiveData.createNextCloudServer

@AndroidEntryPoint
class SuccessfulSetServerFragment :
    BaseBindingFragment<FragmentSuccessfulSetServerBinding>(FragmentSuccessfulSetServerBinding::inflate) {

    private lateinit var nextCloudServer: NextCloudServer
    private var isUpdate: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        retrieveArguments()
        setupListeners()
    }

    private fun retrieveArguments() {
        arguments?.let { args ->
            nextCloudServer = Gson().fromJson(args.getString(OBJECT_KEY), NextCloudServer::class.java)
            isUpdate = args.getBoolean(IS_UPDATE_SERVER, false)
        } ?: run {
            baseActivity.finish()
        }
    }

    private fun setupListeners() {
        binding.okBtn.setOnClickListener {
            applyServerUpdate()
            baseActivity.finish()
        }
    }

    private fun applyServerUpdate() {
        nextCloudServer.name = "NextCloud"
        createNextCloudServer.postValue(nextCloudServer)
    }
}
