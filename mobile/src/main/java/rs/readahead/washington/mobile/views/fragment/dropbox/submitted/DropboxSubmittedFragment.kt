package rs.readahead.washington.mobile.views.fragment.dropbox.submitted

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.views.fragment.dropbox.DropBoxViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportSubmittedFragment

@AndroidEntryPoint
class DropboxSubmittedFragment : BaseReportSubmittedFragment() {

    override val viewModel by viewModels<DropBoxViewModel>()
}

