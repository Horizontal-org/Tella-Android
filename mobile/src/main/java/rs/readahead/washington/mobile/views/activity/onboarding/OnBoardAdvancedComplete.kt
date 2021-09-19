package rs.readahead.washington.mobile.views.activity.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.hzontal.tella_locking_ui.TellaKeysUI
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class OnBoardAdvancedComplete : BaseFragment() {

    private lateinit var startBtn: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.onboard_advanced_complete, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView(view)
    }

    override fun initView(view: View) {
        (activity as OnBoardActivityInterface).hideProgress()

        startBtn = view.findViewById(R.id.startBtn)
        startBtn.setOnClickListener {
            TellaKeysUI.getCredentialsCallback().onLockConfirmed(requireContext())
        }
    }
}