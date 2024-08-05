package rs.readahead.washington.mobile.views.fragment.vault.edit

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.canhub.cropper.CropImageView
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.EditMediaSavedEvent
import rs.readahead.washington.mobile.databinding.FragmentVaultEditBinding
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.util.show
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers.VAULT_FILE_ARG

@AndroidEntryPoint
class VaultEditFragment :
    BaseBindingFragment<FragmentVaultEditBinding>(FragmentVaultEditBinding::inflate),
    CropImageView.OnCropImageCompleteListener,
    CropImageView.OnSetImageUriCompleteListener {
    private val viewModel by viewModels<EditMediaViewModel>()
    private var isNavigationCall = true
    private var currentParent: String? = null

    companion object {
        const val CALLER_PARAM = "cp"
        const val CURRENT_EDIT_PARENT = "cep"

        @JvmStatic
        fun newInstance(
            vaultFile: VaultFile,
            isFromViewer: Boolean,
            currentParent: String? = null
        ): VaultEditFragment {
            val args = Bundle().apply {
                putSerializable(VAULT_FILE_ARG, vaultFile)
                putBoolean(CALLER_PARAM, isFromViewer)
                putString(CURRENT_EDIT_PARENT, currentParent)
            }
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
        arguments?.getBoolean(CALLER_PARAM)?.let {
            isNavigationCall = it
        }

        arguments?.getString(CURRENT_EDIT_PARENT)?.let {
            currentParent = it
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
        binding.close.setOnClickListener { goBack() }
        binding.cropImageView.setOnCropImageCompleteListener(this)
        binding.rotate.setOnClickListener { rotateImage() }
        binding.flipVertically.setOnClickListener { flipImageVertically() }
        binding.flipHorizontally.setOnClickListener { flipImageHorizontally() }
        binding.cropImageView.setOnSetCropOverlayReleasedListener {
            showAcceptButton()
        }
        binding.cropImageView.isAutoZoomEnabled = true
        binding.cropImageView.maxZoom = 2
    }

    /**
     * After the change is made on the image this displays the top left item on the toolbar to save changes.
     * It also adds a dialog on exit to check if the user wants to save changes or not.
     */
    private fun showAcceptButton() {
        if (!binding.accept.isVisible) {
            binding.accept.visibility = View.VISIBLE
            binding.accept.setOnClickListener { cropImage() }
            binding.close.setOnClickListener { checkSaveOrExit() }
        }
    }

    private fun rotateImage() {
        binding.cropImageView.rotateImage(270)
        showAcceptButton()
    }

    private fun flipImageVertically() {
        binding.cropImageView.flipImageVertically()
        showAcceptButton()
    }

    private fun flipImageHorizontally() {
        binding.cropImageView.flipImageHorizontally()
        showAcceptButton()
    }


    /**
     * We take the cropped part of the image as a bitmap and save it as a jpeg
     */
    private fun cropImage() {
        var bitmap: Bitmap? = null
        binding.progressBar.show()
        binding.cropImageView.cropRect?.let {
            bitmap = binding.cropImageView.getCroppedImage(
                it.width(),
                it.height()
            )
        }

        bitmap?.let {
            //binding.cropImageView.setImageBitmap(it)
            viewModel.saveBitmapAsJpeg(it, currentParent)
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
        MyApplication.bus().post(EditMediaSavedEvent())
    }

    private fun onSaveStart() {
        binding.progressBar.show()
    }

    private fun onSaveEnd() {
        binding.progressBar.hide()
        goBack()
    }

    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        binding.progressBar.hide()
        /*if (result.isSuccessful) {
            binding.cropImageView.setImageBitmap(result.bitmap)
            result.bitmap?.let { it1 -> viewModel.saveBitmapAsJpeg(it1, null) }
        }*/
    }

    override fun onSetImageUriComplete(view: CropImageView, uri: Uri, error: Exception?) {
    }

    private fun checkSaveOrExit() {
        BottomSheetUtils.showStandardSheet(
            baseActivity.supportFragmentManager,
            baseActivity.getString(R.string.Edit_Dialog_confirm_exit_title),
            baseActivity.getString(R.string.Edit_Dialog_confirm_exit),
            baseActivity.getString(R.string.action_save).uppercase(),
            baseActivity.getString(R.string.action_exit_without_saving),
            onConfirmClick = {
                cropImage()
            },
            onCancelClick = {
                goBack()
            }
        )
    }

    private fun goBack() {
        if (isNavigationCall) {
            back()
        } else {
            @Suppress("DEPRECATION")
            activity?.onBackPressed()
        }
    }
}