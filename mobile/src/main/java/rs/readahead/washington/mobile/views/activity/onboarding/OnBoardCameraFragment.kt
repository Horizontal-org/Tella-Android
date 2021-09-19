package rs.readahead.washington.mobile.views.activity.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class OnBoardCameraFragment : BaseFragment() {

    private lateinit var backBtn: TextView
    private lateinit var nextBtn: TextView
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.onboard_camera_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView(view)
    }

    override fun initView(view: View) {
        (activity as OnBoardActivityInterface).showProgress()
        (activity as OnBoardActivityInterface).initProgress(5)
        (activity as OnBoardActivityInterface).setCurrentIndicator(0)

        backBtn = view.findViewById(R.id.back_btn)
        backBtn.setOnClickListener {
            activity.onBackPressed()
        }

        nextBtn = view.findViewById(R.id.next_btn)
        nextBtn.setOnClickListener {
            activity.addFragment(
                    this,
                    OnBoardRecorderFragment(),
                    R.id.rootOnboard
            )
        }
    }
}