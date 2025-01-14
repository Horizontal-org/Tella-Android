package rs.readahead.washington.mobile.views.fragment.nextCloud.submitted

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportSubmittedFragment
import rs.readahead.washington.mobile.views.fragment.nextCloud.NextCloudViewModel

@AndroidEntryPoint
class NextCloudSubmittedFragment : BaseReportSubmittedFragment() {

    override val viewModel by viewModels<NextCloudViewModel>()
}

