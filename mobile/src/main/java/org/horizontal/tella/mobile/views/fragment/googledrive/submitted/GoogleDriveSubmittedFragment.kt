package org.horizontal.tella.mobile.views.fragment.googledrive.submitted

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.views.fragment.googledrive.GoogleDriveViewModel
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportSubmittedFragment

@AndroidEntryPoint
class GoogleDriveSubmittedFragment : BaseReportSubmittedFragment() {

    override val viewModel by viewModels<GoogleDriveViewModel>()
}

