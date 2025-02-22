package org.horizontal.tella.mobile.views.fragment.reports.viewpagerfragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.util.jobs.WorkerUploadReport
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.MainReportFragment
import org.horizontal.tella.mobile.views.fragment.reports.ReportsViewModel

class ReportTabsFragment : MainReportFragment() {

    override val viewModel by activityViewModels<ReportsViewModel>()

    override fun getFragmentProvider(): FragmentProvider {
        return ReportsFragmentProvider()
    }

    override fun getToolbarTitle(): String {
        return getString(R.string.Home_BottomNav_Reports)
    }

    override fun navigateToNewReportScreen() {
        this.navManager().navigateFromReportsScreenToNewReportScreen()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scheduleWorker()
    }

    override fun getEmptyMessageIcon(): Int {
        return R.drawable.ic_reports
    }

    private fun scheduleWorker() {
        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        val onetimeJob = OneTimeWorkRequest.Builder(WorkerUploadReport::class.java)
            .setConstraints(constraints).build()
        WorkManager.getInstance(baseActivity)
            .enqueueUniqueWork("WorkerUploadReport", ExistingWorkPolicy.KEEP, onetimeJob)
    }
}