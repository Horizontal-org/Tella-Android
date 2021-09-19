package rs.readahead.washington.mobile.views.activity.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class OnBoardHideTellaFragment: BaseFragment() {

    private lateinit var hideBtn: TextView
    private lateinit var defaultBtn: View
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.onboard_hide_tella_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView(view)
    }

    override fun initView(view: View) {
        (activity as OnBoardActivityInterface).setCurrentIndicator(1)

        hideBtn = view.findViewById(R.id.startBtn)
        hideBtn.setOnClickListener {

            //activity.onBackPressed()
        }

        defaultBtn = view.findViewById(R.id.sheet_two_btn)
        defaultBtn.setOnClickListener {
            activity.addFragment(
                this,
                OnBoardAdvancedComplete (),
                R.id.rootOnboard
            )
        }
    }
}