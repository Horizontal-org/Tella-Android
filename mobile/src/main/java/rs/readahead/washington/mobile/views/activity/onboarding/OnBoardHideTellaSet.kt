package rs.readahead.washington.mobile.views.activity.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class OnBoardHideTellaSet : BaseFragment() {

    private lateinit var nextBtn: TextView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.onboard_hide_set_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView(view)
    }

    override fun initView(view: View) {
        (activity as OnBoardActivityInterface).showProgress()
        (activity as OnBoardActivityInterface).setCurrentIndicator(1)

        nextBtn = view.findViewById(R.id.next_btn)
        nextBtn.setOnClickListener {
            activity.addFragment(
                this,
                OnBoardAdvancedComplete(),
                R.id.rootOnboard
            )
        }
    }
}