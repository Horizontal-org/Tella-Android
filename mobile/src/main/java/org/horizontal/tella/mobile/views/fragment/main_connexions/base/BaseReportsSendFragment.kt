package org.horizontal.tella.mobile.views.fragment.main_connexions.base

import android.os.Bundle
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.views.fragment.uwazi.widgets.ReportsFormEndView

abstract class BaseReportsSendFragment : BaseEntitySendFragment<ReportInstance>() {

    protected lateinit var endView: ReportsFormEndView

    override fun readInstanceFromArguments(arguments: Bundle): ReportInstance? =
        arguments.getSerializable(BUNDLE_REPORT_FORM_INSTANCE) as ReportInstance?

    override fun showFormEndView() {
        val reportFormInstance = reportInstance ?: return

        endView = ReportsFormEndView(
            activity,
            reportFormInstance.title,
            reportFormInstance.description,
        )
        endView.setInstance(
            reportFormInstance, MyApplication.isConnectedToInternet(baseActivity), false
        )
        binding.endViewContainer.removeAllViews()
        binding.endViewContainer.addView(endView)
        endView.clearPartsProgress(reportFormInstance)
    }
}
