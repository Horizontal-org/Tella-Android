package rs.readahead.washington.mobile.views.activity.analytics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class AnalyticsShareDataFragment : BaseFragment() {

    private lateinit var onNext: () -> Unit

    companion object {
        @JvmStatic
        fun newInstance(onNext: () -> Unit) =
            AnalyticsShareDataFragment().apply { this.onNext = onNext }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_analytics_share_data, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun initView(view: View) {
        view.findViewById<TextView>(R.id.next_btn).setOnClickListener { onNext() }
    }
}