package org.horizontal.tella.mobile.views.dialog.reports.step6

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import org.horizontal.tella.mobile.databinding.FragmentSuccessfulSetServerBinding
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.dialog.OBJECT_KEY
import org.horizontal.tella.mobile.views.dialog.SharedLiveData

class SuccessfulSetServerFragment :
    BaseBindingFragment<FragmentSuccessfulSetServerBinding>(
        FragmentSuccessfulSetServerBinding::inflate
    ) {
    private lateinit var server: TellaReportServer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListeners()
    }

    private fun initView() {

        if (arguments == null) return

        arguments?.getString(OBJECT_KEY)?.let {
            server = Gson().fromJson(it, TellaReportServer::class.java)
        }
    }

    private fun initListeners() {
        binding.okBtn.setOnClickListener {
            save(server)
        }

    }

    private fun save(server: TellaReportServer) {
        SharedLiveData.createReportsServer.postValue(server)
        baseActivity.finish()
    }

}