package rs.readahead.washington.mobile.views.fragment.vault.edit

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.hzontal.tella_vault.VaultFile
import com.theartofdev.edmodo.cropper.CropImageView
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentVaultEditBinding
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers.VAULT_FILE_ARG

@AndroidEntryPoint
class VaultEditFragment :
    BaseBindingFragment<FragmentVaultEditBinding>(FragmentVaultEditBinding::inflate),
    CropImageView.OnCropImageCompleteListener,
    CropImageView.OnSetImageUriCompleteListener {
    private val viewModel by viewModels<EditMediaViewModel>()

    companion object {
        @JvmStatic
        fun newInstance(vaultFile: VaultFile): VaultEditFragment {
            val args = Bundle()
            args.putSerializable(VAULT_FILE_ARG, vaultFile)
            val fragment = VaultEditFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListeners()
        initObservers()
    }

    private fun initView() {
        binding.toolbar.backClickListener = { back() }
        binding.toolbar.setRightIcon(R.drawable.ic_check_select)
        arguments?.getSerializable(VAULT_FILE_ARG)?.let {
            (it as VaultFile).run {
                val uri = MediaFileHandler.getEncryptedUri(context, it)
                binding.run {
                    fileInfoTv.text = "crop Image"
                    fileCreatedTv.text = "Rotate"
                    cropImageView.setImageUriAsync(uri)
                }

            }
        }
        binding.toolbar.isVisible = true
    }

    private fun initObservers() {
        with(viewLifecycleOwner) {
            viewModel.saveError.observe(this) { throwable ->
                onSaveError()
            }

            viewModel.saveSuccess.observe(this) { vaultFile ->
                onSaveSuccess()
            }

            viewModel.saveInProgress.observe(this) { isSaving ->
                if (isSaving) onSaveStart() else onSaveEnd()
            }
        }
    }

    private fun initListeners() {
        binding.toolbar.onRightClickListener?.let { cropImage() }
        binding.cropImageView.setOnCropImageCompleteListener(this)
        binding.fileInfoTv.setOnClickListener { cropImage() }
        binding.fileCreatedTv.setOnClickListener { rotateImage() }
    }

    private fun rotateImage() {
        binding.cropImageView.rotateImage(270)
    }

    private fun cropImage() {
        binding.cropImageView.getCroppedImageAsync(500, 500)
    }

    override fun onCropImageComplete(view: CropImageView?, result: CropImageView.CropResult?) {
        result?.let {
            if (result.isSuccessful) {
                binding.cropImageView.setImageBitmap(result.bitmap)
                viewModel.saveBitmapAsJpeg(result.bitmap, null)
            }
        }
    }

    override fun onSetImageUriComplete(view: CropImageView?, uri: Uri?, error: Exception?) {
    }

    private fun onSaveError() {
        DialogUtils.showBottomMessage(
            baseActivity,
            resources.getString(R.string.gallery_toast_fail_saving_file),
            true
        )
    }

    private fun onSaveSuccess() {
        DialogUtils.showBottomMessage(
            baseActivity,
            resources.getString(R.string.gallery_toast_file_encrypted),
            true
        )
    }
    private fun onSaveStart() {
        //showProgressDialog()
    }
    private fun onSaveEnd() {
        //hideProgressDialog()
        back()
    }
}