package rs.readahead.washington.mobile.views.activity.clean_insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class CleanInsightHowWorksFragment : BaseFragment() {

    private lateinit var onNext: () -> Unit
    private lateinit var onPrevious: () -> Unit

    companion object {
        @JvmStatic
        fun newInstance(onNext: () -> Unit, onPrevious: () -> Unit) =
            CleanInsightHowWorksFragment().apply {
                this.onNext = onNext
                this.onPrevious = onPrevious
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_clean_insights_how_works, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun initView(view: View) {
        view.findViewById<TextView>(R.id.back_btn).setOnClickListener { onPrevious() }
        view.findViewById<TextView>(R.id.next_btn).setOnClickListener { onNext() }
    }
}