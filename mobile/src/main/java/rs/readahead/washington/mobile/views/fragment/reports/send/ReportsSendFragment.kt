package rs.readahead.washington.mobile.views.fragment.reports.send

import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.databinding.FragmentSendReportBinding
import rs.readahead.washington.mobile.domain.entity.reports.ReportFormInstance
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.reports.entry.BUNDLE_REPORT_FORM_INSTANCE
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.ReportsFormEndView

@AndroidEntryPoint
class ReportsSendFragment :
    BaseBindingFragment<FragmentSendReportBinding>(FragmentSendReportBinding::inflate) {

    private lateinit var endView: ReportsFormEndView
    private var reportInstance: ReportFormInstance? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initData() {
    }

    private fun initView() {
        arguments?.let {
            reportInstance = it.get(BUNDLE_REPORT_FORM_INSTANCE) as ReportFormInstance
            showFormEndView()
        }

        with(binding) {
            this?.toolbar?.backClickListener = { nav().popBackStack() }

            this?.nextBtn?.setOnClickListener {
                reportInstance?.let {
                    submitEntity()
                }
            }

        }
    }

    private fun showFormEndView() {
        if (reportInstance == null) {
            return
        }

        reportInstance?.let { reportFormInstance ->
            endView = ReportsFormEndView(
                activity,
                reportFormInstance.title,
                reportFormInstance.description
            )
            endView.setInstance(reportFormInstance, false, false)
            binding?.endViewContainer?.removeAllViews()
            binding?.endViewContainer?.addView(endView)
        }
    }

    private fun submitEntity() {
        reportInstance?.let { entity ->

        }
    }


}