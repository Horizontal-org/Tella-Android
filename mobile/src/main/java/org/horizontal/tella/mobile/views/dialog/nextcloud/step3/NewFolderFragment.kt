package org.horizontal.tella.mobile.views.dialog.nextcloud.step3

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.KeyboardUtil
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.NewFolderFragmentBinding
import org.horizontal.tella.mobile.domain.entity.nextcloud.NextCloudServer
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.dialog.googledrive.setp0.OBJECT_KEY
import org.horizontal.tella.mobile.views.dialog.nextcloud.INextCloudAuthFlow
import org.horizontal.tella.mobile.views.dialog.nextcloud.NextCloudLoginFlowViewModel

@AndroidEntryPoint
class NewFolderFragment : BaseBindingFragment<NewFolderFragmentBinding>(
    NewFolderFragmentBinding::inflate
) {
    private val viewModel: NextCloudLoginFlowViewModel by activityViewModels()
    private lateinit var serverNextCloud: NextCloudServer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeView()
        setupObservers()
    }

    private fun initializeView() {
        arguments?.getString(OBJECT_KEY)?.let {
            serverNextCloud = Gson().fromJson(it, NextCloudServer::class.java)
        }
        setupClickListeners()
        KeyboardUtil(binding.root)
    }

    private fun setupClickListeners() {
        binding.nextBtn.setOnClickListener { handleFolderCreation() }
        binding.backBtn.setOnClickListener { baseActivity.onBackPressed() }
    }

    private fun handleFolderCreation() {
        val folderName = binding.createFolderEdit.text.toString()
        if (folderName.isNotBlank()) {
            serverNextCloud.folderName = folderName
            initiateFolderCreation(folderName)
        } else {
            binding.createFolderLayout.error = getString(R.string.Folder_Empty_Error)
        }
    }

    private fun initiateFolderCreation(folderName: String) {
        viewModel.progress.postValue(true)
        (activity as? INextCloudAuthFlow)?.onStartCreateRemoteFolder(folderName)
    }

    private fun setupObservers() {
        viewModel.errorFolderCreation.observe(viewLifecycleOwner) { message ->
            showToast(message)
        }

        viewModel.errorFolderNameExist.observe(viewLifecycleOwner) { message ->
            binding.createFolderLayout.error = message
        }

        viewModel.successFolderCreation.observe(viewLifecycleOwner) {
            navigateToSuccessScreen()
        }

        viewModel.progress.observe(viewLifecycleOwner) { isVisible ->
            binding.progressBar.isVisible = isVisible
        }
    }

    private fun navigateToSuccessScreen() {
        bundle.putString(OBJECT_KEY, Gson().toJson(serverNextCloud))
        navManager().actionNextCloudNewFolderScreenToSuccessfulScreen()
    }
}
