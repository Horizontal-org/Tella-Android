package rs.readahead.washington.mobile.views.dialog.reports.step6

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import rs.readahead.washington.mobile.databinding.FragmentSuccessfulSetServerBinding
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.ID_KEY
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.SharedLiveData

class SuccessfulSetServerFragment :
    BaseBindingFragment<FragmentSuccessfulSetServerBinding>(
        FragmentSuccessfulSetServerBinding::inflate
    ) {
    private lateinit var server: TellaReportServer


    companion object {
        val TAG: String = SuccessfulSetServerFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            server: TellaReportServer
        ): SuccessfulSetServerFragment {
            val frag = SuccessfulSetServerFragment()
            val args = Bundle()
            args.putSerializable(ID_KEY, server.id)
            args.putString(OBJECT_KEY, Gson().toJson(server))
            frag.arguments = args
            return frag
        }
    }

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
        binding?.okBtn?.setOnClickListener {
            save(server)
        }

    }

    private fun save(server: TellaReportServer) {
        SharedLiveData.createReportsServer.postValue(server)
        baseActivity.finish()
    }

}