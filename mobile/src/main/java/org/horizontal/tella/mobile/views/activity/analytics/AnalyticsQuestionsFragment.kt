package org.horizontal.tella.mobile.views.activity.analytics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.base_ui.BaseFragment

class AnalyticsQuestionsFragment : BaseFragment() {

    private lateinit var onNext: () -> Unit
    private lateinit var onPrevious: () -> Unit

    companion object {
        @JvmStatic
        fun newInstance(onNext: () -> Unit, onPrevious: () -> Unit) =
            AnalyticsQuestionsFragment().apply {
                this.onNext = onNext
                this.onPrevious = onPrevious
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_analytics_questions, container, false)
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