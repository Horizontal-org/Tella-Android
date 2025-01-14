package org.horizontal.tella.mobile.views.fragment.nextCloud.submitted

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportSubmittedFragment
import org.horizontal.tella.mobile.views.fragment.nextCloud.NextCloudViewModel

@AndroidEntryPoint
class NextCloudSubmittedFragment : BaseReportSubmittedFragment() {

    override val viewModel by viewModels<NextCloudViewModel>()
}

