package org.horizontal.tella.mobile.views.fragment.vault.info

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.hzontal.tella_vault.Metadata
import com.hzontal.tella_vault.VaultFile
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.horizontal.tella.mobile.databinding.FragmentVaultInfoBinding
import org.horizontal.tella.mobile.util.DateUtil
import org.horizontal.tella.mobile.util.FileUtil
import org.horizontal.tella.mobile.util.VaultFolderPath
import org.horizontal.tella.mobile.views.activity.MetadataHelpActivity
import org.horizontal.tella.mobile.views.activity.MetadataViewerActivity
import org.horizontal.tella.mobile.views.activity.viewer.VerificationCategory
import org.horizontal.tella.mobile.views.activity.viewer.VerificationCategoryBinder
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.vault.attachements.helpers.VAULT_FILE_ARG

class VaultInfoFragment : BaseBindingFragment<FragmentVaultInfoBinding>(FragmentVaultInfoBinding::inflate) {

    private var vaultFile: VaultFile? = null
    private val disposables = CompositeDisposable()

    companion object {
        const val VAULT_FILE_INFO_TOOLBAR = "VAULT_FILE_INFO_TOOLBAR"
        @JvmStatic
        fun newInstance(vaultFile: VaultFile, showToolbar: Boolean): VaultInfoFragment {
            val args = Bundle()
            args.putSerializable(VAULT_FILE_ARG, vaultFile)
            args.putBoolean(VAULT_FILE_INFO_TOOLBAR, showToolbar)
            val fragment = VaultInfoFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        binding.toolbar.backClickListener = { back() }
        binding.toolbar.onRightClickListener = {
            startActivity(Intent(requireContext(), MetadataHelpActivity::class.java))
        }

        vaultFile = arguments?.getSerializable(VAULT_FILE_ARG) as? VaultFile
        vaultFile?.let { file ->
            binding.fileGeneralInfo.run {
                fileInfoTv.text = file.name
                fileFormatTv.text = file.mimeType
                fileCreatedTv.text = DateUtil.getDate(file.created)
                fileSizeTv.text = FileUtil.getFileSizeString(file.size)
            }
            bindFolderPath(file.id)
            bindVerificationSection(file)
        }

        val isToolbarShown = arguments?.getBoolean(VAULT_FILE_INFO_TOOLBAR) ?: false
        binding.toolbar.isVisible = isToolbarShown
        binding.appbar.isVisible = isToolbarShown
        if (!isToolbarShown) {
            binding.toolbar.setRightIcon(-1)
        }
    }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }

    private fun bindFolderPath(fileId: String?) {
        disposables.add(
            VaultFolderPath.resolveAsync(fileId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { path ->
                        if (isAdded) {
                            binding.fileGeneralInfo.filePathTv.text = path
                        }
                    },
                    {
                        if (isAdded) {
                            binding.fileGeneralInfo.filePathTv.text = VaultFolderPath.ROOT
                        }
                    }
                )
        )
    }

    private fun bindVerificationSection(file: VaultFile) {
        val hasMetadata = file.metadata != null
        binding.verificationSection.isVisible = hasMetadata
        binding.toolbar.setRightIconVisibility(hasMetadata)
        if (!hasMetadata) {
            return
        }
        VerificationCategoryBinder.bind(binding.verificationCategories.root) { category ->
            openVerificationCategory(category)
        }
    }

    private fun openVerificationCategory(category: VerificationCategory) {
        val file = vaultFile ?: return
        val intent = Intent(requireContext(), MetadataViewerActivity::class.java)
        intent.putExtra(Metadata.VIEW_METADATA, file)
        intent.putExtra(MetadataViewerActivity.CATEGORY, category)
        startActivity(intent)
    }
}
