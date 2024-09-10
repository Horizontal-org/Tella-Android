package rs.readahead.washington.mobile.views.dialog.googledrive.step4

import SharedGoogleDriveViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.fragment.app.activityViewModels
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class GoogleDriveConnectedServerFragment : BaseFragment() {
    private val sharedViewModel: SharedGoogleDriveViewModel by activityViewModels()
    private lateinit var credentialManager: CredentialManager
    private lateinit var request: GetCredentialRequest
    private lateinit var nextBtn: TextView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.google_drive_connected_server_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun onResume() {
        super.onResume()
//        (baseActivity as OnBoardActivityInterface).enableSwipe(
//            isSwipeable = true, isTabLayoutVisible = true
//        )
//        (baseActivity as OnBoardActivityInterface).showButtons(
//            isNextButtonVisible = true, isBackButtonVisible = true
//        )
    }

    override fun initView(view: View) {
//        (baseActivity as OnBoardActivityInterface).setCurrentIndicator(0)
//
//        nextBtn = view.findViewById(R.id.next_btn)
//        nextBtn.setOnClickListener {
//            baseActivity.addFragment(
//                this,
//                OnBoardHideOptionFragment(),
//                R.id.rootOnboard
//            )
//        }
    }
}