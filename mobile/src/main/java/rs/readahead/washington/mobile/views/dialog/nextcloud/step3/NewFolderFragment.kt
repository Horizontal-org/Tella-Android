package rs.readahead.washington.mobile.views.dialog.nextcloud.step3

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.KeyboardUtil
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.NewFolderFragmentBinding
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.googledrive.setp0.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.nextcloud.INextCloudAuthFlow
import rs.readahead.washington.mobile.views.dialog.nextcloud.NextCloudLoginFlowViewModel

@AndroidEntryPoint
class NewFolderFragment : BaseBindingFragment<NewFolderFragmentBinding>(
    NewFolderFragmentBinding::inflate
) {
    private val viewModel: NextCloudLoginFlowViewModel by activityViewModels()
    private lateinit var serverNextCloud: NextCloudServer
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObservers()
    }

    private fun initView() {
        arguments?.getString(OBJECT_KEY)?.let {
            serverNextCloud = Gson().fromJson(it, NextCloudServer::class.java)
        }
        binding.nextBtn.setOnClickListener {
            createFolder()
        }

        binding.backBtn.setOnClickListener {
            baseActivity.onBackPressed()
        }

        KeyboardUtil(binding.root)
    }

    private fun createFolder() {
        val folderName = binding.createFolderEdit.text.toString()
        serverNextCloud.folderName = folderName
        if (folderName.isNotEmpty()) {
            createFolder(serverNextCloud.folderName)
        } else {
            binding.createFolderLayout.error = getString(R.string.Folder_Empty_Error)
        }
    }

    private fun createFolder(folderName: String) {
        viewModel.progress.postValue(true)
        (activity as? INextCloudAuthFlow)?.onStartCreateRemoteFolder(
            folderName = folderName
        )
    }

    private fun initObservers() {
        viewModel.errorFolderCreation.observe(viewLifecycleOwner) { message ->
            showToast(message)
        }

        viewModel.successFolderCreation.observe(viewLifecycleOwner) { server ->
            bundle.putString(OBJECT_KEY, Gson().toJson(serverNextCloud))
            navManager().actionNextCloudNewFolderScreenToSuccessfulScreen()
        }
    }
}