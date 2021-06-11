package rs.readahead.washington.mobile.views.activity.onboarding

import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.hzontal.shared_ui.buttons.InformationButton
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class OnBoardSetUpFragment : BaseFragment() {

    private lateinit var fullSetupBtn: InformationButton
    private lateinit var quickSetupBtn: InformationButton
    private lateinit var linkTv : TextView
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.onboard_setup_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    private fun initView(view: View) {
        fullSetupBtn = view.findViewById(R.id.fullSetupBtn)
        quickSetupBtn = view.findViewById(R.id.quickSetupBtn)
        linkTv = view.findViewById(R.id.link)
        val content = SpannableString(getString(R.string.onboard_setup_customization))
        content.setSpan(UnderlineSpan(), 0, content.length, 0)
        linkTv.text = content
        initListeners()
    }

    private fun initListeners() {
        fullSetupBtn.setOnClickListener { activity.addFragment(this,OnBoardLockFragment.newInstance(false), R.id.rootOnboard) }
        quickSetupBtn.setOnClickListener { activity.addFragment(this,OnBoardLockFragment(), R.id.rootOnboard) }
    }
}