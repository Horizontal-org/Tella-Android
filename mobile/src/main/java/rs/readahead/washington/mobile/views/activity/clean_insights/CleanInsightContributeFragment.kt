package rs.readahead.washington.mobile.views.activity.clean_insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.TextView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class CleanInsightContributeFragment : BaseFragment() {

    private lateinit var onNext: (Boolean) -> Unit
    private lateinit var onPrevious: () -> Unit
    private lateinit var learnMore: () -> Unit
    private var fromLearnMore: Boolean = false

    companion object {
        @JvmStatic
        fun newInstance(
            onNext: (Boolean) -> Unit,
            onPrevious: () -> Unit,
            learnMore: () -> Unit,
            fromLearnMore: Boolean = false
        ) =
            CleanInsightContributeFragment().apply {
                this.onNext = onNext
                this.onPrevious = onPrevious
                this.learnMore = learnMore
                this.fromLearnMore = fromLearnMore
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_clean_insights_contribute, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun initView(view: View) {
        view.findViewById<TextView>(R.id.back_btn).setOnClickListener { onPrevious() }
        view.findViewById<TextView>(R.id.btn_yes).setOnClickListener { onNext(true) }
        view.findViewById<TextView>(R.id.btn_no).setOnClickListener { onNext(false) }
        val tvLearnMore = view.findViewById<TextView>(R.id.btn_learn_more)
        tvLearnMore.setOnClickListener { learnMore() }
        if (fromLearnMore) tvLearnMore.visibility = GONE
    }
}