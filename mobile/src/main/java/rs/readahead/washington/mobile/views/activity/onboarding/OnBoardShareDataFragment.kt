package rs.readahead.washington.mobile.views.activity.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class OnBoardShareDataFragment : BaseFragment() {

    private lateinit var connectBtn: TextView
    private lateinit var continueBtn: TextView
    private lateinit var backBtn: TextView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.onboard_share_data_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView(view)
    }

    override fun initView(view: View) {
        (activity as OnBoardActivityInterface).initProgress(2)
        (activity as OnBoardActivityInterface).setCurrentIndicator(0)

        connectBtn = view.findViewById(R.id.startBtn)
        connectBtn.setOnClickListener {
            (activity as OnBoardActivityInterface).showChooseServerTypeDialog()
        }

        continueBtn = view.findViewById(R.id.sheet_two_btn)
        continueBtn.setOnClickListener {
            activity.addFragment(
                this,
                OnBoardHideOptionFragment(),
                R.id.rootOnboard
            )
        }

        backBtn = view.findViewById(R.id.back_btn)
        backBtn.setOnClickListener {
back()        }
    }
}