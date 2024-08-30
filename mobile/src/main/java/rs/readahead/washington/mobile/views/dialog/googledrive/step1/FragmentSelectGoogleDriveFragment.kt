package rs.readahead.washington.mobile.views.dialog.googledrive.step1

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSelectGoogleDriveBinding
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY

class FragmentSelectGoogleDriveFragment :
    BaseBindingFragment<FragmentSelectGoogleDriveBinding>(FragmentSelectGoogleDriveBinding::inflate),
    View.OnClickListener {
    private var server: TellaReportServer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
     //   initListeners()
    }

    private fun initView() {

        if (arguments == null) return

        arguments?.getString(OBJECT_KEY)?.let {
            server = Gson().fromJson(it, TellaReportServer::class.java)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.yes_btn -> {

            }

            R.id.no_btn -> {

            }

            R.id.back_btn -> {
                baseActivity.onBackPressed()
            }

            R.id.next_btn -> {
                validateAndLogin()
            }
        }
    }

    private fun validateAndLogin() {
        if (server == null) return
        if (binding?.yesBtn?.isChecked == true) {

        } else {

        }
    }
}

