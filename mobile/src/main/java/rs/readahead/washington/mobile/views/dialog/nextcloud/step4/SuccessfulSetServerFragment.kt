package rs.readahead.washington.mobile.views.dialog.nextcloud.step4

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.databinding.FragmentSuccessfulSetServerBinding
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY

@AndroidEntryPoint
class SuccessfulSetServerFragment :
    BaseBindingFragment<FragmentSuccessfulSetServerBinding>(
        FragmentSuccessfulSetServerBinding::inflate
    ) {
    private lateinit var server: NextCloudServer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListeners()
    }

    private fun initView() {

        if (arguments == null) return

        arguments?.getString(OBJECT_KEY)?.let {
            server = Gson().fromJson(it, NextCloudServer::class.java)
        }
    }

    private fun initListeners() {
        binding.okBtn.setOnClickListener {
            save(server)
        }

    }

    private fun save(server: NextCloudServer) {
        baseActivity.finish()
    }

}