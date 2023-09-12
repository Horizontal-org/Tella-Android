package rs.readahead.washington.mobile.views.fragment.vault.edit

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.hzontal.tella_vault.VaultFile
import com.theartofdev.edmodo.cropper.CropImageView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentVaultEditBinding
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.util.FileUtil
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers.VAULT_FILE_ARG
import timber.log.Timber
import java.io.ByteArrayOutputStream

class VaultEditFragment :
    BaseBindingFragment<FragmentVaultEditBinding>(FragmentVaultEditBinding::inflate),
    CropImageView.OnCropImageCompleteListener,
    CropImageView.OnSetImageUriCompleteListener {


    companion object {
        const val VAULT_FILE_EDIT_TOOLBAR = "VAULT_FILE_EDIT_TOOLBAR"

        @JvmStatic
        fun newInstance(vaultFile: VaultFile, showToolbar: Boolean): VaultEditFragment {
            val args = Bundle()
            args.putSerializable(VAULT_FILE_ARG, vaultFile)
            args.putBoolean(VAULT_FILE_EDIT_TOOLBAR, showToolbar)
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
        binding.toolbar.backClickListener = { back() }
        binding.toolbar.setRightIcon(R.drawable.ic_check_select)
        arguments?.getSerializable(VAULT_FILE_ARG)?.let {
            (it as VaultFile).run {
                val uri = MediaFileHandler.getEncryptedUri(context, it)
                binding.run {
                    fileInfoTv.text = "crop Image"
                    fileFormatTv.text = mimeType
                    fileCreatedTv.text = "Rotate"
                    fileSizeTv.text = FileUtil.getFileSizeString(size)
                    filePathTv.text = path
                    cropImageView.setImageUriAsync(uri)
                }

            }
        }
        binding.toolbar.onRightClickListener?.let { cropImage() }
        binding.cropImageView.setOnCropImageCompleteListener(this)
        val isToolbarShown = arguments?.getBoolean(VAULT_FILE_EDIT_TOOLBAR) ?: false
        binding.toolbar.isVisible = isToolbarShown
        binding.fileInfoTv.setOnClickListener { cropImage() }
        binding.fileCreatedTv.setOnClickListener { rotateImage() }
    }

    private fun rotateImage() {
        Timber.d("++++ rotateImage()")
        binding.cropImageView.rotateImage(270)
    }
    private fun cropImage() {
        Timber.d("++++ cropImage()")
        binding.cropImageView.getCroppedImageAsync(500, 500)
        /*val bitmap: Bitmap = binding.cropImageView.getCroppedImage(500,500)
        val imageJpegStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, imageJpegStream)
        try {
            Timber.d("++++ MediaFileHandler.saveJpegPhoto")
            MediaFileHandler.saveJpegPhoto(imageJpegStream.toByteArray(), null)
        } catch (throwable: Throwable) {
            Timber.d("++++ error %s", throwable.localizedMessage)
        }*/
    }

    override fun onCropImageComplete(view: CropImageView?, result: CropImageView.CropResult?) {
        Timber.d("++++ onCropImageComplete")
        if (result != null) {
            if (result.isSuccessful) {
                val bitmap: Bitmap = result.bitmap
                binding.cropImageView.setImageBitmap(bitmap);

                val imageJpegStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, imageJpegStream)
                try {
                    Timber.d("++++ MediaFileHandler.saveJpegPhoto")
                    MediaFileHandler.saveJpegPhoto(imageJpegStream.toByteArray(), null)
                } catch (throwable: Throwable) {
                    Timber.d("++++ error %s", throwable.localizedMessage)
                }
            }
        }
    }

    override fun onSetImageUriComplete(view: CropImageView?, uri: Uri?, error: Exception?) {
        Timber.d("++++ onSetImageUriComplete")
        if (uri != null) {
            Timber.d("++++ uri %s", uri.path)
        }

        if (error != null) {
            Timber.d("++++ error %s", error.localizedMessage)
        }
    }
}