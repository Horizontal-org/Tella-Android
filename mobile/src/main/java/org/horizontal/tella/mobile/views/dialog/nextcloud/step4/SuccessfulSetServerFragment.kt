package org.horizontal.tella.mobile.views.dialog.nextcloud.step4

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.databinding.FragmentSuccessfulSetServerBinding
import org.horizontal.tella.mobile.domain.entity.nextcloud.NextCloudServer
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.dialog.IS_UPDATE_SERVER
import org.horizontal.tella.mobile.views.dialog.OBJECT_KEY
import org.horizontal.tella.mobile.views.dialog.SharedLiveData.createNextCloudServer

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
