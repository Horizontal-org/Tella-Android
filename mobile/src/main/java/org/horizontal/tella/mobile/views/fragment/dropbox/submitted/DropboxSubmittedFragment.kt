package org.horizontal.tella.mobile.views.fragment.dropbox.submitted

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.views.fragment.dropbox.DropBoxViewModel
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportSubmittedFragment

@AndroidEntryPoint
class DropboxSubmittedFragment : BaseReportSubmittedFragment() {

    override val viewModel by viewModels<DropBoxViewModel>()
}

