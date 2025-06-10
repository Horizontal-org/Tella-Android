package org.horizontal.tella.mobile.views.activity

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import com.simplify.ink.InkView
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.ActivitySignatureBinding
import org.horizontal.tella.mobile.mvvm.signature.SignatureViewModel
import org.horizontal.tella.mobile.util.DialogsUtil
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity
import org.hzontal.shared_ui.utils.DialogUtils
import java.io.ByteArrayOutputStream

@AndroidEntryPoint
class SignatureActivity : BaseLockActivity() {

    companion object {
        const val MEDIA_FILE_KEY = "mfk"
    }

    private lateinit var toolbar: Toolbar
    private lateinit var ink: InkView
    private var progressDialog: ProgressDialog? = null
    private lateinit var binding: ActivitySignatureBinding
    private val signatureViewModel: SignatureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        isManualOrientation = true
        super.onCreate(savedInstanceState)

        binding = ActivitySignatureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdge(binding.root)
        initView()

        setupToolbar()
        setupSignaturePad()
        initObservers()
    }

    private fun initObservers() {

        signatureViewModel.addingInProgress.observe(this) { isInProgress ->
            if (isInProgress) {
                onAddingStart()
            } else {
                onAddingEnd()
            }
        }


        signatureViewModel.addSuccess.observe(this, ::onAddSuccess)
        signatureViewModel.addError.observe(this, ::onAddError)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.signature_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_clear -> {
                ink.clear()
                true
            }

            R.id.menu_item_save -> {
                saveSignature()
                true
            }

            android.R.id.home -> {
                onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.collect_form_signature_app_bar)
            setHomeAsUpIndicator(R.drawable.ic_close_white)
        }
    }

    private fun saveSignature() {
        try {
            val stream = ByteArrayOutputStream()

            if (ink.getBitmap(resources.getColor(R.color.wa_white))
                    .compress(Bitmap.CompressFormat.PNG, 100, stream)
            ) {
                signatureViewModel.addPngImage(stream.toByteArray())
            }
        } catch (exception: Exception) {
            FirebaseCrashlytics.getInstance().recordException(exception)
        }
    }

    private fun onAddingStart() {
        progressDialog =
            DialogsUtil.showProgressDialog(
                this,
                getString(R.string.gallery_dialog_expl_encrypting)
            )
    }

    private fun onAddingEnd() {
        hideProgressDialog()
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.gallery_toast_file_encrypted),
            false
        )
    }

    private fun onAddSuccess(vaultFile: VaultFile) {
        setResult(Activity.RESULT_OK, Intent().putExtra(MEDIA_FILE_KEY, vaultFile))
        finish()
    }

    private fun onAddError(error: Throwable) {
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.collect_form_signature_toast_fail_saving),
            true
        )
    }

    private fun setupSignaturePad() {
        ink.apply {
            setColor(resources.getColor(android.R.color.black))
            setMinStrokeWidth(1.5f)
            setMaxStrokeWidth(6f)
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun initView() {
        toolbar = binding.toolbar
        ink = binding.content.ink
    }
}
