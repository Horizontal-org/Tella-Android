package org.horizontal.tella.mobile.views.fragment.vault.info

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.hzontal.tella_vault.VaultFile
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.horizontal.tella.mobile.databinding.FragmentVaultInfoBinding
import org.horizontal.tella.mobile.util.DateUtil
import org.horizontal.tella.mobile.util.FileUtil
import org.horizontal.tella.mobile.util.VaultFolderPath
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.vault.attachements.helpers.VAULT_FILE_ARG

class VaultInfoFragment : BaseBindingFragment<FragmentVaultInfoBinding>(FragmentVaultInfoBinding::inflate) {

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
        arguments?.getSerializable(VAULT_FILE_ARG)?.let {
            (it as VaultFile).run {
                binding.run {
                    fileInfoTv.text = name
                    fileFormatTv.text = mimeType
                    fileCreatedTv.text = DateUtil.getDate(created)
                    fileSizeTv.text = FileUtil.getFileSizeString(size)
                }
                bindFolderPath(id)
            }
        }
        val isToolbarShown = arguments?.getBoolean(VAULT_FILE_INFO_TOOLBAR) ?: false
        binding.toolbar.isVisible = isToolbarShown
        binding.appbar.isVisible = isToolbarShown
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
                            binding.filePathTv.text = path
                        }
                    },
                    {
                        if (isAdded) {
                            binding.filePathTv.text = VaultFolderPath.ROOT
                        }
                    }
                )
        )
    }
}
