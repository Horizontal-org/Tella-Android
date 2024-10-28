package rs.readahead.washington.mobile.views.dialog.nextcloud.step3

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.databinding.NewFolderFragmentBinding
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.nextcloud.NextCloudLoginFlowViewModel

@AndroidEntryPoint
class NewFolderFragment : BaseBindingFragment<NewFolderFragmentBinding>(
    NewFolderFragmentBinding::inflate
) {
    private val viewModel: NextCloudLoginFlowViewModel by viewModels()
    private val serverNextCloud: NextCloudServer by lazy {
        NextCloudServer()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}