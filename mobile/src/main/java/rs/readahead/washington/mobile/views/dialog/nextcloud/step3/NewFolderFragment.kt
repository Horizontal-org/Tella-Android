package rs.readahead.washington.mobile.views.dialog.nextcloud.step3

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.NewFolderFragmentBinding
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.nextcloud.INextCloudAuthFlow
import rs.readahead.washington.mobile.views.dialog.nextcloud.NextCloudLoginFlowViewModel

@AndroidEntryPoint
class NewFolderFragment : BaseBindingFragment<NewFolderFragmentBinding>(
    NewFolderFragmentBinding::inflate
) {
    private val viewModel: NextCloudLoginFlowViewModel by viewModels()
    private lateinit var serverNextCloud: NextCloudServer
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        binding.nextBtn.setOnClickListener {
            createFolder()
        }

        binding.backBtn.setOnClickListener {
            baseActivity.onBackPressed()
        }
    }

    private fun createFolder() {
        val folderName = binding.folderName.text.toString()
        serverNextCloud.folderName = folderName
        if (folderName.isNotEmpty()) {
            createFolder(serverNextCloud.folderName)
        } else {
            binding.folderLayout.error = getString(R.string.Folder_Empty_Error)
        }
    }

    private fun createFolder(folderName: String) {
        viewModel.progress.postValue(true)
        (activity as? INextCloudAuthFlow)?.onStartCreateRemoteFolder(
            folderName = folderName
        )
    }
}