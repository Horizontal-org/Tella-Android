package rs.readahead.washington.mobile.views.fragment.vault.edit

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.databinding.FragmentVaultEditBinding
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.util.DateUtil
import rs.readahead.washington.mobile.util.FileUtil
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers.VAULT_FILE_ARG

class VaultEditFragment : BaseBindingFragment<FragmentVaultEditBinding>(FragmentVaultEditBinding::inflate) {


    companion object {
        const val VAULT_FILE_EDIT_TOOLBAR = "VAULT_FILE_EDIT_TOOLBAR"
        @JvmStatic
        fun newInstance(vaultFile: VaultFile,showToolbar : Boolean): VaultEditFragment {
            val args = Bundle()
            args.putSerializable(VAULT_FILE_ARG,vaultFile)
            args.putBoolean(VAULT_FILE_EDIT_TOOLBAR,showToolbar)
            val fragment = VaultEditFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }



    private fun initView() {
        binding.toolbar.backClickListener = {back()}
         arguments?.getSerializable(VAULT_FILE_ARG)?.let {
             (it as VaultFile).run {
                 val uri = MediaFileHandler.getEncryptedUri(context, it)
                 binding.run {
                     fileInfoTv.text = name
                     fileFormatTv.text = mimeType
                     fileCreatedTv.text = DateUtil.getDate(created)
                     fileSizeTv.text = FileUtil.getFileSizeString(size)
                     filePathTv.text = path
                     cropImageView.setImageUriAsync(uri)
                 }

             }
         }
        val isToolbarShown = arguments?.getBoolean(VAULT_FILE_EDIT_TOOLBAR) ?: false
        binding.toolbar.isVisible = isToolbarShown
    }
}