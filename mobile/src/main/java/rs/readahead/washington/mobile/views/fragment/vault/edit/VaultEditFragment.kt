package rs.readahead.washington.mobile.views.fragment.vault.edit

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.hzontal.tella_vault.VaultFile
import com.canhub.cropper.CropImageView
import dagger.hilt.android.AndroidEntryPoint
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
        arguments?.getSerializable(VAULT_FILE_ARG)?.let {
            (it as VaultFile).run {
                val uri = MediaFileHandler.getEncryptedUri(context, it)
                binding.run {
                    cropImageView.setImageUriAsync(uri)
                }
            }
        }
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
        binding.close.setOnClickListener { back() }
        binding.cropImageView.setOnCropImageCompleteListener(this)
        binding.rotate.setOnClickListener { rotateImage() }
        binding.cropImageView.setOnSetCropOverlayReleasedListener {
            showAcceptButton()
        }
    }

    private fun showAcceptButton() {
        if (!binding.accept.isVisible) {
            binding.accept.visibility = View.VISIBLE
            binding.accept.setOnClickListener { cropImage() }
        }
    }

    private fun rotateImage() {
        binding.cropImageView.rotateImage(270)
        showAcceptButton()
    }

    private fun cropImage() {
        var bitmap: Bitmap? = null
        binding.cropImageView.cropRect?.let {
            bitmap = binding.cropImageView.getCroppedImage(
                it.width(),
                it.height()
            )
        }

        bitmap?.let{
            binding.cropImageView.setImageBitmap(it)
            viewModel.saveBitmapAsJpeg(it, null)
        }
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

    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        /*if (result.isSuccessful) {
            binding.cropImageView.setImageBitmap(result.bitmap)
            result.bitmap?.let { it1 -> viewModel.saveBitmapAsJpeg(it1, null) }
        }*/
    }

    override fun onSetImageUriComplete(view: CropImageView, uri: Uri, error: Exception?) {

    }
}